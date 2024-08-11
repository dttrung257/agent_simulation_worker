package com.uet.agent_simulation_worker.commands.executors;

import java.util.concurrent.CompletableFuture;

public interface IGamaCommandExecutor extends ICommandExecutor {
    /**
     * This method is used to execute the legacy mode gama headless.
     *
     * @param createXmlCommand - command to create xml file
     * @param runLegacyCommand - command to run legacy mode
     * @param pathToXmlFile - path to xml file
     * @param experimentId - id of the experiment
     * @param experimentName - name of the experiment
     * @param finalStep - final step of the experiment
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> executeLegacy(String createXmlCommand, String runLegacyCommand, String pathToXmlFile, int experimentId, String experimentName, long finalStep);
}
