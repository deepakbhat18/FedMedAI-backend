package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.ml.MlDiseasePredictionDto;
import com.fedlearn.medical.dto.ml.MlPredictResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FallbackPredictionEngine {

    public MlPredictResponse calculateFallback(List<Double> features) {
        List<MlDiseasePredictionDto> predictions = new ArrayList<>();

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

        predictions.add(buildFallbackDto("heart_disease", "Heart Disease", heartScore));

        double diabetesScore = 0;
        if (glucose > 140) diabetesScore += 0.4;
        if (bmi > 30) diabetesScore += 0.2;
        if (diabetes == 1) diabetesScore += 0.3;

        predictions.add(buildFallbackDto("diabetes", "Diabetes", diabetesScore));

        predictions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        return MlPredictResponse.builder()
                .predictions(predictions)
                .modelVersion("fallback_v1.0")
                .totalDiseasesChecked(predictions.size())
                .build();
    }

    private MlDiseasePredictionDto buildFallbackDto(String diseaseType, String diseaseName, double score) {
        int prediction = score >= 0.5 ? 1 : 0;
        double confidence = Math.min(0.95, Math.max(0.05, score));

        return MlDiseasePredictionDto.builder()
                .diseaseType(diseaseType)
                .diseaseName(diseaseName)
                .prediction(prediction)
                .predictionLabel(prediction == 1 ? diseaseName + " Detected" : "Healthy")
                .confidence(Math.round(confidence * 10000.0) / 10000.0)
                .riskLevel(confidence >= 0.75 ? "HIGH" : confidence >= 0.5 ? "MODERATE" : confidence >= 0.25 ? "LOW" : "MINIMAL")
                .probabilities(List.of(Math.round((1 - confidence) * 10000.0) / 10000.0, Math.round(confidence * 10000.0) / 10000.0))
                .build();
    }
}
