package com.uet.agent_simulation_worker.pubsub.publisher;

import com.uet.agent_simulation_worker.pubsub.message.master.MasterMessage;

/**
 * This interface is used to publish a message to a Redis channel.
 */
public interface MessagePublisher {
    void publish(MasterMessage message);
}
