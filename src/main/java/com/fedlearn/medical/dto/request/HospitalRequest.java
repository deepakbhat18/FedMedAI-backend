
package com.fedlearn.medical.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalRequest {

    @NotBlank(message = "Hospital name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Hospital code is required")
    @Size(max = 20)
    private String code;

    @Size(max = 500)
    private String address;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Size(max = 20)
    private String contactPhone;

    private Long adminUserId;
}