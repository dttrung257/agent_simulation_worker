package com.uet.agent_simulation_worker.services.simulation;

import com.uet.agent_simulation_worker.commands.builders.IGamaCommandBuilder;
import com.uet.agent_simulation_worker.commands.executors.IGamaCommandExecutor;
import com.uet.agent_simulation_worker.constant.ExperimentResultStatusConst;
import com.uet.agent_simulation_worker.constant.SimulationConst;
import com.uet.agent_simulation_worker.exceptions.errors.ExperimentErrors;
import com.uet.agent_simulation_worker.exceptions.errors.ModelErrors;
import com.uet.agent_simulation_worker.exceptions.errors.ProjectErrors;
import com.uet.agent_simulation_worker.exceptions.experiment.ExperimentNotFoundException;
import com.uet.agent_simulation_worker.exceptions.model.ModelNotFoundException;
import com.uet.agent_simulation_worker.exceptions.simulation.CannotClearOldSimulationOutputException;
import com.uet.agent_simulation_worker.exceptions.project.ProjectNotFoundException;
import com.uet.agent_simulation_worker.models.Experiment;
import com.uet.agent_simulation_worker.models.ExperimentResult;
import com.uet.agent_simulation_worker.models.ExperimentResultCategory;
import com.uet.agent_simulation_worker.models.ExperimentResultImage;
import com.uet.agent_simulation_worker.repositories.ExperimentRepository;
import com.uet.agent_simulation_worker.repositories.ExperimentResultCategoryRepository;
import com.uet.agent_simulation_worker.repositories.ExperimentResultImageRepository;
import com.uet.agent_simulation_worker.repositories.ModelRepository;
import com.uet.agent_simulation_worker.repositories.ProjectRepository;
import com.uet.agent_simulation_worker.requests.simulation.CreateSimulationRequest;
import com.uet.agent_simulation_worker.services.auth.IAuthService;
import com.uet.agent_simulation_worker.services.experiment_result.IExperimentResultService;
import com.uet.agent_simulation_worker.services.image.IImageService;
import com.uet.agent_simulation_worker.services.node.INodeService;
import com.uet.agent_simulation_worker.services.s3.IS3Service;
import com.uet.agent_simulation_worker.utils.ConvertUtil;
import com.uet.agent_simulation_worker.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This service is used to handle simulation logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService implements ISimulationService {
    private final TimeUtil timeUtil;
    private final IS3Service s3Service;
    private final ConvertUtil convertUtil;
    private final IAuthService authService;
    private final INodeService nodeService;
    private final IImageService imageService;
    private final ModelRepository modelRepository;
    private final ProjectRepository projectRepository;
    private final ExecutorService virtualThreadExecutor;
    private final IGamaCommandBuilder gamaCommandBuilder;
    private final IGamaCommandExecutor gamaCommandExecutor;
    private final ExperimentRepository experimentRepository;
    private final IExperimentResultService experimentResultService;
    private final ExperimentResultImageRepository experimentResultImageRepository;
    private final ExperimentResultCategoryRepository experimentResultCategoryRepository;

    @Value("${app.project.path}")
    private String projectPath;

    @Override
    public void run() {
        final var start = timeUtil.getCurrentTime();
        log.info("Simulation started at {}", timeUtil.getCurrentTimeString());

        final var processBuilder = new ProcessBuilder();

        final var command = gamaCommandBuilder.buildLegacy(
                Map.of(
                        "-hpc", "8",
                        "-m", "8"
                ),
                "/Users/trungdt/Workspaces/uet/gama/pig-farm/models/sml-01.xml",
                "/Users/trungdt/Workspaces/uet/gama/pig-farm/models/test_output/"
        );
        /*
        final String batchCommand = gamaCommandBuilder.buildBatch(
                Map.of(
                        "-hpc", "2",
                        "-m", "8"
                ),
                "Normal",
                "/Users/trungdt/Workspaces/uet/gama/pig-farm/models/simulator-01.gaml"
        );
        */

        try {
            log.info("Clear old output directory");
            processBuilder.command("bash", "-c", "rm -rf /Users/trungdt/Workspaces/uet/gama/pig-farm/includes/output/normal/**").start();

            log.info("Running command: {}", command);
            final var process = processBuilder.command("bash", "-c", command).start();

            final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("Simulation ended at {}, took {} seconds",
                timeUtil.getCurrentTimeString(),
                (timeUtil.getCurrentTime() - start) * 1.0 / 1000
        );
    }

    @Override
    @Transactional(rollbackFor = { Exception.class, Throwable.class })
    public void run(CreateSimulationRequest request) {
        // Run simulation
        runSimulation(request);
    }

    /**
     * This method is used to run simulation.
     *
     * @param request CreateSimulationRequest
     */
    private void runSimulation(CreateSimulationRequest request) {
        final var nodeId = nodeService.getCurrentNodeId() != null ? nodeService.getCurrentNodeId().toString() : "";

        final var userId = authService.getCurrentUserId();
        request = calculateExperimentResultId(request);
        final var projectId = request.getProjectId();
        final var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(ProjectErrors.E_PJ_0001.defaultMessage()));

        final var projectLocation = projectPath + project.getLocation();

        request.getExperiments().forEach(experimentReq -> {
            // Get model
            final var model = modelRepository.findByModeIdAndProjectId(experimentReq.getModelId(), projectId)
                    .orElseThrow(() -> new ModelNotFoundException(ModelErrors.E_MODEL_0001.defaultMessage()));

            // Get experiment
            final var experiment = experimentRepository
                    .findByExperimentIdAndModelId(experimentReq.getId(), experimentReq.getModelId())
                    .orElseThrow(() -> new ExperimentNotFoundException(ExperimentErrors.E_EXP_0001.defaultMessage()));

            experimentReq.setGamlFile(model.getName() + ".gaml");
            experimentReq.setExperiment(experiment);
        });

        request.getExperiments().forEach(experimentReq -> {
            // Get experiment data from request.
            final var modelId = experimentReq.getModelId();
            final var experimentId = experimentReq.getId();
            final var gamlFile = experimentReq.getGamlFile();
            final var finalStep = experimentReq.getFinalStep() != null ? experimentReq.getFinalStep() : SimulationConst.DEFAULT_FINAL_STEP;
            final var experimentResultId = experimentReq.getExperimentResultId();
            final var gamlFileName = gamlFile.replace(".gaml", "");
            final var experimentName = experimentReq.getExperiment().getName();
            final var pathToExperimentPlanXmlFile = getPathToExperimentPlanXmlFile(nodeId, userId, projectId, modelId,
                experimentId, experimentResultId, gamlFileName, experimentName);
            final var pathToLocalExperimentOutputDir = getPathToLocalExperimentOutputDir(nodeId, userId, projectId,
                modelId, experimentId, experimentResultId, gamlFileName, experimentName);

            // Clear old xml file
            clearLocalResource(pathToExperimentPlanXmlFile);

            // Create experiment plan XML file.
            final var createXmlCommand = gamaCommandBuilder.createXmlFile(
                    experimentName,  projectLocation + "/models/" + gamlFile, pathToExperimentPlanXmlFile);

            // Build command to run experiment.
            final var runLegacyCommand = gamaCommandBuilder.buildLegacy(null, pathToExperimentPlanXmlFile, pathToLocalExperimentOutputDir);

            // Recreate experiment result.
            final var experimentResult = experimentResultService.recreate(experimentReq.getExperiment(), finalStep,
                    pathToLocalExperimentOutputDir);

            // Execute legacy command.
            executeLegacy(createXmlCommand, runLegacyCommand, pathToExperimentPlanXmlFile,
                    experimentReq.getExperiment(), finalStep, pathToLocalExperimentOutputDir, experimentResult);
        });
    }

    private String getPathToExperimentPlanXmlFile(String nodeId, BigInteger userId, BigInteger projectId,
            BigInteger modelId, BigInteger experimentId, int experimentResultId, String gamlFileName, String experimentName) {

        return String.format("storage/xmls/node-%s_user-%s_project-%s_model-%s_experiment-%s_result-%s_%s-%s.xml",
                nodeId, userId, projectId, modelId, experimentId, experimentResultId, gamlFileName, experimentName);
    }

    private String getPathToLocalExperimentOutputDir(String nodeId, BigInteger userId, BigInteger projectId,
            BigInteger modelId, BigInteger experimentId, int experimentResultId, String gamlFileName, String experimentName) {

        return String.format("storage/outputs/node-%s_user-%s_project-%s_model-%s_experiment-%s_result-%s_%s-%s",
                nodeId, userId, projectId, modelId, experimentId, experimentResultId, gamlFileName, experimentName);
    }

    /**
     * This method is used to calculate experiment result id.
     *
     * @param request CreateSimulationRequest
     * @return CreateSimulationRequest
     */
    private CreateSimulationRequest calculateExperimentResultId(CreateSimulationRequest request) {
        final var userId = authService.getCurrentUserId();
        final Map<String, AtomicInteger> idCounters = new HashMap<>();

        request.getExperiments().forEach(experiment -> {
            final var modelId = experiment.getModelId();
            final var experimentId = experiment.getId();
            final String key = userId + "_" + modelId + "_" + experimentId;

            final AtomicInteger counter = idCounters.computeIfAbsent(key, value -> new AtomicInteger(1));
            experiment.setExperimentResultId(counter.getAndIncrement());
        });

        return request;
    }

    /**
     * This method is used to run job execute legacy command.
     *
     * @param createXmlCommand String
     * @param runLegacyCommand String
     * @param pathToExperimentPlanXmlFile String
     * @param experiment Experiment
     * @param finalStep long
     * @param pathToLocalExperimentOutputDir String
     * @param experimentResult ExperimentResult
     */
    private void executeLegacy(String createXmlCommand, String runLegacyCommand, String pathToExperimentPlanXmlFile,
        Experiment experiment, long finalStep, String pathToLocalExperimentOutputDir, ExperimentResult experimentResult) {
        try {
            final var experimentId = experiment.getId();
            final var experimentName = experiment.getName();
            experimentResultService.updateStatus(experimentResult, ExperimentResultStatusConst.IN_PROGRESS);

            virtualThreadExecutor.submit(() -> {
                final var clearResourceFuture = waitForClearLocalResource(pathToLocalExperimentOutputDir);
                clearResourceFuture.whenComplete((clearResourceResult, clearResourceError) -> {
                    if (clearResourceError != null) {
                        log.error("Error while clearing local resource", clearResourceError);
                        experimentResultService.updateStatus(experimentResult, ExperimentResultStatusConst.FAILED);
                        return;
                    }

                    // Execute commands
                    final var executeCommandFuture = gamaCommandExecutor.executeLegacy(createXmlCommand,
                        runLegacyCommand, pathToExperimentPlanXmlFile, experimentId, experimentName, finalStep);

                    executeCommandFuture.whenComplete((executeCommandResult, executeCommandError) -> {
                        if (executeCommandError != null) {
                            log.error("Error while running simulation", executeCommandError);

                            clearLocalResource(pathToLocalExperimentOutputDir);
                            clearLocalResource(pathToExperimentPlanXmlFile);
                            experimentResultService.updateStatus(experimentResult, ExperimentResultStatusConst.FAILED);
                            return;
                        }

                        log.info("Simulation completed for experiment: {}", experimentId);

                        // If execution is successful, upload output directory to S3
//                    clearOldS3Result(projectId, modelId, experimentId, experimentResultId);
//                    uploadResult(projectId, modelId, experimentId, experimentResultId, pathToLocalExperimentOutputDir);
//                    clearLocalResource(pathToLocalExperimentOutputDir);
                        clearLocalResource(pathToExperimentPlanXmlFile);

                        // Save result to database.
//                    final var experimentResultKey = getOutputObjectKey(projectId, modelId, experimentId, experimentResultId);
//                    final var experimentResultImageKeys = s3Service.listFileNamesInDirectory(experimentResultKey + "snapshot/");
//                    if (experimentResultImageKeys.isEmpty()) {
//                        log.error("No image found in S3 for experiment result: {}", experimentResultKey);
//                        updateExperimentResultStatus(experimentResultStatus, ExperimentResultStatusConst.FAILED);
//                        return;
//                    }

//                    saveResultS3(experimentResultImageKeys, experiment, experimentResultKey, finalStep, experimentResultStatus);

                        // Sleep for 1 second to make sure all images are loaded.
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Error while sleeping", e);
                        }
                        final var imageList = imageService.listImagesInDirectory(pathToLocalExperimentOutputDir + "/snapshot");
                        saveResult(experimentResult, imageList, pathToLocalExperimentOutputDir);
                    });

                    executeCommandFuture.join();
                });
                clearResourceFuture.join();
            });
        } catch (Exception e) {
            log.error("Error while executing legacy command: ", e);
        }
    }

    /**
     * This method is used to clear local resource.
     *
     * @param dir String
     */
    private void clearLocalResource(String dir) {
        // Clear old output directory
        final var processBuilder = new ProcessBuilder();

        log.info("Start clear old output directory: {}", dir);
        try {
            final var process = processBuilder.command("bash", "-c", "rm -rf " + dir).start();

            log.info("Successfully clear old output directory: {}", dir);
        } catch (Exception e) {
            throw new CannotClearOldSimulationOutputException(e.getMessage());
        }
    }

    /**
     * This method is used to clear local resource.
     *
     * @param dir String
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> waitForClearLocalResource(String dir) {
        return CompletableFuture.runAsync(() -> {
            // Clear old output directory
            final var processBuilder = new ProcessBuilder();

            log.info("Start clear old output directory: " + dir);

            try {
                processBuilder.command("bash", "-c", "rm -rf " + dir).start().waitFor();
                log.info("Successfully clear old output directory: " + dir);
            } catch (Exception e) {
                throw new CannotClearOldSimulationOutputException(e.getMessage());
            }
        }, virtualThreadExecutor);
    }

    /**
     * This method is used to save result after simulation.
     *
     * @param experimentResult ExperimentResult
     * @param imageList List<List<String>>
     * @param experimentResultLocation String
     */
    private void saveResult(ExperimentResult experimentResult, List<List<String>> imageList, String experimentResultLocation) {
        long start = timeUtil.getCurrentTimeNano();
        // Save new experiment result images.
        final List<ExperimentResultImage> experimentResultImageList = new ArrayList<>();
        final var imageCategoryList = imageList.stream().map(image -> image.get(2)).distinct().toList();
        final var experimentResultCategoryList = imageCategoryList.stream().map(imageCategory -> ExperimentResultCategory.builder()
            .name(imageCategory)
            .experimentResult(experimentResult)
            .build()).toList();

        final var experimentResultCategories = experimentResultCategoryRepository.saveAll(experimentResultCategoryList);

        imageList.forEach(image -> {
            final var imageName = image.getFirst();
            final var imageExtension = image.get(1);
            final var imageCategory = image.get(2);
            final var imageStep = !image.get(3).isEmpty() ? convertUtil.convertStringToInteger(image.get(3)) : null;
            final var experimentResultCategory = experimentResultCategories.stream()
                .filter(category -> category.getName().equals(imageCategory))
                .findFirst()
                .orElse(null);

            experimentResultImageList.add(ExperimentResultImage.builder()
                .name(imageName)
                .location(experimentResultLocation + "/snapshot/" + imageName)
                .extension(imageExtension)
                .experimentResult(experimentResult)
                .experimentResultCategory(experimentResultCategory)
                .step(imageStep)
                .build());
        });
        experimentResultImageRepository.saveAll(experimentResultImageList);

        long end = timeUtil.getCurrentTimeNano();
        log.info("Save experiment result images took: {} ns", end - start);

        // Update experiment result status.
        experimentResultService.updateStatus(experimentResult, ExperimentResultStatusConst.FINISHED);
    }

//    /**
//     * This method is used to save result after simulation.
//     *
//     * @param experimentResultImageKeys List<String>
//     * @param experiment Experiment
//     * @param experimentResultKey String
//     * @param finalStep long
//     * @param experimentResultStatus ExperimentResultStatus
//     */
//    private void saveResultS3(List<String> experimentResultImageKeys, Experiment experiment, String experimentResultKey,
//        long finalStep, ExperimentResultStatus experimentResultStatus) {
//        // Delete old experiment result.
//        experimentResultRepository.deleteByExperimentId(experiment.getId());
//
//        // Save new experiment result.
//        final var experimentResult = experimentResultRepository.save(
//            ExperimentResult.builder()
//                .experiment(experiment).location(s3Service.getS3PrefixUrl() + experimentResultKey)
//                .finalStep((int) finalStep).build());
//
//        // Delete old images.
//        experimentResultImageRepository.deleteByExperimentResultId(experimentResult.getId());
//
//        long start = timeUtil.getCurrentTimeNano();
//        // Save new experiment result images.
//        final List<ExperimentResultImage> experimentResultImages = new ArrayList<>();
//        experimentResultImageKeys.forEach(experimentResultImageKey -> {
//            experimentResultImages.add(
//                ExperimentResultImage.builder().location(s3Service.getS3PrefixUrl() + experimentResultImageKey)
//                    .experimentResult(experimentResult).build());
//        });
//        experimentResultImageRepository.saveAll(experimentResultImages);
//        long end = timeUtil.getCurrentTimeNano();
//        log.info("Save experiment result images took: {} ns", end - start);
//
//        // Update experiment result status.
//        updateExperimentResultStatus(experimentResultStatus, ExperimentResultStatusConst.DONE);
//    }

    /**
     * This method is used to clear old S3 result.
     *
     * @param projectId BigInteger
     * @param modelId BigInteger
     * @param experimentId BigInteger
     * @param experimentResultId int
     */
    private void clearOldS3Result(BigInteger projectId, BigInteger modelId, BigInteger experimentId, int experimentResultId) {
        final var outputObjectKey = getOutputObjectKey(projectId, modelId, experimentId, experimentResultId);

        s3Service.clear(outputObjectKey);
    }

    /**
     * This method is used to upload result to S3.
     *
     * @param projectId BigInteger
     * @param modelId BigInteger
     * @param experimentId BigInteger
     * @param experimentResultId int
     * @param localOutputDir String
     */
    private void uploadResult(BigInteger projectId, BigInteger modelId, BigInteger experimentId, int experimentResultId,
        String localOutputDir) {
        log.info("Start get S3 object key");
        final var outputObjectKey = getOutputObjectKey(projectId, modelId, experimentId, experimentResultId);
        log.info("End get S3 object key: {}", outputObjectKey);

        log.info("Start upload directory to S3");
        s3Service.uploadDirectory(localOutputDir, outputObjectKey);
        log.info("End upload directory to S3");
    }

    /**
     * This method is used to get output object key.
     *
     * @param projectId BigInteger
     * @param modelId BigInteger
     * @param experimentId BigInteger
     * @param experimentResultId int
     * @return String
     */
    private String getOutputObjectKey(BigInteger projectId, BigInteger modelId, BigInteger experimentId,
        int experimentResultId) {
        final var userId = authService.getCurrentUserId();

        return String.format("simulation_results/user_%s/project_%s/model_%s/experiment_%s/result_%s/",
            projectId, userId, modelId, experimentId, experimentResultId);
    }
}