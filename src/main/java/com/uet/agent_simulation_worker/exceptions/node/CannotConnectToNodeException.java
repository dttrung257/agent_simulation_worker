package com.uet.agent_simulation_worker.exceptions.node;

public class CannotConnectToNodeException extends RuntimeException {
    public CannotConnectToNodeException(String message) {
        super(message);
    }
}
