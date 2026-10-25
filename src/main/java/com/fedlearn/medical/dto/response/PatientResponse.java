
package com.fedlearn.medical.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {
    private Long id;
    private Long hospitalId;
    private String hospitalName;
    private String patientCode;
    private Integer age;
    private String gender;
    private Map<String, Object> features;
    private String diagnosis;
    private Integer actualLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}