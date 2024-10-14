package com.uet.agent_simulation_worker.services.experiment;

import com.uet.agent_simulation_worker.models.Experiment;

import java.math.BigInteger;
import java.util.List;

public interface IExperimentService {
    List<Experiment> get(BigInteger projectId, BigInteger modelId);
}
