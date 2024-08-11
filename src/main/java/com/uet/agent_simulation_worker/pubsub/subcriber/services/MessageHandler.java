package com.uet.agent_simulation_worker.pubsub.subcriber.services;

import com.uet.agent_simulation_worker.pubsub.message.master.simulation.RunSimulation;

public interface MessageHandler {
    /**
     * Worker run simulation.
     *
     * @param messageData The message data.
     */
    void runSimulation(RunSimulation messageData);
}
