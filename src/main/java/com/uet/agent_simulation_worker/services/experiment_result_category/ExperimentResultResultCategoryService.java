package com.uet.agent_simulation_worker.services.experiment_result_category;

import com.uet.agent_simulation_worker.models.ExperimentResultCategory;
import com.uet.agent_simulation_worker.repositories.ExperimentResultCategoryRepository;
import com.uet.agent_simulation_worker.services.auth.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentResultResultCategoryService implements IExperimentResultCategoryService {
    private final IAuthService authService;
    private final ExperimentResultCategoryRepository experimentResultCategoryRepository;

    @Override
    public List<ExperimentResultCategory> get(
        BigInteger experimentResultId,
        BigInteger experimentId,
        BigInteger modelId,
        BigInteger projectId
    ) {
        return experimentResultCategoryRepository.find(
            experimentResultId,
            experimentId,
            modelId,
            projectId,
            authService.getCurrentUserId()
        );
    }
}
