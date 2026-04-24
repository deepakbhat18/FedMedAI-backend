package com.fedlearn.medical.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerStatusResponse {

    private Boolean enabled;
    private Boolean isRunning;
    private String cronExpression;
    private String cronDescription;
    private LocalDateTime lastRunTime;
    private String lastRunStatus;
    private String lastRunMessage;
    private LocalDateTime nextRunTime;
    private Integer totalScheduledRuns;
    private Integer successfulRuns;
    private Integer failedRuns;
    private List<Map<String, Object>> runHistory;
}