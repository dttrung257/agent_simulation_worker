package com.uet.agent_simulation_worker.repositories;

import com.uet.agent_simulation_worker.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, BigInteger> {
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
