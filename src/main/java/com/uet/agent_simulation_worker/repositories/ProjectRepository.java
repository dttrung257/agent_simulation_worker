package com.uet.agent_simulation_worker.repositories;

import com.uet.agent_simulation_worker.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface ProjectRepository extends JpaRepository<Project, BigInteger> {
}
