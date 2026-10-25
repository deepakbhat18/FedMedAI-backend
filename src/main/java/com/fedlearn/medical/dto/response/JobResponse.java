package com.fedlearn.medical.dto.response;

import com.fedlearn.medical.enums.JobStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {

    private String jobId;
    private String jobType;
    private JobStatus status;
    private Map<String, Object> requestPayload;
    private Map<String, Object> resultData;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime completionTime;
    private LocalDateTime createdAt;
}
