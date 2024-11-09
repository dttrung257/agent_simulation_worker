package com.uet.agent_simulation_worker.pubsub.subcriber.services;

import com.uet.agent_simulation_worker.pubsub.message.master.simulation.DeleteSimulationResult;
import com.uet.agent_simulation_worker.pubsub.message.master.simulation.RunSimulation;
import com.uet.agent_simulation_worker.services.experiment_result.IExperimentResultService;
import com.uet.agent_simulation_worker.services.simulation.ISimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * This class is used to handle messages from a Redis channel.
 */
@Service
@RequiredArgsConstructor
public class MessageHandlerService implements MessageHandler {
    private final ISimulationService simulationService;
    private final IExperimentResultService experimentResultService;

    @Override
    public void runSimulation(RunSimulation messageData) {
        final var simulation = messageData.getSimulation();
        simulationService.run(simulation);
    }


    @Override
    public void deleteSimulationResult(DeleteSimulationResult messageData) {
        final var experimentResultId = messageData.getExperimentResultId();
        experimentResultService.delete(experimentResultId);
    }
}
