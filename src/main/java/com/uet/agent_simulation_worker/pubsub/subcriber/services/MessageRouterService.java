package com.uet.agent_simulation_worker.pubsub.subcriber.services;

import com.uet.agent_simulation_worker.pubsub.PubSubCommands;
import com.uet.agent_simulation_worker.pubsub.message.master.simulation.RunSimulation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageRouterService {
    private final MessageHandler messageHandler;

    /**
     * Handle the message.
     *
     * @param command The command.
     * @param messageData The message data.
     */
    public void handle(String command, Object messageData) {
        switch (command) {
            case PubSubCommands.RUN_SIMULATION:
                messageHandler.runSimulation((RunSimulation) messageData);
                break;
        }
    }
}
