package com.fedlearn.medical.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelVersionResponse {
    private Long id;
    private String versionName;
    private String modelType;
    private Double accuracy;
    private Double loss;
    private Double precisionScore;
    private Double recallScore;
    private Double f1Score;
    private Boolean isActive;
    private Integer trainingRound;
    private String participatingHospitals;
    private Integer totalSamples;
    private String aggregationMethod;
    private LocalDateTime createdAt;
}