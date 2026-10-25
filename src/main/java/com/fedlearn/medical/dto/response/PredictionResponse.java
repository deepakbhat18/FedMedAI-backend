
package com.fedlearn.medical.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponse {
    private Long id;
    private Long patientId;
    private String patientCode;
    private Long hospitalId;
    private String hospitalName;
    private Integer prediction;
    private String predictionLabel;
    private Double confidence;
    private String modelVersion;
    private Map<String, Object> featuresUsed;
    private LocalDateTime createdAt;
}