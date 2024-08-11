package com.uet.agent_simulation_worker.responses;

import com.uet.agent_simulation_worker.constant.AppConst;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ResponseHandler {
    /**
     * This method is used to response success for this API.
     *
     * @param data Object
     *
     * @return ResponseEntity<SuccessResponse>
     */
    public ResponseEntity<SuccessResponse> respondSuccess(Object data) {
        return ResponseEntity.ok(new SuccessResponse(AppConst.SUCCESS, HttpStatus.OK.value(), data));
    }

    /**
     * This method is used to response success for this API.
     *
     * @param httpStatus HttpStatus
     * @param data Object
     *
     * @return ResponseEntity<SuccessResponse>
     */
    public ResponseEntity<SuccessResponse> respondSuccess(HttpStatus httpStatus, Object data) {
        return ResponseEntity.ok(new SuccessResponse(AppConst.SUCCESS, httpStatus.value(), data));
    }
}
