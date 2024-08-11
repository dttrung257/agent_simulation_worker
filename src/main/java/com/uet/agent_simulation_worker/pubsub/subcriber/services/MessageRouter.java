package com.uet.agent_simulation_worker.pubsub.subcriber.services;

public interface MessageRouter {
    void handle(String command, Object messageData);
}
