
package com.fedlearn.medical.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalResponse {
    private Long id;
    private String name;
    private String code;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private String status;
    private Long adminUserId;
    private String adminUserName;
    private Long patientCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}