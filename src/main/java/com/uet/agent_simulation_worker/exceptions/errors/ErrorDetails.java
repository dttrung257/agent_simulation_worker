package com.uet.agent_simulation_worker.exceptions.errors;

/**
 * Error details class.
 */
public record ErrorDetails(
    int statusCode,
    String errorCode,
    String defaultMessage
) {
}
