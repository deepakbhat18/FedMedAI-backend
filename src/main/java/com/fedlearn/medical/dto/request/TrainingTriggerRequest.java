package com.fedlearn.medical.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingTriggerRequest {

    @NotNull(message = "Hospital IDs are required")
    @Size(min = 1, message = "At least one hospital must participate")
    private List<Long> hospitalIds;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer epochs = 5;

    @DecimalMin("0.0001")
    @DecimalMax("1.0")
    @Builder.Default
    private Double learningRate = 0.01;

    @Min(1)
    @Max(512)
    @Builder.Default
    private Integer batchSize = 32;

    @Builder.Default
    private String engine = "custom";

    @Min(1)
    @Max(20)
    @Builder.Default
    private Integer numRounds = 3;
}