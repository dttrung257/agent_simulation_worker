package com.uet.agent_simulation_worker.exceptions.handlers;

import com.uet.agent_simulation_worker.constant.AppConst;
import com.uet.agent_simulation_worker.exceptions.ErrorResponse;
import com.uet.agent_simulation_worker.exceptions.errors.SimulationErrors;
import com.uet.agent_simulation_worker.exceptions.simulation.CannotClearOldSimulationOutputException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimulationExceptionHandler {
    /**
     * Handle CannotClearOldSimulationOutputException
     *
     * @param e CannotClearOldSimulationOutputException
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(CannotClearOldSimulationOutputException.class)
    public ResponseEntity<ErrorResponse> handleCannotClearOldSimulationOutputException(CannotClearOldSimulationOutputException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponse(
                        AppConst.ERROR,
                        SimulationErrors.E_SM_0001.statusCode(),
                        SimulationErrors.E_SM_0001.errorCode(),
                        SimulationErrors.E_SM_0001.defaultMessage(),
                        e.getMessage()
                )
        );
    }
}
