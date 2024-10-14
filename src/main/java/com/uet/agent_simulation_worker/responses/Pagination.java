package com.uet.agent_simulation_worker.responses;

import java.math.BigInteger;

public record Pagination<T> (
    BigInteger total,
    T data
) {}
