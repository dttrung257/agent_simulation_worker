package com.uet.agent_simulation_worker;

import com.uet.agent_simulation_worker.repositories.NodeRepository;
import com.uet.agent_simulation_worker.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
	private final NodeRepository nodeRepository;

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
	}

	/**
	 * Check if the application.yml file exists.
	 *
	 * @return true if the file exists, false otherwise.
	 */
	private boolean existsEnvFile() {
		return fileUtil.fileExists("src/main/resources/application.yml");
	}

	private boolean checkNodeId() {
		final var nodeId = fileUtil.getValueByKey("src/main/resources/application.yml", "node_id");
		if (nodeId == null) {
			log.error("An error occurred while getting the node ID");

			return false;
		}

		if (nodeId.isEmpty()) {
			log.info("Node ID undefined in application.yml. Start setting node ID");
			final var newNodeId = generateNodeId().toString();

			fileUtil.findAndWrite("src/main/resources/application.yml", "node_id", newNodeId);
			fileUtil.findAndWrite("src/main/resources/application.yml", "node_role", "2");
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
}
