package com.uet.agent_simulation_worker.responses;

import java.math.BigInteger;

/**
 * SuccessResponse class
 */
public record SuccessResponse(
        String status,
        int statusCode,
        BigInteger total,
        Object data
) { }
