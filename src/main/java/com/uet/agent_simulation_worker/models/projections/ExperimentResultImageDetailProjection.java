package com.uet.agent_simulation_worker.models.projections;

import java.math.BigInteger;

public interface ExperimentResultImageDetailProjection {
    BigInteger getId();
    Integer getNodeId();
    String getLocation();
    String getExtension();
}
