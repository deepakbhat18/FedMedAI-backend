package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.ml.MlDiseasePredictionDto;
import com.fedlearn.medical.dto.ml.MlPredictResponse;
import com.fedlearn.medical.dto.request.PredictionRequest;
import com.fedlearn.medical.dto.response.PredictionResponse;
import com.fedlearn.medical.dto.response.RiskTimelineResponse;
import com.fedlearn.medical.entity.ModelVersion;
import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.entity.Prediction;
import com.fedlearn.medical.exception.MLServiceException;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.ModelVersionRepository;
import com.fedlearn.medical.repository.PatientRepository;
import com.fedlearn.medical.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PredictionServiceImpl implements PredictionService {

    private static final Logger logger = LoggerFactory.getLogger(PredictionServiceImpl.class);

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ModelVersionRepository modelVersionRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Autowired
    private FallbackPredictionEngine fallbackPredictionEngine;

    @Autowired
    private PdfReportGeneratorService pdfReportGeneratorService;

    @Autowired
    private RiskTimelineAnalyticsService riskTimelineAnalyticsService;

    @Autowired
    private FeatureFlagService featureFlagService;

    @Override
    @Transactional
    public Map<String, Object> makePrediction(PredictionRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.getPatientId()));

        ModelVersion modelVersion = modelVersionRepository.findByIsActiveTrue().orElse(null);

        MlPredictResponse mlResponse;
        boolean usedFallback = false;

        try {
            mlResponse = mlServiceClient.predictAllDiseasesTyped(request.getFeatures());
        } catch (MLServiceException e) {
            if (featureFlagService.isFeatureEnabled("fallback.prediction.enabled")) {
                logger.warn("ML Service unavailable, initiating fallback rule engine prediction logic: {}", e.getMessage());
                mlResponse = fallbackPredictionEngine.calculateFallback(request.getFeatures());
                usedFallback = true;
            } else {
                throw e;
            }
        }

        List<MlDiseasePredictionDto> predictions = mlResponse.getPredictions();
        List<Map<String, Object>> savedPredictions = new ArrayList<>();

        if (predictions != null && !predictions.isEmpty()) {
            for (MlDiseasePredictionDto pred : predictions) {
                try {
                    Map<String, Object> featuresMap = buildFeaturesMap(request.getFeatures());

                    Prediction prediction = Prediction.builder()
                            .patient(patient)
                            .hospital(patient.getHospital())
                            .modelVersion(modelVersion)
                            .prediction(pred.getPrediction())
                            .predictionLabel(pred.getPredictionLabel())
                            .confidence(pred.getConfidence())
                            .featuresUsed(featuresMap)
                            .build();

                    prediction = predictionRepository.save(prediction);

                    Map<String, Object> savedPred = new HashMap<>();
                    savedPred.put("id", prediction.getId());
                    savedPred.put("disease_type", pred.getDiseaseType());
                    savedPred.put("disease_name", pred.getDiseaseName());
                    savedPred.put("prediction", pred.getPrediction());
                    savedPred.put("prediction_label", pred.getPredictionLabel());
                    savedPred.put("confidence", pred.getConfidence());
                    savedPred.put("risk_level", pred.getRiskLevel());
                    savedPred.put("probabilities", pred.getProbabilities());

                    savedPredictions.add(savedPred);
                } catch (Exception e) {
                    logger.error("Error persisting prediction record to database: {}", e.getMessage());
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("patientId", patient.getId());
        response.put("patientCode", patient.getPatientCode());
        response.put("hospitalId", patient.getHospital().getId());
        response.put("hospitalName", patient.getHospital().getName());
        response.put("predictions", savedPredictions);
        response.put("totalDiseasesChecked", mlResponse.getTotalDiseasesChecked());
        response.put("modelVersion", modelVersion != null ? modelVersion.getVersionName() : (usedFallback ? "fallback" : "v1.0"));
        response.put("usedFallback", usedFallback);
        response.put("timestamp", LocalDateTime.now().toString());

        if (mlResponse.getRecommendations() != null) {
            response.put("recommendations", mlResponse.getRecommendations());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PredictionResponse getPredictionById(Long id) {
        Prediction prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prediction", "id", id));
        return mapToResponse(prediction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PredictionResponse> getPredictionsByPatient(Long patientId) {
        return predictionRepository.findByPatientId(patientId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PredictionResponse> getPredictionsByHospital(Long hospitalId) {
        return predictionRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPredictionAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        long total = predictionRepository.count();
        analytics.put("totalPredictions", total);

        List<Object[]> distribution = predictionRepository.countByPredictionGrouped();
        Map<String, Long> dist = new HashMap<>();
        if (distribution != null) {
            for (Object[] row : distribution) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    int predVal = ((Number) row[0]).intValue();
                    String label = (predVal == 1) ? "Disease Detected" : "Healthy";
                    long countVal = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                    dist.put(label, countVal);
                }
            }
        }
        analytics.put("predictionDistribution", dist);

        List<PredictionResponse> recent = predictionRepository.findLatest10().stream()
                .map(this::mapToResponse)
                .toList();
        analytics.put("recentPredictions", recent);

        return analytics;
    }

    @Override
    public byte[] generatePDFReport(Long patientId, Map<String, Object> requestBody) {
        return pdfReportGeneratorService.generatePDFReport(patientId, requestBody);
    }

    @Override
    public RiskTimelineResponse getPatientRiskTimeline(Long patientId) {
        return riskTimelineAnalyticsService.getPatientRiskTimeline(patientId);
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
                .modelVersion(prediction.getModelVersion() != null ? prediction.getModelVersion().getVersionName() : "fallback")
                .featuresUsed(prediction.getFeaturesUsed())
                .createdAt(prediction.getCreatedAt())
                .build();
    }
}
