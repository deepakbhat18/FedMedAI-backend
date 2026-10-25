package com.fedlearn.medical.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlDiseasePredictionDto {

    @JsonProperty("disease_type")
    private String diseaseType;

    @JsonProperty("disease_name")
    private String diseaseName;

    private Integer prediction;

    @JsonProperty("prediction_label")
    private String predictionLabel;

    private Double confidence;

    @JsonProperty("risk_level")
    private String riskLevel;

    private List<Double> probabilities;
}
