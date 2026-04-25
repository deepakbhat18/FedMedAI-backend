
package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.response.AnalyticsResponse;
import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.service.AnalyticsService;
import com.fedlearn.medical.service.MLServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Dashboard",
        description = "APIs for system analytics and dashboard data")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private MLServiceClient mlServiceClient;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard analytics",
            description = "Get comprehensive dashboard data")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getDashboard() {
        AnalyticsResponse analytics = analyticsService.getDashboardAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/ml-service/health")
    @Operation(summary = "ML Service health check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkMLServiceHealth() {
        boolean healthy = mlServiceClient.isHealthy();
        Map<String, Object> status = Map.of(
                "mlServiceStatus", healthy ? "UP" : "DOWN",
                "message", healthy
                        ? "ML Service is running"
                        : "ML Service is not available"
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/ml-metrics")
    @Operation(summary = "Real ML model metrics",
            description = "Get live accuracy, feature importance from ML service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMLMetrics() {
        try {
            Map<String, Object> metrics = mlServiceClient.getFullAnalytics();
            return ResponseEntity.ok(ApiResponse.success("ML metrics loaded", metrics));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(
                    "ML Service unavailable: " + e.getMessage()));
        }
    }
}