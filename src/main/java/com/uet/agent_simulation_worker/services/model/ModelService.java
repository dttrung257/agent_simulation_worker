package com.uet.agent_simulation_worker.services.model;

import com.uet.agent_simulation_worker.models.Model;
import com.uet.agent_simulation_worker.repositories.ModelRepository;
import com.uet.agent_simulation_worker.services.auth.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelService implements IModelService {
    private final IAuthService authService;
    private final ModelRepository modelRepository;

    @Override
    public List<Model> get(BigInteger projectId, Boolean hasExperiment) {
        if (hasExperiment != null) {
            return modelRepository.findByExperimentNotNull(authService.getCurrentUserId(), projectId);
        }

        return modelRepository.find(authService.getCurrentUserId(), projectId);
    }
}
