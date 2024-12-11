package com.uet.agent_simulation_worker.pubsub.message.master.simulation;

import com.uet.agent_simulation_worker.pubsub.message.master.MasterMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeleteSimulationResult extends MasterMessage {
    private BigInteger experimentResultId;
}
