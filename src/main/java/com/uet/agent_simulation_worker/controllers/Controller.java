package com.uet.agent_simulation_worker.controllers;

import com.uet.agent_simulation_worker.responses.ResponseHandler;
import com.uet.agent_simulation_worker.responses.SuccessResponse;
import com.uet.agent_simulation_worker.services.node.INodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class Controller {
    private final INodeService nodeService;
    private final ResponseHandler ResponseHandler;

    @Value("${server.port}")
    private int port;

    @Value("${spring.profiles.active}")
    private String profile;

    @GetMapping("/health")
    public ResponseEntity<SuccessResponse> health() {
        return ResponseHandler.respondSuccess("Ok");
    }

    @GetMapping("/current_node")
    public ResponseEntity<SuccessResponse> getCurrentNode() {
        return ResponseHandler.respondSuccess(nodeService.getCurrentNode());
    }

    @GetMapping("/webclient_test")
    public ResponseEntity<SuccessResponse> webClientTest() {
        final var httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(3));
        final var webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://localhost:9998").build();

        try {
            String res = webClient.get()
                    .uri("/api/v1/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return ResponseHandler.respondSuccess(res);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
