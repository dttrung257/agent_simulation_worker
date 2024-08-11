package com.uet.agent_simulation_worker.requests.simulation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Create simulation request class.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSimulationRequest {
    private Integer nodeId;

    @Valid
    @NotNull
    private List<CreateExperimentRequest> experiments;
}
