package com.fedlearn.medical.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalEmailRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Patient email is required")
    @Email(message = "Invalid email format")
    private String patientEmail;

    private String patientName;

    @NotNull(message = "Predictions are required")
    private List<Map<String, Object>> predictions;

    private Map<String, Object> recommendations;

    private Map<String, Object> features;

    private String subject;
}