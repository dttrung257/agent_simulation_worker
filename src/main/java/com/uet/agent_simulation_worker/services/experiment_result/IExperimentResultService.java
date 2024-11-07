package com.uet.agent_simulation_worker.services.experiment_result;

import com.uet.agent_simulation_worker.models.Experiment;
import com.uet.agent_simulation_worker.models.ExperimentResult;
import com.uet.agent_simulation_worker.responses.experiment_result.DownloadExperimentResultResponse;
import com.uet.agent_simulation_worker.responses.experiment_result.ExperimentProgressResponse;

import java.math.BigInteger;
import java.util.List;

public interface IExperimentResultService {
    List<ExperimentResult> get(BigInteger experimentId, BigInteger modelId, BigInteger projectId);

    void updateStatus(ExperimentResult experimentResult, int status);

    ExperimentProgressResponse getExperimentProgress(BigInteger id);

    DownloadExperimentResultResponse download(BigInteger id);

    void stop(BigInteger id);
}
