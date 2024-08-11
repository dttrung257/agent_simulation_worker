package com.uet.agent_simulation_worker.services.impl;

import com.uet.agent_simulation_worker.commands.builders.IGamaCommandBuilder;
import com.uet.agent_simulation_worker.commands.executors.IGamaCommandExecutor;
import com.uet.agent_simulation_worker.constant.SimulationConst;
import com.uet.agent_simulation_worker.exceptions.simulation.CannotClearOldSimulationOutputException;
import com.uet.agent_simulation_worker.requests.simulation.CreateSimulationRequest;
import com.uet.agent_simulation_worker.services.ISimulationService;
import com.uet.agent_simulation_worker.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * This service is used to handle simulation logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService implements ISimulationService {
    private final TimeUtil timeUtil;
    private final S3Service s3Service;
    private final AuthService authService;
    private final ExecutorService virtualThreadExecutor;
    private final IGamaCommandBuilder gamaCommandBuilder;
    private final IGamaCommandExecutor gamaCommandExecutor;

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
    public void run(CreateSimulationRequest request) {
        // Clear old simulation output directories.
        clearOldLocalResult(request);

        // Run simulation
        runSimulation(request);
    }

    /**
     * This method is used to run simulation.
     *
     * @param request CreateSimulationRequest
     */
    private void runSimulation(CreateSimulationRequest request) {
        final var experiments = request.getExperiments();

        experiments.forEach(experiment -> {
            // Get experiment data from request.
            final var experimentId = experiment.getId();
            final var gamlFile = experiment.getGamlFile();
            final var experimentName = experiment.getExperimentName();
            final var finalStep = experiment.getFinalStep() != null ? experiment.getFinalStep() : SimulationConst.DEFAULT_FINAL_STEP;
            final var gamlFileName = gamlFile.replace(".gaml", "");
            final var pathToXmlFile = "./storage/xmls/" + experimentId + "_" + gamlFileName + ".xml";
            final var localOutputDir = "./storage/outputs/" + experimentId + "_" + gamlFileName + "/";

            // Create experiment plan XML file.
            final var createXmlCommand = gamaCommandBuilder.createXmlFile(
                experimentName, "/Users/trungdt/Workspaces/uet/gama/pig-farm/models/" + gamlFile, pathToXmlFile
            );

            // Build command to run experiment.
            final var runLegacyCommand = gamaCommandBuilder.buildLegacy(null, pathToXmlFile, localOutputDir);

            // Execute legacy command.
            executeLegacy(createXmlCommand, runLegacyCommand, pathToXmlFile, experimentId, experimentName, finalStep, localOutputDir);
        });
    }

    /**
     * This method is used to clear old simulation output directories.
     *
     * @param request CreateSimulationRequest
     */
    private void clearOldLocalResult(CreateSimulationRequest request) {
        final var experiments = request.getExperiments();
        log.info("Start clearing old output directories");
        experiments.forEach(experiment -> {
            // Get experiment data from request
            final var experimentId = experiment.getId();
            final var gamlFileName = experiment.getGamlFile().replace(".gaml", "");
            final var localOutputDir = "./storage/outputs/" + experimentId + "_" + gamlFileName + "/";

            // Clear old output directory
            final var processBuilder = new ProcessBuilder();
            try {
                processBuilder.command("bash", "-c", "rm -rf " + localOutputDir).start();
            } catch (Exception e) {
                throw new CannotClearOldSimulationOutputException(e.getMessage());
            }
        });
        log.info("Finished clearing old output directories");
    }

    /**
     * This method is used to run job execute legacy command.
     *
     * @param createXmlCommand String
     * @param runLegacyCommand String
     * @param pathToXmlFile String
     * @param experimentId int
     * @param experimentName String
     * @param finalStep long
     * @param localOutputDir String
     */
    private void executeLegacy(String createXmlCommand, String runLegacyCommand, String pathToXmlFile,
        int experimentId, String experimentName, long finalStep, String localOutputDir) {
        try {
            virtualThreadExecutor.submit(() -> {
                // Execute commands
                final var future = gamaCommandExecutor.executeLegacy(createXmlCommand, runLegacyCommand, pathToXmlFile,
                    experimentId, experimentName, finalStep);

                future.whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error while running simulation", error);

                        clearLocalResource(localOutputDir);
                        return;
                    }

                    // If execution is successful, upload output directory to S3
                    log.info("Simulation completed for experiment: {}", experimentId);

                    clearOldResultInS3(BigInteger.ONE, experimentId);
                    uploadResult(BigInteger.ONE, experimentId, localOutputDir);
                    clearLocalResource(localOutputDir);
                });

                future.join();
            });
        } catch (Exception e) {
            log.error("Error while executing legacy command: ", e);
        }
    }

    /**
     * This method is used to clear local resource.
     *
     * @param localOutputDir String
     */
    private void clearLocalResource(String localOutputDir) {
        // Clear old output directory
        final var processBuilder = new ProcessBuilder();
        try {
            processBuilder.command("bash", "-c", "rm -rf " + localOutputDir).start();
        } catch (Exception e) {
            throw new CannotClearOldSimulationOutputException(e.getMessage());
        }
    }

    /**
     * This method is used to clear old S3 result.
     *
     * @param modelId BigInteger
     * @param experimentId int
     */
    private void clearOldResultInS3(BigInteger modelId, int experimentId) {
        final var outputObjectKey = getOutputObjectKey(modelId, experimentId);

        s3Service.clear(outputObjectKey);
    }

    /**
     * This method is used to upload result to S3.
     *
     * @param modelId BigInteger
     * @param experimentId int
     * @param localOutputDir String
     */
    private void uploadResult(BigInteger modelId, int experimentId, String localOutputDir) {
        final var outputObjectKey = getOutputObjectKey(modelId, experimentId);

        s3Service.uploadDirectory(localOutputDir, outputObjectKey);
    }

    /**
     * This method is used to get output object key.
     *
     * @param modelId BigInteger
     * @param experimentId int
     * @return String
     */
    private String getOutputObjectKey(BigInteger modelId, int experimentId) {
        final var userId = authService.getCurrentUserId();

        return String.format("simulation_results/user_%s/model_%s/experiment_%s", userId, modelId, experimentId);
    }

}
