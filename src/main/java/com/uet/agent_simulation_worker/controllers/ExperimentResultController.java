package com.uet.agent_simulation_worker.controllers;

import com.uet.agent_simulation_worker.responses.ResponseHandler;
import com.uet.agent_simulation_worker.responses.experiment_result.ExperimentProgressResponse;
import com.uet.agent_simulation_worker.services.experiment_result.IExperimentResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;

@RestController
@RequestMapping("/api/v1/experiment_results")
@RequiredArgsConstructor
public class ExperimentResultController {
    private final ResponseHandler responseHandler;
    private final IExperimentResultService experimentResultService;

    /**
     * Check process.
     *
     * @return ResponseEntity<SuccessResponse>
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<ExperimentProgressResponse> getExperimentProgress(@PathVariable BigInteger id) {
        return ResponseEntity.ok(experimentResultService.getExperimentProgress(id));
    }
}