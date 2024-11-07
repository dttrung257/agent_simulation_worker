package com.uet.agent_simulation_worker.services.model;

import com.uet.agent_simulation_worker.models.Model;

import java.math.BigInteger;
import java.util.List;

public interface IModelService {
    List<Model> get(BigInteger projectId, Boolean hasExperiment);
}
