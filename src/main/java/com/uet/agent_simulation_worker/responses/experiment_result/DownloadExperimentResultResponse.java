package com.uet.agent_simulation_worker.responses.experiment_result;

import org.springframework.core.io.InputStreamResource;

public record DownloadExperimentResultResponse(
        InputStreamResource resource,
        String filename,
        Long fileSize
) {}
