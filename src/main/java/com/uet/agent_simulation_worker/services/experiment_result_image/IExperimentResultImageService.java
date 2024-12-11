package com.uet.agent_simulation_worker.services.experiment_result_image;

import com.uet.agent_simulation_worker.models.ExperimentResultImage;
import com.uet.agent_simulation_worker.responses.Pagination;
import com.uet.agent_simulation_worker.responses.experiment_result_image.ExperimentResultImageDetailResponse;
import com.uet.agent_simulation_worker.responses.experiment_result_image.ExperimentResultImageListResponse;
import reactor.core.publisher.Flux;

import java.math.BigInteger;
import java.util.List;

public interface IExperimentResultImageService {
    Pagination<List<ExperimentResultImage>> get(BigInteger experimentResultId, BigInteger experimentId, BigInteger modelId,
        BigInteger projectId, BigInteger experimentResultCategoryId, Integer step, Integer page, Integer pageSize,
        String orderBy, String orderDirection);

    ExperimentResultImageDetailResponse getImageData(BigInteger id);

    String getImageDataEncoded(BigInteger id);

    ExperimentResultImageListResponse getByRange(BigInteger experimentResultId, Integer startStep, Integer endStep);

    Flux<ExperimentResultImageListResponse> getAnimatedImages(BigInteger experimentResultId, Integer startStep, Integer endStep, long duration);

    Flux<ExperimentResultImageListResponse> getMultiExperimentAnimatedImages(
        String experimentResultIds,
        Integer startStep,
        Integer endStep,
        long duration,
        String categoryIds
    );
}
