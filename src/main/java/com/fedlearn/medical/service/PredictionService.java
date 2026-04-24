package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.PredictionRequest;
import com.fedlearn.medical.dto.response.PredictionResponse;
import com.fedlearn.medical.entity.*;
import com.fedlearn.medical.exception.MLServiceException;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private static final Logger logger =
            LoggerFactory.getLogger(PredictionService.class);

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ModelVersionRepository modelVersionRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> makePrediction(PredictionRequest request) {

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient", "id", request.getPatientId()));

        ModelVersion modelVersion =
                modelVersionRepository.findByIsActiveTrue().orElse(null);


        Map<String, Object> mlResult;
        boolean usedFallback = false;

        try {
            mlResult = mlServiceClient.predictAllDiseases(request.getFeatures());
            logger.info("ML Service prediction successful for patient: {}",
                    request.getPatientId());
        } catch (MLServiceException e) {
            logger.warn("ML service unavailable, using fallback: {}",
                    e.getMessage());
            mlResult = fallbackPrediction(request.getFeatures());
            usedFallback = true;
        }

        List<Map<String, Object>> predictions =
                (List<Map<String, Object>>) mlResult.get("predictions");

        List<Map<String, Object>> savedPredictions = new ArrayList<>();

        if (predictions != null && !predictions.isEmpty()) {
            for (Map<String, Object> pred : predictions) {
                try {

                    Map<String, Object> featuresMap = buildFeaturesMap(
                            request.getFeatures());


                    Prediction prediction = Prediction.builder()
                            .patient(patient)
                            .hospital(patient.getHospital())
                            .modelVersion(modelVersion)
                            .prediction(((Number) pred.get("prediction")).intValue())
                            .predictionLabel((String) pred.get("prediction_label"))
                            .confidence(((Number) pred.get("confidence")).doubleValue())
                            .featuresUsed(featuresMap)
                            .build();

                    prediction = predictionRepository.save(prediction);


                    Map<String, Object> savedPred = new HashMap<>(pred);
                    savedPred.put("id", prediction.getId());
                    savedPredictions.add(savedPred);

                } catch (Exception e) {
                    logger.error("Error saving prediction: {}", e.getMessage());
                }
            }
        }


        Map<String, Object> response = new HashMap<>();
        response.put("patientId", patient.getId());
        response.put("patientCode", patient.getPatientCode());
        response.put("hospitalId", patient.getHospital().getId());
        response.put("hospitalName", patient.getHospital().getName());
        response.put("predictions", savedPredictions.isEmpty()
                ? predictions : savedPredictions);
        response.put("totalDiseasesChecked",
                mlResult.get("total_diseases_checked"));
        response.put("modelVersion",
                modelVersion != null
                        ? modelVersion.getVersionName()
                        : (usedFallback ? "fallback" : "v1.0"));
        response.put("usedFallback", usedFallback);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        // Pass recommendations from ML service
        if (mlResult.containsKey("recommendations")) {
            response.put("recommendations", mlResult.get("recommendations"));
        }

        return response;
    }

    public PredictionResponse getPredictionById(Long id) {
        Prediction prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prediction", "id", id));
        return mapToResponse(prediction);
    }

    public List<PredictionResponse> getPredictionsByPatient(Long patientId) {
        return predictionRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PredictionResponse> getPredictionsByHospital(Long hospitalId) {
        return predictionRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getPredictionAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        long total = predictionRepository.count();
        analytics.put("totalPredictions", total);

        List<Object[]> distribution =
                predictionRepository.countByPredictionGrouped();
        Map<String, Long> dist = new HashMap<>();
        for (Object[] row : distribution) {
            String label = ((Integer) row[0]) == 1
                    ? "Disease Detected" : "Healthy";
            dist.put(label, (Long) row[1]);
        }
        analytics.put("predictionDistribution", dist);

        List<PredictionResponse> recent =
                predictionRepository.findLatest10().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
        analytics.put("recentPredictions", recent);

        return analytics;
    }

    private Map<String, Object> fallbackPrediction(List<Double> features) {
        List<Map<String, Object>> predictions = new ArrayList<>();

        double bp = features.size() > 2 ? features.get(2) : 120;
        double cholesterol = features.size() > 3 ? features.get(3) : 200;
        double glucose = features.size() > 5 ? features.get(5) : 100;
        double bmi = features.size() > 6 ? features.get(6) : 25;
        double smoking = features.size() > 7 ? features.get(7) : 0;
        double diabetes = features.size() > 8 ? features.get(8) : 0;


        double heartScore = 0;
        if (bp > 140) heartScore += 0.2;
        if (cholesterol > 240) heartScore += 0.2;
        if (smoking == 1) heartScore += 0.2;
        if (diabetes == 1) heartScore += 0.15;
        if (bmi > 30) heartScore += 0.1;

        predictions.add(createFallbackPrediction(
                "heart_disease", "Heart Disease", heartScore));


        double diabetesScore = 0;
        if (glucose > 140) diabetesScore += 0.4;
        if (bmi > 30) diabetesScore += 0.2;
        if (diabetes == 1) diabetesScore += 0.3;

        predictions.add(createFallbackPrediction(
                "diabetes", "Diabetes", diabetesScore));


        predictions.sort((a, b) -> Double.compare(
                (Double) b.get("confidence"),
                (Double) a.get("confidence")));

        Map<String, Object> result = new HashMap<>();
        result.put("predictions", predictions);
        result.put("total_diseases_checked", predictions.size());
        result.put("model_version", "fallback_v1.0");

        return result;
    }

    private Map<String, Object> createFallbackPrediction(
            String diseaseType, String diseaseName, double score) {
        int prediction = score >= 0.5 ? 1 : 0;
        double confidence = Math.min(0.95, Math.max(0.05, score));

        Map<String, Object> pred = new HashMap<>();
        pred.put("disease_type", diseaseType);
        pred.put("disease_name", diseaseName);
        pred.put("prediction", prediction);
        pred.put("prediction_label",
                prediction == 1 ? diseaseName + " Detected" : "Healthy");
        pred.put("confidence", confidence);
        pred.put("risk_level", confidence >= 0.75 ? "HIGH"
                : confidence >= 0.5 ? "MODERATE"
                  : confidence >= 0.25 ? "LOW" : "MINIMAL");
        pred.put("probabilities",
                List.of(1 - confidence, confidence));

        return pred;
    }

    private Map<String, Object> buildFeaturesMap(List<Double> features) {
        String[] featureNames = {
                "age", "gender", "blood_pressure", "cholesterol",
                "heart_rate", "glucose", "bmi", "smoking",
                "diabetes_history", "family_history", "creatinine",
                "hemoglobin", "bilirubin", "alt_enzyme",
                "albumin", "potassium", "sodium", "urea"
        };

        Map<String, Object> featuresMap = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(features.size(), featureNames.length); i++) {
            featuresMap.put(featureNames[i], features.get(i));
        }
        return featuresMap;
    }

    private PredictionResponse mapToResponse(Prediction prediction) {
        return PredictionResponse.builder()
                .id(prediction.getId())
                .patientId(prediction.getPatient().getId())
                .patientCode(prediction.getPatient().getPatientCode())
                .hospitalId(prediction.getHospital().getId())
                .hospitalName(prediction.getHospital().getName())
                .prediction(prediction.getPrediction())
                .predictionLabel(prediction.getPredictionLabel())
                .confidence(prediction.getConfidence())
                .modelVersion(prediction.getModelVersion() != null
                        ? prediction.getModelVersion().getVersionName()
                        : "fallback")
                .featuresUsed(prediction.getFeaturesUsed())
                .createdAt(prediction.getCreatedAt())
                .build();
    }
    @SuppressWarnings("unchecked")
    public byte[] generatePDFReport(Long patientId, Map<String, Object> requestBody) {
        // Get patient data
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));

        // Build request for Python ML service
        String url = "http://localhost:8000/ml/report/generate";

        Map<String, Object> patientData = new java.util.LinkedHashMap<>();
        patientData.put("patientCode", patient.getPatientCode());
        patientData.put("age", patient.getAge());
        patientData.put("gender", patient.getGender() != null
                ? patient.getGender().name() : "Unknown");
        patientData.put("hospitalName", patient.getHospital().getName());
        patientData.put("diagnosis", patient.getDiagnosis());

        Map<String, Object> hospitalData = new java.util.LinkedHashMap<>();
        hospitalData.put("name", patient.getHospital().getName());
        hospitalData.put("code", patient.getHospital().getCode());

        Map<String, Object> payload = new java.util.HashMap<>(requestBody);
        payload.put("patient_data", patientData);
        payload.put("hospital_data", hospitalData);

        org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(payload, headers);

        org.springframework.http.ResponseEntity<byte[]> response =
                new org.springframework.web.client.RestTemplate().exchange(
                        url,
                        org.springframework.http.HttpMethod.POST,
                        entity,
                        byte[].class
                );

        return response.getBody();
    }

    public com.fedlearn.medical.dto.response.RiskTimelineResponse
    getPatientRiskTimeline(Long patientId) {

        logger.info("Getting risk timeline for patient: {}", patientId);


        com.fedlearn.medical.entity.Patient patient =
                patientRepository.findById(patientId)
                        .orElseThrow(() -> new com.fedlearn.medical.exception
                                .ResourceNotFoundException("Patient", "id", patientId));


        List<com.fedlearn.medical.entity.Prediction> predictions =
                predictionRepository.findByPatientIdOrderByCreatedAtAsc(patientId);

        if (predictions.isEmpty()) {
            return com.fedlearn.medical.dto.response.RiskTimelineResponse.builder()
                    .patientId(patientId)
                    .patientCode(patient.getPatientCode())
                    .hospitalName(patient.getHospital().getName())
                    .summary(new java.util.ArrayList<>())
                    .detailed(new java.util.ArrayList<>())
                    .totalPredictions(0)
                    .currentRiskScore(0.0)
                    .riskTrend("STABLE")
                    .build();
        }


        java.util.Map<String, List<com.fedlearn.medical.entity.Prediction>>
                groupedByDate = predictions.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> {
                    return p.getCreatedAt().toLocalDate().toString();
                }));

        List<com.fedlearn.medical.dto.response.RiskTimelineResponse.TimelinePoint>
                summary = new java.util.ArrayList<>();

        List<com.fedlearn.medical.dto.response.RiskTimelineResponse.DetailedTimelinePoint>
                detailed = new java.util.ArrayList<>();

        List<String> sortedDates = new java.util.ArrayList<>(groupedByDate.keySet());
        java.util.Collections.sort(sortedDates);

        for (String date : sortedDates) {
            List<com.fedlearn.medical.entity.Prediction> dayPreds =
                    groupedByDate.get(date);

            double avgRisk = dayPreds.stream()
                    .filter(p -> p.getConfidence() != null)
                    .mapToDouble(com.fedlearn.medical.entity.Prediction::getConfidence)
                    .average()
                    .orElse(0.0);

            long diseasesDetected = dayPreds.stream()
                    .filter(p -> p.getPrediction() != null && p.getPrediction() == 1)
                    .count();

            String riskLevel = getRiskLevelFromConfidence(avgRisk);

            java.time.LocalDateTime timestamp = dayPreds.get(0).getCreatedAt();

            summary.add(
                    com.fedlearn.medical.dto.response.RiskTimelineResponse
                            .TimelinePoint.builder()
                            .date(date)
                            .timestamp(timestamp)
                            .avgRisk(Math.round(avgRisk * 10000.0) / 10000.0)
                            .riskLevel(riskLevel)
                            .diseasesDetected((int) diseasesDetected)
                            .totalDiseases(dayPreds.size())
                            .build()
            );

            java.util.Map<String, Double> diseaseConfidences =
                    new java.util.LinkedHashMap<>();
            java.util.Map<String, Integer> diseasePredictions =
                    new java.util.LinkedHashMap<>();

            for (com.fedlearn.medical.entity.Prediction pred : dayPreds) {
                String diseaseKey = extractDiseaseKey(pred.getPredictionLabel());
                if (pred.getConfidence() != null) {
                    diseaseConfidences.put(diseaseKey,
                            Math.round(pred.getConfidence() * 10000.0) / 10000.0);
                }
                if (pred.getPrediction() != null) {
                    diseasePredictions.put(diseaseKey, pred.getPrediction());
                }
            }

            detailed.add(
                    com.fedlearn.medical.dto.response.RiskTimelineResponse
                            .DetailedTimelinePoint.builder()
                            .date(date)
                            .timestamp(timestamp)
                            .diseaseConfidences(diseaseConfidences)
                            .diseasePredictions(diseasePredictions)
                            .avgRisk(Math.round(avgRisk * 10000.0) / 10000.0)
                            .build()
            );
        }


        String trend = "STABLE";
        if (summary.size() >= 2) {
            double firstRisk = summary.get(0).getAvgRisk();
            double lastRisk = summary.get(summary.size() - 1).getAvgRisk();
            double diff = lastRisk - firstRisk;
            if (diff > 0.05) trend = "WORSENING";
            else if (diff < -0.05) trend = "IMPROVING";
        }

        double currentRisk = summary.isEmpty() ? 0.0
                : summary.get(summary.size() - 1).getAvgRisk();

        return com.fedlearn.medical.dto.response.RiskTimelineResponse.builder()
                .patientId(patientId)
                .patientCode(patient.getPatientCode())
                .hospitalName(patient.getHospital().getName())
                .summary(summary)
                .detailed(detailed)
                .totalPredictions(predictions.size())
                .currentRiskScore(currentRisk)
                .riskTrend(trend)
                .firstPrediction(predictions.get(0).getCreatedAt())
                .lastPrediction(predictions.get(predictions.size() - 1).getCreatedAt())
                .build();
    }

    private String getRiskLevelFromConfidence(double confidence) {
        if (confidence >= 0.75) return "HIGH";
        if (confidence >= 0.50) return "MODERATE";
        if (confidence >= 0.25) return "LOW";
        return "MINIMAL";
    }

    private String extractDiseaseKey(String predictionLabel) {
        if (predictionLabel == null) return "unknown";
        String lower = predictionLabel.toLowerCase();
        if (lower.contains("heart")) return "heart_disease";
        if (lower.contains("diabet")) return "diabetes";
        if (lower.contains("kidney")) return "kidney_disease";
        if (lower.contains("liver")) return "liver_disease";
        if (lower.contains("stroke")) return "stroke";
        return "unknown";
    }
}