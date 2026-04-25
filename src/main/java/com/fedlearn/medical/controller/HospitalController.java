package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.request.HospitalEmailRequest;
import com.fedlearn.medical.dto.request.HospitalRequest;
import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.dto.response.HospitalResponse;
import com.fedlearn.medical.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
@Tag(name = "Hospital Management", description = "APIs for managing hospitals")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    private static final Logger logger =
            LoggerFactory.getLogger(HospitalController.class);

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register new hospital", description = "Admin only - register a new hospital")
    public ResponseEntity<ApiResponse<HospitalResponse>> createHospital(
            @Valid @RequestBody HospitalRequest request) {
        HospitalResponse response = hospitalService.createHospital(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hospital registered successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all hospitals", description = "Get all registered hospitals")
    public ResponseEntity<ApiResponse<List<HospitalResponse>>> getAllHospitals() {
        List<HospitalResponse> hospitals = hospitalService.getAllHospitals();
        return ResponseEntity.ok(ApiResponse.success(hospitals));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hospital by ID", description = "Get hospital details by ID")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalById(@PathVariable Long id) {
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(ApiResponse.success(hospital));
    }

    @GetMapping("/my-hospital")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Get my hospital", description = "Get hospital linked to current user")
    public ResponseEntity<ApiResponse<HospitalResponse>> getMyHospital(
            @RequestParam Long userId) {
        HospitalResponse hospital = hospitalService.getHospitalByAdminUser(userId);
        return ResponseEntity.ok(ApiResponse.success(hospital));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update hospital", description = "Admin only - update hospital details")
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospital(
            @PathVariable Long id,
            @Valid @RequestBody HospitalRequest request) {
        HospitalResponse response = hospitalService.updateHospital(id, request);
        return ResponseEntity.ok(ApiResponse.success("Hospital updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate hospital", description = "Admin only - deactivate a hospital")
    public ResponseEntity<ApiResponse<Void>> deleteHospital(@PathVariable Long id) {
        hospitalService.deleteHospital(id);
        return ResponseEntity.ok(ApiResponse.success("Hospital deactivated successfully", null));
    }

    @PostMapping("/{hospitalId}/email-report")
    @Operation(
            summary = "Email medical report to patient",
            description = "Send PDF medical report from hospital " +
                    "to patient email. Doctor enters patient " +
                    "email at send time - not stored in system."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> emailPatientReport(
            @PathVariable Long hospitalId,
            @Valid @RequestBody HospitalEmailRequest request) {
        try {
            logger.info("Email report requested: hospital={}, to={}",
                    hospitalId, request.getPatientEmail());

            Map<String, Object> result =
                    hospitalService.emailPatientReport(hospitalId, request);

            return ResponseEntity.ok(ApiResponse.success(
                    "Medical report sent successfully from hospital",
                    result));

        } catch (Exception e) {
            logger.error("Email report failed: {}", e.getMessage());
            return ResponseEntity.ok(
                    ApiResponse.error("Failed to send report: "
                            + e.getMessage()));
        }
    }
}