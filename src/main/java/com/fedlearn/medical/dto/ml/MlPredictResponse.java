package com.fedlearn.medical.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlPredictResponse {

    private List<MlDiseasePredictionDto> predictions;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("total_diseases_checked")
    private Integer totalDiseasesChecked;

    private Map<String, Object> recommendations;
}
