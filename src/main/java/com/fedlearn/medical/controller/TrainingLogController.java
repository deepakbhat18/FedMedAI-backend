
package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.dto.response.TrainingLogResponse;
import com.fedlearn.medical.service.TrainingLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training-logs")
@Tag(name = "Training Logs", description = "APIs for viewing FL training history")
public class TrainingLogController {

    @Autowired
    private TrainingLogService trainingLogService;

    @GetMapping
    @Operation(summary = "Get all training logs")
    public ResponseEntity<ApiResponse<List<TrainingLogResponse>>> getAllLogs() {
        List<TrainingLogResponse> logs = trainingLogService.getAllLogs();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/round/{round}")
    @Operation(summary = "Get logs by training round")
    public ResponseEntity<ApiResponse<List<TrainingLogResponse>>> getLogsByRound(
            @PathVariable Integer round) {
        List<TrainingLogResponse> logs = trainingLogService.getLogsByRound(round);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/hospital/{hospitalId}")
    @Operation(summary = "Get logs by hospital")
    public ResponseEntity<ApiResponse<List<TrainingLogResponse>>> getLogsByHospital(
            @PathVariable Long hospitalId) {
        List<TrainingLogResponse> logs = trainingLogService.getLogsByHospital(hospitalId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Training analytics",
            description = "Get training history and accuracy progression")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrainingAnalytics() {
        Map<String, Object> analytics = trainingLogService.getTrainingAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}