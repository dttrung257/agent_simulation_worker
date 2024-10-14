package com.uet.agent_simulation_worker;

import com.uet.agent_simulation_worker.models.Node;
import com.uet.agent_simulation_worker.repositories.NodeRepository;
import com.uet.agent_simulation_worker.services.node.INodeService;
import com.uet.agent_simulation_worker.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
@EnableJpaAuditing
public class AgentSimulationWorkerApplication implements CommandLineRunner {
	private final FileUtil fileUtil;
	private final INodeService nodeService;
	private final NodeRepository nodeRepository;

	@Value("${cluster.config.path}")
	private String clusterConfigPath;

	public static void main(String[] args) {
		SpringApplication.run(AgentSimulationWorkerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (!existsEnvFile()) {
			// exit program
			log.error("application.yml not found");
			System.exit(1);
		}

		if (!checkNodeId()) {
			// exit program
			log.error("Error while checking node ID");
			System.exit(1);
		}

		initNode();
	}

	/**
	 * Check if the application.yml file exists.
	 *
	 * @return true if the file exists, false otherwise.
	 */
	private boolean existsEnvFile() {
		return fileUtil.fileExists(clusterConfigPath);
	}

	/**
	 * Check if the node ID is valid.
	 *
	 * @return true if the node ID is valid, false otherwise.
	 */
	private boolean checkNodeId() {
		final var nodeId = fileUtil.getValueByKey(clusterConfigPath, "node_id");
		if (nodeId == null) {
			log.error("An error occurred while getting the node ID");

			return false;
		}

		if (nodeId.isEmpty()) {
			log.info("Node ID undefined in application.yml. Start setting node ID");
			final var newNodeId = generateNodeId().toString();

			fileUtil.findAndWrite(clusterConfigPath, "node_id", newNodeId);
			fileUtil.findAndWrite(clusterConfigPath, "node_role", "2");
			return true;
		}

		try {
			Integer.parseInt(nodeId);
		} catch (NumberFormatException e) {
			log.error("Invalid node ID in application.yml. Node ID must be an integer");

			return false;
		}

		return true;
	}

	private Integer generateNodeId() {
		final var nodeId = nodeRepository.findMaxId();
		if (nodeId == null) {
			return 2;
		}

		return nodeId + 1;
	}

	/**
	 * Initialize node.
	 */
	private void initNode() {
		final var nodeId = fileUtil.getValueByKey(clusterConfigPath, "node_id");
		final var host = fileUtil.getValueByKey(clusterConfigPath, "host");
		final var port = fileUtil.getValueByKey(clusterConfigPath, "port");
		final var nodeName = fileUtil.getValueByKey(clusterConfigPath, "node_name");
		final var node = nodeRepository.findById(Integer.parseInt(nodeId));

		if (node.isEmpty()) {
			nodeRepository.save(Node.builder()
				.id(Integer.parseInt(nodeId))
				.name(nodeName)
				.role(2)
				.host(host)
				.port(Integer.parseInt(port))
				.createdBy("system")
				.updatedBy("system")
				.build());

			return;
		}

		node.get().setName(nodeName);
		node.get().setRole(2);
		node.get().setHost(host);
		node.get().setPort(Integer.parseInt(port));
		node.get().setCreatedBy("system");
		node.get().setUpdatedBy("system");

		nodeRepository.save(node.get());
	}
}
