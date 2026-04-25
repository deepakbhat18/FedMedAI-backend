package com.fedlearn.medical.controller;
import com.fedlearn.medical.dto.request.PredictionRequest;
import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.dto.response.PredictionResponse;
import com.fedlearn.medical.dto.response.RiskTimelineResponse;
import com.fedlearn.medical.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
@Tag(name = "Predictions",
        description = "Multi-Disease Prediction APIs using Federated Learning")
public class PredictionController {

    private static final Logger logger =
            LoggerFactory.getLogger(PredictionController.class);

    @Autowired
    private PredictionService predictionService;

    @PostMapping
    @Operation(
            summary = "Predict ALL diseases",
            description = "Submit patient features and get predictions for all 5 diseases"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> makePrediction(
            @Valid @RequestBody PredictionRequest request) {
        Map<String, Object> response =
                predictionService.makePrediction(request);
        return ResponseEntity.ok(
                ApiResponse.success("Multi-disease prediction completed", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prediction by ID")
    public ResponseEntity<ApiResponse<PredictionResponse>> getPredictionById(
            @PathVariable Long id) {
        PredictionResponse prediction =
                predictionService.getPredictionById(id);
        return ResponseEntity.ok(ApiResponse.success(prediction));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get predictions by patient")
    public ResponseEntity<ApiResponse<List<PredictionResponse>>> getPredictionsByPatient(
            @PathVariable Long patientId) {
        List<PredictionResponse> predictions =
                predictionService.getPredictionsByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.success(predictions));
    }

    @GetMapping("/hospital/{hospitalId}")
    @Operation(summary = "Get predictions by hospital")
    public ResponseEntity<ApiResponse<List<PredictionResponse>>> getPredictionsByHospital(
            @PathVariable Long hospitalId) {
        List<PredictionResponse> predictions =
                predictionService.getPredictionsByHospital(hospitalId);
        return ResponseEntity.ok(ApiResponse.success(predictions));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Prediction analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPredictionAnalytics() {
        Map<String, Object> analytics =
                predictionService.getPredictionAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @PostMapping("/report/{patientId}")
    @Operation(
            summary = "Generate PDF medical report",
            description = "Generate and download professional PDF diagnosis report"
    )
    public ResponseEntity<byte[]> generateReport(
            @PathVariable Long patientId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            logger.info("PDF report requested for patient: {}", patientId);

            byte[] pdfBytes =
                    predictionService.generatePDFReport(patientId, requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                    "attachment",
                    "medical_report_" + patientId + ".pdf");
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl("no-cache");

            logger.info("PDF generated: {} bytes for patient: {}",
                    pdfBytes.length, patientId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("PDF generation error for patient {}: {}",
                    patientId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }


    @GetMapping("/patient/{patientId}/timeline")
    @Operation(
            summary = "Get patient risk timeline",
            description = "Get historical risk data for a patient " +
                    "(summary + per-disease detailed view)"
    )
    public ResponseEntity<ApiResponse<RiskTimelineResponse>> getPatientTimeline(
            @PathVariable Long patientId) {
        RiskTimelineResponse timeline =
                predictionService.getPatientRiskTimeline(patientId);
        return ResponseEntity.ok(
                ApiResponse.success("Risk timeline retrieved", timeline));
    }
}