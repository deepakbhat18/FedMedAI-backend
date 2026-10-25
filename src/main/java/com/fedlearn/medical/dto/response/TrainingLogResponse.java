
package com.fedlearn.medical.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingLogResponse {
    private Long id;
    private Integer trainingRound;
    private Long hospitalId;
    private String hospitalName;
    private Long modelVersionId;
    private String modelVersionName;
    private Double localAccuracy;
    private Double localLoss;
    private Double localPrecision;
    private Double localRecall;
    private Double localF1Score;
    private Integer samplesUsed;
    private Integer epochs;
    private Double learningRate;
    private Integer batchSize;
    private Double trainingDurationSeconds;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}