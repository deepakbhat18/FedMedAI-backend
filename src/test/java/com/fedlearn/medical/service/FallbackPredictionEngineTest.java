package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.ml.MlPredictResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FallbackPredictionEngineTest {

    private FallbackPredictionEngine fallbackEngine;

    @BeforeEach
    void setUp() {
        fallbackEngine = new FallbackPredictionEngine();
    }

    @Test
    @DisplayName("Should calculate fallback ML predictions from raw clinical feature list")
    void calculateFallback_Success() {
        List<Double> features = List.of(
                55.0, 1.0, 140.0, 240.0, 80.0, 120.0, 29.5, 0.0, 0.0, 1.0, 1.1, 14.5, 0.8, 30.0, 4.2, 4.5, 140.0, 35.0
        );

        MlPredictResponse response = fallbackEngine.calculateFallback(features);

        assertNotNull(response);
        assertEquals("fallback_v1.0", response.getModelVersion());
        assertNotNull(response.getPredictions());
        assertFalse(response.getPredictions().isEmpty());
        assertEquals(2, response.getTotalDiseasesChecked());
    }

    @Test
    @DisplayName("Should safely handle empty feature array without crashing")
    void calculateFallback_EmptyFeatures_HandlesGracefully() {
        List<Double> emptyFeatures = List.of();

        MlPredictResponse response = fallbackEngine.calculateFallback(emptyFeatures);

        assertNotNull(response);
        assertEquals("fallback_v1.0", response.getModelVersion());
        assertNotNull(response.getPredictions());
    }
}
