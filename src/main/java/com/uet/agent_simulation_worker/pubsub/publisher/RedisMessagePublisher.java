package com.uet.agent_simulation_worker.pubsub.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uet.agent_simulation_worker.pubsub.message.master.MasterMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

/**
 * This class is used to publish a message to a Redis channel.
 */
@Service
@RequiredArgsConstructor
public class RedisMessagePublisher implements MessagePublisher {
    private final ChannelTopic topic;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void publish(MasterMessage message) {
        var messageJson = "";
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        redisTemplate.convertAndSend(topic.getTopic(), messageJson);
    }
}
