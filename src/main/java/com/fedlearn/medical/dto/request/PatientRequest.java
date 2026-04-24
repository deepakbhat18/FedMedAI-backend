package com.fedlearn.medical.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequest {

    @NotNull(message = "Hospital ID is required")
    private Long hospitalId;

    @NotBlank(message = "Patient code is required")
    @Size(max = 50)
    private String patientCode;

    @Min(0)
    @Max(150)
    private Integer age;

    private String gender;

    @NotNull(message = "Features are required")
    private Map<String, Object> features;

    @Size(max = 200)
    private String diagnosis;

    private Integer actualLabel;

}