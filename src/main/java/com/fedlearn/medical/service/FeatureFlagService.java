package com.fedlearn.medical.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeatureFlagService {

    private final Map<String, Boolean> featureFlags = new ConcurrentHashMap<>();

    public FeatureFlagService(
            @Value("${app.features.fallback-prediction:true}") boolean fallbackEnabled,
            @Value("${app.features.flower-engine:false}") boolean flowerEngineEnabled,
            @Value("${app.features.pdf-report-generation:true}") boolean pdfReportEnabled) {

        featureFlags.put("fallback.prediction.enabled", fallbackEnabled);
        featureFlags.put("ml.engine.flower.enabled", flowerEngineEnabled);
        featureFlags.put("pdf.report.enabled", pdfReportEnabled);
    }

    public boolean isFeatureEnabled(String featureName) {
        return featureFlags.getOrDefault(featureName, false);
    }

    public void setFeatureFlag(String featureName, boolean enabled) {
        featureFlags.put(featureName, enabled);
    }

    public Map<String, Boolean> getAllFeatureFlags() {
        return Map.copyOf(featureFlags);
    }
}
