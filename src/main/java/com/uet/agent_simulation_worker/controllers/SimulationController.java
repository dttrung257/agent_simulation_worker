//package com.uet.agent_simulation_worker.controllers;
//
//import com.uet.agent_simulation_worker.pubsub.PubSubCommands;
//import com.uet.agent_simulation_worker.pubsub.message.master.simulation.RunSimulation;
//import com.uet.agent_simulation_worker.pubsub.publisher.MessagePublisher;
//import com.uet.agent_simulation_worker.requests.simulation.CreateClusterSimulationRequest;
//import com.uet.agent_simulation_worker.requests.simulation.CreateSimulationRequest;
//import com.uet.agent_simulation_worker.responses.ResponseHandler;
//import com.uet.agent_simulation_worker.responses.SuccessResponse;
//import com.uet.agent_simulation_worker.services.ISimulationService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@Slf4j
//@RequestMapping("/api/v1/simulations")
//@RequiredArgsConstructor
//public class SimulationController {
//    private final MessagePublisher messagePublisher;
//    private final ResponseHandler responseHandler;
//    private final ISimulationService simulationService;
//
//    @PostMapping
//    public ResponseEntity<SuccessResponse> runSimulation(@Valid @RequestBody CreateSimulationRequest request) {
////        simulationService.run(request);
//        final var message = RunSimulation.builder()
//                .nodeId(1)
//                .command(PubSubCommands.RUN_SIMULATION)
//                .simulation(request)
//                .build();
//
//        messagePublisher.publish(message);
//
//        return responseHandler.respondSuccess("Simulation is running");
//    }
//
//    @PostMapping("/cluster")
//    public ResponseEntity<SuccessResponse> runSimulationCluster(@Valid @RequestBody CreateClusterSimulationRequest request) {
//        request.getSimulationRequests().forEach((simulationRequest) -> {
//            final var message = RunSimulation.builder()
//                    .nodeId(simulationRequest.getNodeId())
//                    .command(PubSubCommands.RUN_SIMULATION)
//                    .simulation(simulationRequest)
//                    .build();
//
//            messagePublisher.publish(message);
//        });
//
//        return responseHandler.respondSuccess("Simulation is running");
//    }
//
//    @GetMapping("/publish")
//    public ResponseEntity<SuccessResponse> publishSimulations() {
////        messagePublisher.publish("Hello from Redis!");
//
//        return responseHandler.respondSuccess("Simulation is published");
//    }
//}
