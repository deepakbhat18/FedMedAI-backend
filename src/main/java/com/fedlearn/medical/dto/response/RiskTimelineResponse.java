package com.fedlearn.medical.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskTimelineResponse {

    private Long patientId;
    private String patientCode;
    private String hospitalName;

    private List<TimelinePoint> summary;

    private List<DetailedTimelinePoint> detailed;

    private Integer totalPredictions;
    private Double currentRiskScore;
    private String riskTrend;
    private LocalDateTime firstPrediction;
    private LocalDateTime lastPrediction;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelinePoint {
        private String date;
        private LocalDateTime timestamp;
        private Double avgRisk;
        private String riskLevel;
        private Integer diseasesDetected;
        private Integer totalDiseases;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailedTimelinePoint {
        private String date;
        private LocalDateTime timestamp;
        private Map<String, Double> diseaseConfidences;
        private Map<String, Integer> diseasePredictions;
        private Double avgRisk;
    }
}