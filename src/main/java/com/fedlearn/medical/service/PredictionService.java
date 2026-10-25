package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.PredictionRequest;
import com.fedlearn.medical.dto.response.PredictionResponse;
import com.fedlearn.medical.dto.response.RiskTimelineResponse;

import java.util.List;
import java.util.Map;

public interface PredictionService {

    Map<String, Object> makePrediction(PredictionRequest request);

    PredictionResponse getPredictionById(Long id);

    List<PredictionResponse> getPredictionsByPatient(Long patientId);

    List<PredictionResponse> getPredictionsByHospital(Long hospitalId);

    Map<String, Object> getPredictionAnalytics();

    byte[] generatePDFReport(Long patientId, Map<String, Object> requestBody);

    RiskTimelineResponse getPatientRiskTimeline(Long patientId);
}