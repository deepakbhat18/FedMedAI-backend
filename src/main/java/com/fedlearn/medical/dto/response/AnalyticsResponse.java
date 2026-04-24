package com.fedlearn.medical.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private Long totalHospitals;
    private Long totalPatients;
    private Long totalPredictions;
    private Long totalTrainingRounds;
    private Double latestModelAccuracy;
    private String latestModelVersion;
    private List<Map<String, Object>> hospitalStats;
    private List<Map<String, Object>> trainingHistory;
    private Map<String, Long> predictionDistribution;
}