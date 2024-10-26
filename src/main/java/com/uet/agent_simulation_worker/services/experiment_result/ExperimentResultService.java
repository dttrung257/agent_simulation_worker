package com.uet.agent_simulation_worker.services.experiment_result;

import com.uet.agent_simulation_worker.constant.ExperimentResultStatusConst;
import com.uet.agent_simulation_worker.exceptions.errors.ExperimentResultErrors;
import com.uet.agent_simulation_worker.exceptions.experiment_result.ExperimentResultNotFoundException;
import com.uet.agent_simulation_worker.exceptions.node.CannotFetchNodeDataException;
import com.uet.agent_simulation_worker.models.Experiment;
import com.uet.agent_simulation_worker.models.ExperimentResult;
import com.uet.agent_simulation_worker.repositories.ExperimentResultRepository;
import com.uet.agent_simulation_worker.responses.experiment_result.DownloadExperimentResultResponse;
import com.uet.agent_simulation_worker.responses.experiment_result.ExperimentProgressResponse;
import com.uet.agent_simulation_worker.services.auth.IAuthService;
import com.uet.agent_simulation_worker.services.node.INodeService;
import com.uet.agent_simulation_worker.utils.FileUtil;
import com.uet.agent_simulation_worker.utils.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperimentResultService implements IExperimentResultService {
    private final FileUtil fileUtil;
    private final ThreadUtil threadUtil;
    private final IAuthService authService;
    private final INodeService nodeService;
    private final ExperimentResultRepository experimentResultRepository;

    @Override
    public List<ExperimentResult> get(BigInteger experimentId, BigInteger modelId, BigInteger projectId) {
        return experimentResultRepository.find(authService.getCurrentUserId(), experimentId, modelId, projectId);
    }

    @Override
    public ExperimentResult recreate(Experiment experiment, long finalStep, String outputDir) {
        experimentResultRepository.deleteByExperimentId(experiment.getId());

        // Save new experiment result.
        return experimentResultRepository.save(ExperimentResult.builder()
                .experiment(experiment)
                .location(outputDir)
                .finalStep((int) finalStep)
                .node(nodeService.getCurrentNode())
                .status(ExperimentResultStatusConst.PENDING)
                .build());
    }

    @Override
    public void updateStatus(ExperimentResult experimentResult, int status) {
        experimentResult.setStatus(status);

        experimentResultRepository.save(experimentResult);
    }

    @Override
    public ExperimentProgressResponse getExperimentProgress(BigInteger id) {
        final var experimentResult = experimentResultRepository.findById(id);
        if (experimentResult.isEmpty()) {
            throw new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage());
        }

        final var currentNodeId = nodeService.getCurrentNodeId();
        if (!nodeService.getCurrentNodeId().equals(experimentResult.get().getNodeId())) {
            return getExperimentProgressFromNode(id, experimentResult.get().getNodeId());
        }

        if (!Files.exists(Paths.get(experimentResult.get().getLocation()))) {
            return new ExperimentProgressResponse(true, experimentResult.get().getStatus(), null,
                    experimentResult.get().getFinalStep());
        }

        final var consoleOutputFileName = fileUtil.getFileNameByPrefix(experimentResult.get().getLocation(), "console-output");
        final var simulationOutputLastLine = fileUtil.readLastLine(experimentResult.get().getLocation() + "/" + consoleOutputFileName, "step");

        var step = extractStep(simulationOutputLastLine);
        if (step < 0 || step == 0) {
            step = 0;
        } else if (!step.equals(experimentResult.get().getFinalStep())) {
            step -= 1;
        }

        return new ExperimentProgressResponse(false, experimentResult.get().getStatus(), step, experimentResult.get().getFinalStep());
    }

    private Integer extractStep(String line) {
        int stepNumber = -1;
        try {
            final var pattern = Pattern.compile("step\\s+(\\d+)");
            final var matcher = pattern.matcher(line);

            if (matcher.find()) {
                stepNumber = Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.error("Error while extracting step number: {}", e.getMessage());
        }

        return stepNumber;
    }

    private ExperimentProgressResponse getExperimentProgressFromNode(BigInteger resultId, Integer nodeId) {
        final var webClient = nodeService.getWebClientByNodeId(nodeId);

        return fetchExperimentProgress(webClient, resultId);
    }

    private ExperimentProgressResponse fetchExperimentProgress(WebClient webClient, BigInteger resultId) {
        try {
            final var response = webClient.get().uri("/api/v1/experiment_results/" + resultId + "/progress")
                    .retrieve().bodyToMono(ExperimentProgressResponse.class).block();

            if (response == null) {
                throw new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Error while fetching experiment progress: {}", e.getMessage());

            throw new CannotFetchNodeDataException(e.getMessage());
        }
    }

    @Override
    public DownloadExperimentResultResponse download(BigInteger id) {
        final var experimentResult = experimentResultRepository.findById(id);
        if (experimentResult.isEmpty()) {
            throw new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage());
        }

        if (!nodeService.getCurrentNodeId().equals(experimentResult.get().getNodeId())) {
            throw new ExperimentResultNotFoundException("Experiment result is not in current node");
        }

        final var path = Paths.get(experimentResult.get().getLocation() + ".zip");
        final var file = path.toFile();
        InputStreamResource resource;
        try {
            resource = new InputStreamResource(new FileInputStream(file));
        } catch (Exception e) {
            throw new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage());
        }

        return new DownloadExperimentResultResponse(resource, file.getName(), file.length());
    }

    @Override
    public void stop(BigInteger id) {
        final var experimentResult = experimentResultRepository.findById(id);
        if (experimentResult.isEmpty()) {
            throw new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage());
        }

        if (!nodeService.getCurrentNodeId().equals(experimentResult.get().getNodeId())) {
            final var webClient = nodeService.getWebClientByNodeId(experimentResult.get().getNodeId());
            webClient.delete().uri("/api/v1/experiment_results/" + id + "/stop").retrieve().bodyToMono(String.class).block();

            return;
        }

        if (experimentResult.get().getRunCommandPid() == null) {
            return;
        }

        threadUtil.killProcessById(experimentResult.get().getRunCommandPid());
    }
}
