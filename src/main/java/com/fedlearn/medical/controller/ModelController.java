package com.fedlearn.medical.controller;
import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.dto.response.ApiResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/api/models")
@Tag(name = "Model Management", description = "APIs for ML model management and FL training")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping
    @Operation(summary = "List all model versions",
            description = "Get all trained model versions")
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
    @Operation(summary = "Get latest active model",
            description = "Get the currently active global model")
    public ResponseEntity<ApiResponse<ModelVersionResponse>> getLatestModel() {
        ModelVersionResponse model = modelService.getLatestModel();
        return ResponseEntity.ok(ApiResponse.success(model));
    }

    @PostMapping("/trigger-training")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger federated learning round",
            description = "Admin only - Start a new FL training round across hospitals")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTraining(
            @Valid @RequestBody TrainingTriggerRequest request) {
        Map<String, Object> result = modelService.triggerTraining(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Federated learning round initiated", result));
    }
}