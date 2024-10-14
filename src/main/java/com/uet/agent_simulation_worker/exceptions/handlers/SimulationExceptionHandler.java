package com.uet.agent_simulation_worker.exceptions.handlers;

import com.uet.agent_simulation_worker.exceptions.ErrorResponse;
import com.uet.agent_simulation_worker.exceptions.errors.SimulationErrors;
import com.uet.agent_simulation_worker.exceptions.simulation.CannotClearOldSimulationOutputException;
import com.uet.agent_simulation_worker.responses.ResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SimulationExceptionHandler {
    private final ResponseHandler responseHandler;

    /**
     * Handle CannotClearOldSimulationOutputException
     *
     * @param e CannotClearOldSimulationOutputException
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(CannotClearOldSimulationOutputException.class)
    public ResponseEntity<ErrorResponse> handleCannotClearOldSimulationOutputException(CannotClearOldSimulationOutputException e) {
        return responseHandler.respondError(e, SimulationErrors.E_SM_0001);
    }
}
