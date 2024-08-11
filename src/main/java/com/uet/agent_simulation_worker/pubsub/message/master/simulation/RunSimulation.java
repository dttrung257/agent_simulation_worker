package com.uet.agent_simulation_worker.pubsub.message.master.simulation;

import com.uet.agent_simulation_worker.pubsub.message.master.MasterMessage;
import com.uet.agent_simulation_worker.requests.simulation.CreateSimulationRequest;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RunSimulation extends MasterMessage {
    private CreateSimulationRequest simulation;
}
