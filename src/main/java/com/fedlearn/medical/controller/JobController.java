package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.dto.response.JobResponse;
import com.fedlearn.medical.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/jobs", "/api/v1/jobs"})
@Tag(name = "Job Management", description = "APIs for tracking async background job execution status")
public class JobController {

    @Autowired
    private ModelService modelService;

    @GetMapping("/{jobId}")
    @Operation(summary = "Get background job status", description = "Poll asynchronous background job progress by jobId")
    public ResponseEntity<ApiResponse<JobResponse>> getJobStatus(@PathVariable String jobId) {
        JobResponse response = modelService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
