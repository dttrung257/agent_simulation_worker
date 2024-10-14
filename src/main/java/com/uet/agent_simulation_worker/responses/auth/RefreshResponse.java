package com.uet.agent_simulation_worker.responses.auth;

public record RefreshResponse (
    String accessToken,
    String refreshToken
) {}
