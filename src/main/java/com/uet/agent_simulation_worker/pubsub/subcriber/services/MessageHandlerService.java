package com.uet.agent_simulation_worker.pubsub.subcriber.services;

import com.uet.agent_simulation_worker.pubsub.message.master.simulation.RunSimulation;
import com.uet.agent_simulation_worker.services.ISimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * This class is used to handle messages from a Redis channel.
 */
@Service
@RequiredArgsConstructor
public class MessageHandlerService implements MessageHandler {
    private final ISimulationService simulationService;

    @Override
    public void runSimulation(RunSimulation messageData) {
        final var simulation = messageData.getSimulation();
        simulationService.run(simulation);
    }
}
