package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.response.RiskTimelineResponse;
import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.entity.Prediction;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.PatientRepository;
import com.fedlearn.medical.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RiskTimelineAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(RiskTimelineAnalyticsService.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Transactional(readOnly = true)
    public RiskTimelineResponse getPatientRiskTimeline(Long patientId) {
        logger.info("Computing risk timeline analytics for patient: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));

        List<Prediction> predictions = predictionRepository.findByPatientIdOrderByCreatedAtAsc(patientId);

        if (predictions.isEmpty()) {
            return RiskTimelineResponse.builder()
                    .patientId(patientId)
                    .patientCode(patient.getPatientCode())
                    .hospitalName(patient.getHospital().getName())
                    .summary(new ArrayList<>())
                    .detailed(new ArrayList<>())
                    .totalPredictions(0)
                    .currentRiskScore(0.0)
                    .riskTrend("STABLE")
                    .build();
        }

        Map<String, List<Prediction>> groupedByDate = predictions.stream()
                .collect(Collectors.groupingBy(p -> p.getCreatedAt().toLocalDate().toString()));

        List<RiskTimelineResponse.TimelinePoint> summary = new ArrayList<>();
        List<RiskTimelineResponse.DetailedTimelinePoint> detailed = new ArrayList<>();

        List<String> sortedDates = new ArrayList<>(groupedByDate.keySet());
        Collections.sort(sortedDates);

        for (String date : sortedDates) {
            List<Prediction> dayPreds = groupedByDate.get(date);

            double avgRisk = dayPreds.stream()
                    .filter(p -> p.getConfidence() != null)
                    .mapToDouble(Prediction::getConfidence)
                    .average()
                    .orElse(0.0);

            long diseasesDetected = dayPreds.stream()
                    .filter(p -> p.getPrediction() != null && p.getPrediction() == 1)
                    .count();

            LocalDateTime timestamp = dayPreds.get(0).getCreatedAt();

            summary.add(RiskTimelineResponse.TimelinePoint.builder()
                    .date(date)
                    .timestamp(timestamp)
                    .avgRisk(Math.round(avgRisk * 10000.0) / 10000.0)
                    .riskLevel(getRiskLevelFromConfidence(avgRisk))
                    .diseasesDetected((int) diseasesDetected)
                    .totalDiseases(dayPreds.size())
                    .build());

            Map<String, Double> diseaseConfidences = new LinkedHashMap<>();
            Map<String, Integer> diseasePredictions = new LinkedHashMap<>();

            for (Prediction pred : dayPreds) {
                String diseaseKey = extractDiseaseKey(pred.getPredictionLabel());
                if (pred.getConfidence() != null) {
                    diseaseConfidences.put(diseaseKey, Math.round(pred.getConfidence() * 10000.0) / 10000.0);
                }
                if (pred.getPrediction() != null) {
                    diseasePredictions.put(diseaseKey, pred.getPrediction());
                }
            }

            detailed.add(RiskTimelineResponse.DetailedTimelinePoint.builder()
                    .date(date)
                    .timestamp(timestamp)
                    .diseaseConfidences(diseaseConfidences)
                    .diseasePredictions(diseasePredictions)
                    .avgRisk(Math.round(avgRisk * 10000.0) / 10000.0)
                    .build());
        }

        String trend = "STABLE";
        if (summary.size() >= 2) {
            double firstRisk = summary.get(0).getAvgRisk();
            double lastRisk = summary.get(summary.size() - 1).getAvgRisk();
            double diff = lastRisk - firstRisk;
            if (diff > 0.05) trend = "WORSENING";
            else if (diff < -0.05) trend = "IMPROVING";
        }

        double currentRisk = summary.isEmpty() ? 0.0 : summary.get(summary.size() - 1).getAvgRisk();

        return RiskTimelineResponse.builder()
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
