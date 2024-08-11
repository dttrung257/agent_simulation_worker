package com.uet.agent_simulation_worker.exceptions.errors;

import org.springframework.http.HttpStatus;

/**
 * This class is used to store common errors.
 */
public final class CommonErrors {
    // Internal server error
    public static final ErrorDetails E0001 = new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.value(), "E0001", "Internal Server Error");

    // Validation error
    public static final ErrorDetails E0002 = new ErrorDetails(HttpStatus.BAD_REQUEST.value(), "E0002", "Validation Error");
}
