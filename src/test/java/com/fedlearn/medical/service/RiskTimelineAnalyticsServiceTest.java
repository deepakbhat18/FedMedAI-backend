package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.response.RiskTimelineResponse;
import com.fedlearn.medical.entity.Hospital;
import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.entity.Prediction;
import com.fedlearn.medical.enums.Gender;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.PatientRepository;
import com.fedlearn.medical.repository.PredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskTimelineAnalyticsServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @InjectMocks
    private RiskTimelineAnalyticsService analyticsService;

    private Patient testPatient;
    private Prediction testPrediction1;
    private Prediction testPrediction2;

    @BeforeEach
    void setUp() {
        Hospital testHospital = Hospital.builder().id(1L).name("City Hospital").code("HOSP-01").build();

        testPatient = Patient.builder()
                .id(50L)
                .hospital(testHospital)
                .patientCode("PAT-50")
                .age(60)
                .gender(Gender.FEMALE)
                .build();

        testPrediction1 = Prediction.builder()
                .id(1L)
                .patient(testPatient)
                .hospital(testHospital)
                .prediction(1)
                .predictionLabel("High Diabetes Risk")
                .confidence(0.85)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        testPrediction2 = Prediction.builder()
                .id(2L)
                .patient(testPatient)
                .hospital(testHospital)
                .prediction(0)
                .predictionLabel("Low Risk")
                .confidence(0.92)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Test
    @DisplayName("Should generate structured risk timeline for patient with multiple historical predictions")
    void getPatientRiskTimeline_Success() {
        when(patientRepository.findById(50L)).thenReturn(Optional.of(testPatient));
        when(predictionRepository.findByPatientIdOrderByCreatedAtAsc(50L))
                .thenReturn(List.of(testPrediction1, testPrediction2));

        RiskTimelineResponse timeline = analyticsService.getPatientRiskTimeline(50L);

        assertNotNull(timeline);
        assertEquals(50L, timeline.getPatientId());
        assertEquals("PAT-50", timeline.getPatientCode());
        assertEquals(2, timeline.getTotalPredictions());
        assertNotNull(timeline.getSummary());
        assertFalse(timeline.getSummary().isEmpty());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent patient ID")
    void getPatientRiskTimeline_PatientNotFound_ThrowsException() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> analyticsService.getPatientRiskTimeline(999L));
    }
}
