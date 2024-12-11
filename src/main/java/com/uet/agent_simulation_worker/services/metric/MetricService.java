package com.uet.agent_simulation_worker.services.metric;

import com.uet.agent_simulation_worker.services.node.INodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetricService implements IMetricService {
    private final INodeService nodeService;

    @Override
    public Double getCpuUsage() {
        final var currentNode = nodeService.getCurrentNode();
        final var cpuUsageEndpoint = "http://" + currentNode.getHost() + ":" + currentNode.getPort() + "/actuator/metrics/system.cpu.usage";

        return nodeService.getWebClientByNodeId(nodeService.getCurrentNodeId())
                .get()
                .uri(cpuUsageEndpoint)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> measurements = (List<Map<String, Object>>) response.get("measurements");
                    return (Double) measurements.getFirst().get("value");
                })
                .block();
    }

    @Override
    public Double getRamUsage() {
        final var currentNode = nodeService.getCurrentNode();
        final var ramUsageEndpoint = "http://" + currentNode.getHost() + ":" + currentNode.getPort() + "/actuator/metrics/jvm.memory.used";

        return nodeService.getWebClientByNodeId(nodeService.getCurrentNodeId())
                .get()
                .uri(ramUsageEndpoint)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> measurements = (List<Map<String, Object>>) response.get("measurements");
                    return (Double) measurements.getFirst().get("value");
                })
                .block();
    }
}
