package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.dto.response.JobResponse;
import com.fedlearn.medical.dto.response.ModelVersionResponse;
import com.fedlearn.medical.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/models", "/api/v1/models"})
@Tag(name = "Model Management", description = "APIs for ML model management and FL training")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping
    @Operation(summary = "List all model versions", description = "Get all trained model versions")
    public ResponseEntity<ApiResponse<List<ModelVersionResponse>>> getAllModels() {
        List<ModelVersionResponse> models = modelService.getAllModels();
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get model by ID")
    public ResponseEntity<ApiResponse<ModelVersionResponse>> getModelById(@PathVariable Long id) {
        ModelVersionResponse model = modelService.getModelById(id);
        return ResponseEntity.ok(ApiResponse.success(model));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest active model", description = "Get the currently active global model")
    public ResponseEntity<ApiResponse<ModelVersionResponse>> getLatestModel() {
        ModelVersionResponse model = modelService.getLatestModel();
        return ResponseEntity.ok(ApiResponse.success(model));
    }

    @PostMapping({"/train", "/trigger-training"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger federated learning round asynchronously", description = "Admin only - Immediately returns HTTP 202 Accepted with a unique jobId for status polling")
    public ResponseEntity<ApiResponse<JobResponse>> triggerTraining(
            @Valid @RequestBody TrainingTriggerRequest request) {
        JobResponse jobResponse = modelService.triggerTrainingAsync(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Federated learning training round initiated asynchronously", jobResponse));
    }
}