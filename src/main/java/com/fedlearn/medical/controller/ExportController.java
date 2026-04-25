package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
@Tag(name = "CSV Export",
        description = "APIs for exporting data to CSV format")
public class ExportController {

    private static final Logger logger =
            LoggerFactory.getLogger(ExportController.class);

    @Autowired
    private ExportService exportService;

    @GetMapping("/patients")
    @Operation(
            summary = "Export patients to CSV",
            description = "Download all patients or filter by hospital as CSV file"
    )
    public ResponseEntity<byte[]> exportPatients(
            @RequestParam(required = false) Long hospitalId) {
        try {
            logger.info("CSV export requested: patients, hospitalId={}", hospitalId);

            byte[] csvBytes = exportService.exportPatientsCsv(hospitalId);

            String filename = "patients_export_"
                    + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(csvBytes.length);
            headers.set("X-Record-Count",
                    String.valueOf(csvBytes.length));

            logger.info("Exporting patients CSV: {} bytes", csvBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);

        } catch (Exception e) {
            logger.error("Patients CSV export failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/predictions")
    @Operation(
            summary = "Export predictions to CSV",
            description = "Download all predictions or filter by hospital as CSV file"
    )
    public ResponseEntity<byte[]> exportPredictions(
            @RequestParam(required = false) Long hospitalId) {
        try {
            logger.info("CSV export requested: predictions, hospitalId={}", hospitalId);

            byte[] csvBytes = exportService.exportPredictionsCsv(hospitalId);

            String filename = "predictions_export_"
                    + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(csvBytes.length);

            logger.info("Exporting predictions CSV: {} bytes", csvBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);

        } catch (Exception e) {
            logger.error("Predictions CSV export failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get export statistics",
            description = "Get counts before downloading CSV"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExportStats(
            @RequestParam(required = false) Long hospitalId) {
        Map<String, Object> stats = exportService.getExportStats(hospitalId);
        return ResponseEntity.ok(ApiResponse.success(
                "Export statistics retrieved", stats));
    }
}