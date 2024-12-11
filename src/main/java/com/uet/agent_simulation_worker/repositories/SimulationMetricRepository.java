package com.uet.agent_simulation_worker.repositories;

import com.uet.agent_simulation_worker.models.SimulationMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface SimulationMetricRepository extends JpaRepository<SimulationMetric, BigInteger> {
}
