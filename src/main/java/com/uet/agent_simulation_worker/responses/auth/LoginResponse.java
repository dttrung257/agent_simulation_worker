package com.uet.agent_simulation_worker.responses.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
