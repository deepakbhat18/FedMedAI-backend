
package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.response.AnalyticsResponse;
import com.fedlearn.medical.entity.ModelVersion;
import com.fedlearn.medical.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private ModelVersionRepository modelVersionRepository;

    @Autowired
    private TrainingLogRepository trainingLogRepository;

    public AnalyticsResponse getDashboardAnalytics() {

        long totalHospitals = hospitalRepository.count();
        long totalPatients = patientRepository.count();
        long totalPredictions = predictionRepository.count();

        List<Integer> rounds = trainingLogRepository.findDistinctTrainingRounds();
        long totalRounds = rounds.size();

        String latestVersion = "None";
        Double latestAccuracy = null;
        Optional<ModelVersion> activeModel = modelVersionRepository.findByIsActiveTrue();
        if (activeModel.isPresent()) {
            latestVersion = activeModel.get().getVersionName();
            latestAccuracy = activeModel.get().getAccuracy();
        }

        List<Map<String, Object>> hospitalStats = new ArrayList<>();
        hospitalRepository.findAll().forEach(hospital -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("hospitalId", hospital.getId());
            stat.put("hospitalName", hospital.getName());
            stat.put("patientCount", hospitalRepository.countPatientsByHospitalId(hospital.getId()));
            stat.put("predictionCount", predictionRepository.countByHospitalId(hospital.getId()));
            hospitalStats.add(stat);
        });

        List<Map<String, Object>> trainingHistory = new ArrayList<>();
        modelVersionRepository.findAllByOrderByTrainingRoundDesc().forEach(model -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("round", model.getTrainingRound());
            entry.put("version", model.getVersionName());
            entry.put("accuracy", model.getAccuracy());
            entry.put("totalSamples", model.getTotalSamples());
            entry.put("createdAt", model.getCreatedAt());
            trainingHistory.add(entry);
        });

        Map<String, Long> predictionDist = new HashMap<>();
        predictionRepository.countByPredictionGrouped().forEach(row -> {
            String label = ((Integer) row[0]) == 1 ? "Disease Detected" : "Healthy";
            predictionDist.put(label, (Long) row[1]);
        });

        return AnalyticsResponse.builder()
                .totalHospitals(totalHospitals)
                .totalPatients(totalPatients)
                .totalPredictions(totalPredictions)
                .totalTrainingRounds(totalRounds)
                .latestModelVersion(latestVersion)
                .latestModelAccuracy(latestAccuracy)
                .hospitalStats(hospitalStats)
                .trainingHistory(trainingHistory)
                .predictionDistribution(predictionDist)
                .build();
    }
}