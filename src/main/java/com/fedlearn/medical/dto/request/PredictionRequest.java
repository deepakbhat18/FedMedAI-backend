
package com.fedlearn.medical.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private String modelVersion;

    @NotNull(message = "Features are required")
    private List<Double> features;
}