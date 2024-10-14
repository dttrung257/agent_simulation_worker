package com.uet.agent_simulation_worker.responses.experiment_result;

public record ExperimentProgressResponse(
    Boolean waiting,
    Integer status,
    Integer currentStep,
    Integer finalStep
) {}
