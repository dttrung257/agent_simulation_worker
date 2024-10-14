package com.uet.agent_simulation_worker.services.simulation;

import com.uet.agent_simulation_worker.requests.simulation.CreateSimulationRequest;

/**
 * Simulation service interface.
 */
public interface ISimulationService {
    /**
     * This method is used to run simulation.
     */
    void run();

    /**
     * This method is used to run simulation with a specific request.
     *
     * @param request CreateSimulationRequest
     */
    void run(CreateSimulationRequest request);
}
