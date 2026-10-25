package com.fedlearn.medical.dto.ml;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlPredictRequest {

    @NotEmpty(message = "Features list cannot be empty")
    private List<Double> features;

    private String diseaseType;
}
