package com.uet.agent_simulation_worker.requests.simulation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create experiment request class.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperimentRequest {
    @Min(value = 0, message = "Experiment id must be greater than or equal to 1")
    @NotNull
    private Integer id;

    @NotBlank(message = "GAML file must not be blank")
    private String gamlFile;

    @NotBlank(message = "Experiment name must not be blank")
    private String experimentName;

    @Min(value = 1, message = "Final step must be greater than or equal to 1")
    private Long finalStep;
}
