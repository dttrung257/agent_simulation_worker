package com.uet.agent_simulation_worker.services.node;

import com.uet.agent_simulation_worker.exceptions.errors.NodeErrors;
import com.uet.agent_simulation_worker.exceptions.node.CannotConnectToNodeException;
import com.uet.agent_simulation_worker.exceptions.node.NodeNotFoundException;
import com.uet.agent_simulation_worker.models.Node;
import com.uet.agent_simulation_worker.repositories.NodeRepository;
import com.uet.agent_simulation_worker.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeService implements INodeService {
    private final FileUtil fileUtil;
    private final NodeRepository nodeRepository;

    @Value("${cluster.config.path}")
    private String clusterConfigPath;

    @Override
    public Node getCurrentNode() {
        final var nodeId = fileUtil.getValueByKey(clusterConfigPath, "node_id");

        return nodeRepository.findById(Integer.parseInt(nodeId)).orElse(null);
    }

    @Override
    public Integer getCurrentNodeId() {
        try {
            final var nodeId = fileUtil.getValueByKey(clusterConfigPath, "node_id");

            return Integer.parseInt(nodeId);
        } catch (Exception e) {
            log.error("Error while getting current node id: {}", e.getMessage());

            return null;
        }
    }

    @Override
    public Node getNodeById(Integer nodeId) {
        return nodeRepository.findById(nodeId).orElseThrow(() ->
                new NodeNotFoundException(NodeErrors.E_NODE_0001.defaultMessage()));
    }

    @Override
    public WebClient getWebClientByNodeId(Integer nodeId) {
        final var node = getNodeById(nodeId);
        try {
            final var httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(3));
            final var url = getNodeUrl(node);

            return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                    .baseUrl(url).build();
        } catch (Exception e) {
            throw new CannotConnectToNodeException("Cannot connect to node: " + node.getName());
        }
    }

    private String getNodeUrl(Node node) {
        return "http://" + node.getHost() + ":" + node.getPort();
    }
}
