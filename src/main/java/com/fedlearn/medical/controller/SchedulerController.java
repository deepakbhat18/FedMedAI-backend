package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.service.TrainingSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@Tag(name = "Training Scheduler",
        description = "APIs for managing automatic FL training scheduler")
public class SchedulerController {

    @Autowired
    private TrainingSchedulerService schedulerService;

    @GetMapping("/status")
    @Operation(
            summary = "Get scheduler status",
            description = "Get current status of the auto training scheduler"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = schedulerService.getStatus();
        return ResponseEntity.ok(ApiResponse.success(
                "Scheduler status retrieved", status));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Manually trigger training",
            description = "Admin only - Manually trigger a federated learning round"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerManually() {
        Map<String, Object> result = schedulerService.triggerManually();
        boolean success = Boolean.TRUE.equals(result.get("success"))
                || "COMPLETED".equals(result.get("status"));
        String message = success
                ? "Training round triggered successfully"
                : "Training trigger failed: " + result.getOrDefault("message", "Unknown error");
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get scheduler run history",
            description = "Get history of all scheduled training runs"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory() {
        try {
            Map<String, Object> status = schedulerService.getStatus();


            Map<String, Object> history = new HashMap<>();
            history.put("runHistory",
                    status.getOrDefault("runHistory",
                            new java.util.ArrayList<>()));
            history.put("totalRuns",
                    status.getOrDefault("totalScheduledRuns", 0));
            history.put("successfulRuns",
                    status.getOrDefault("successfulRuns", 0));
            history.put("failedRuns",
                    status.getOrDefault("failedRuns", 0));
            history.put("lastRunTime",
                    status.getOrDefault("lastRunTime", "Never"));
            history.put("lastRunStatus",
                    status.getOrDefault("lastRunStatus", "NEVER"));
            history.put("lastRunMessage",
                    status.getOrDefault("lastRunMessage",
                            "No training has been scheduled yet"));

            return ResponseEntity.ok(ApiResponse.success(
                    "Scheduler history retrieved", history));

        } catch (Exception e) {

            Map<String, Object> emptyHistory = new HashMap<>();
            emptyHistory.put("runHistory",    new java.util.ArrayList<>());
            emptyHistory.put("totalRuns",     0);
            emptyHistory.put("successfulRuns",0);
            emptyHistory.put("failedRuns",    0);
            emptyHistory.put("lastRunTime",   "Never");
            emptyHistory.put("lastRunStatus", "NEVER");
            emptyHistory.put("lastRunMessage","No training runs yet");

            return ResponseEntity.ok(ApiResponse.success(
                    "No history available", emptyHistory));
        }
    }
}