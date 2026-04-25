
package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.request.PatientRequest;
import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.dto.response.PatientResponse;
import com.fedlearn.medical.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patient Management", description = "APIs for managing patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @PostMapping
    @Operation(summary = "Add new patient", description = "Add a patient to a hospital")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody PatientRequest request) {
        PatientResponse response = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient added successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all patients", description = "Get all patients")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {
        List<PatientResponse> patients = patientService.getAllPatients();
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID", description = "Get patient details by ID")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable Long id) {
        PatientResponse patient = patientService.getPatientById(id);
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @GetMapping("/hospital/{hospitalId}")
    @Operation(summary = "Get patients by hospital",
            description = "Get all patients of a specific hospital")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getPatientsByHospital(
            @PathVariable Long hospitalId) {
        List<PatientResponse> patients = patientService.getPatientsByHospital(hospitalId);
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient", description = "Update patient details")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {
        PatientResponse response = patientService.updatePatient(id, request);
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient", description = "Delete patient by ID")
    public ResponseEntity<ApiResponse<Void>> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }

}