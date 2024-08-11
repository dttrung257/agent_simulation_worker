package com.uet.agent_simulation_worker.exceptions;

/**
 * Error response class.
 */
public record ErrorResponse(
    String status,
    int statusCode,
    String errorCode,
    String message,
    Object details
) {
}
