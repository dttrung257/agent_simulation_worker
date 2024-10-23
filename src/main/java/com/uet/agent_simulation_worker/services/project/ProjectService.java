package com.uet.agent_simulation_worker.services.project;

import com.uet.agent_simulation_worker.models.Project;
import com.uet.agent_simulation_worker.repositories.ProjectRepository;
import com.uet.agent_simulation_worker.services.auth.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService implements IProjectService {
    private final IAuthService authService;
    private final ProjectRepository projectRepository;

    @Override
    public List<Project> get() {
        return projectRepository.find(authService.getCurrentUserId());
    }

    @Override
    public Optional<Project> getProject(BigInteger id) {
        return projectRepository.findById(id);
    }
}
