package com.fedlearn.medical.service;

import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.exception.MLServiceException;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PdfReportGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(PdfReportGeneratorService.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.ml-service.base-url:http://localhost:8000}")
    private String mlServiceBaseUrl;

    public byte[] generatePDFReport(Long patientId, Map<String, Object> requestBody) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));

        String url = mlServiceBaseUrl + "/ml/report/generate";
        logger.info("Calling PDF generation service for patient {}: {}", patientId, url);

        Map<String, Object> patientData = new LinkedHashMap<>();
        patientData.put("patientCode", patient.getPatientCode());
        patientData.put("age", patient.getAge());
        patientData.put("gender", patient.getGender() != null ? patient.getGender().name() : "Unknown");
        patientData.put("hospitalName", patient.getHospital().getName());
        patientData.put("diagnosis", patient.getDiagnosis());

        Map<String, Object> hospitalData = new LinkedHashMap<>();
        hospitalData.put("name", patient.getHospital().getName());
        hospitalData.put("code", patient.getHospital().getCode());

        Map<String, Object> payload = new LinkedHashMap<>(requestBody);
        payload.put("patient_data", patientData);
        payload.put("hospital_data", hospitalData);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new MLServiceException("PDF service returned empty response");
        } catch (Exception e) {
            logger.error("Failed to generate PDF report: {}", e.getMessage());
            throw new MLServiceException("PDF report generation failed: " + e.getMessage(), e);
        }
    }
}
