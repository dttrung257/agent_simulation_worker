package com.uet.agent_simulation_worker.services.experiment;

import com.uet.agent_simulation_worker.models.Experiment;
import com.uet.agent_simulation_worker.repositories.ExperimentRepository;
import com.uet.agent_simulation_worker.services.auth.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperimentService implements IExperimentService {
    private final IAuthService authService;
    private final ExperimentRepository experimentRepository;

    @Override
    public List<Experiment> get(BigInteger projectId, BigInteger modelId) {
        return experimentRepository.find(authService.getCurrentUserId(), projectId, modelId);
    }
}
