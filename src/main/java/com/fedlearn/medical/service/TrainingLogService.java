package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.response.TrainingLogResponse;
import com.fedlearn.medical.entity.TrainingLog;
import com.fedlearn.medical.repository.TrainingLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainingLogService {

    @Autowired
    private TrainingLogRepository trainingLogRepository;

    public List<TrainingLogResponse> getAllLogs() {
        return trainingLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TrainingLogResponse> getLogsByRound(Integer round) {
        return trainingLogRepository.findByTrainingRound(round).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TrainingLogResponse> getLogsByHospital(Long hospitalId) {
        return trainingLogRepository.findByHospitalIdOrderByRoundDesc(hospitalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getTrainingAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        List<Integer> rounds = trainingLogRepository.findDistinctTrainingRounds();
        analytics.put("totalRounds", rounds.size());
        analytics.put("rounds", rounds);

        List<Map<String, Object>> accuracyHistory = new ArrayList<>();
        for (Integer round : rounds) {
            List<TrainingLog> logs = trainingLogRepository.findByTrainingRound(round);
            double avgAccuracy = logs.stream()
                    .filter(l -> l.getLocalAccuracy() != null)
                    .mapToDouble(TrainingLog::getLocalAccuracy)
                    .average().orElse(0.0);

            Map<String, Object> entry = new HashMap<>();
            entry.put("round", round);
            entry.put("avgAccuracy", Math.round(avgAccuracy * 10000.0) / 10000.0);
            entry.put("hospitals", logs.size());
            accuracyHistory.add(entry);
        }
        analytics.put("accuracyHistory", accuracyHistory);

        return analytics;
    }

    private TrainingLogResponse mapToResponse(TrainingLog log) {
        return TrainingLogResponse.builder()
                .id(log.getId())
                .trainingRound(log.getTrainingRound())
                .hospitalId(log.getHospital() != null ? log.getHospital().getId() : null)
                .hospitalName(log.getHospital() != null ? log.getHospital().getName() : null)
                .modelVersionId(log.getModelVersion() != null ? log.getModelVersion().getId() : null)
                .modelVersionName(log.getModelVersion() != null
                        ? log.getModelVersion().getVersionName() : null)
                .localAccuracy(log.getLocalAccuracy())
                .localLoss(log.getLocalLoss())
                .localPrecision(log.getLocalPrecision())
                .localRecall(log.getLocalRecall())
                .localF1Score(log.getLocalF1Score())
                .samplesUsed(log.getSamplesUsed())
                .epochs(log.getEpochs())
                .learningRate(log.getLearningRate())
                .batchSize(log.getBatchSize())
                .trainingDurationSeconds(log.getTrainingDurationSeconds())
                .status(log.getStatus().name())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}