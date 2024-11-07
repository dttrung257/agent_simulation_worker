package com.uet.agent_simulation_worker.exceptions.errors;

import org.springframework.http.HttpStatus;

/**
 * This class is used to store simulation errors.
 */
public final class SimulationErrors {
    public static final ErrorDetails E_SM_0001 = new ErrorDetails(HttpStatus.BAD_REQUEST,
            "E_SM_0001", "Error when clear old simulation output");
}
