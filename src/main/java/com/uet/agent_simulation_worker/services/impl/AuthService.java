package com.uet.agent_simulation_worker.services.impl;

import com.uet.agent_simulation_worker.services.IAuthService;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class AuthService implements IAuthService {
    public BigInteger getCurrentUserId() {
        return BigInteger.valueOf(1);
    }
}
