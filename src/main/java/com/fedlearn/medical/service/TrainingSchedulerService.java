package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.enums.HospitalStatus;
import com.fedlearn.medical.repository.HospitalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TrainingSchedulerService {

    private static final Logger logger =
            LoggerFactory.getLogger(TrainingSchedulerService.class);

    @Autowired
    private ModelService modelService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${app.scheduler.epochs:5}")
    private int defaultEpochs;

    @Value("${app.scheduler.learning-rate:0.01}")
    private double defaultLearningRate;

    @Value("${app.scheduler.batch-size:32}")
    private int defaultBatchSize;

    @Value("${app.scheduler.engine:custom}")
    private String defaultEngine;

    @Value("${app.scheduler.num-rounds:3}")
    private int defaultNumRounds;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private LocalDateTime lastRunTime = null;
    private String lastRunStatus = "NEVER";
    private String lastRunMessage = "No training has been scheduled yet";
    private int totalRuns = 0;
    private int successfulRuns = 0;
    private int failedRuns = 0;
    private final List<Map<String, Object>> runHistory = new ArrayList<>();

    @Scheduled(cron = "${app.scheduler.cron:0 0 2 * * MON}")
    public void scheduledTraining() {
        if (!schedulerEnabled) {
            logger.info("Scheduler is disabled. Skipping scheduled training.");
            return;
        }

        logger.info("========================================");
        logger.info("  SCHEDULED FL TRAINING TRIGGERED");
        logger.info("  Time: {}", LocalDateTime.now());
        logger.info("========================================");

        runTraining("SCHEDULED");
    }

    public Map<String, Object> triggerManually() {
        logger.info("Manual FL training trigger requested");

        if (isRunning.get()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Training is already in progress. Please wait.");
            response.put("isRunning", true);
            return response;
        }

        if (!mlServiceClient.isHealthy()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "ML Service is not available. Cannot trigger training.");
            response.put("mlServiceStatus", "DOWN");
            return response;
        }

        return runTraining("MANUAL");
    }

    private Map<String, Object> runTraining(String triggerType) {
        if (!isRunning.compareAndSet(false, true)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Training already in progress");
            return response;
        }

        LocalDateTime startTime = LocalDateTime.now();
        totalRuns++;

        try {
            logger.info("Starting {} FL training round...", triggerType);

            List<Long> activeHospitalIds = hospitalRepository
                    .findByStatus(HospitalStatus.ACTIVE)
                    .stream()
                    .map(h -> h.getId())
                    .collect(java.util.stream.Collectors.toList());

            if (activeHospitalIds.isEmpty()) {
                String msg = "No active hospitals found. Skipping training.";
                logger.warn(msg);
                updateState("SKIPPED", msg, startTime);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", msg);
                return response;
            }

            logger.info("Training with {} active hospitals: {}",
                    activeHospitalIds.size(), activeHospitalIds);


            TrainingTriggerRequest request = TrainingTriggerRequest.builder()
                    .hospitalIds(activeHospitalIds)
                    .epochs(defaultEpochs)
                    .learningRate(defaultLearningRate)
                    .batchSize(defaultBatchSize)
                    .engine(defaultEngine)
                    .numRounds(defaultNumRounds)
                    .build();

            Map<String, Object> result = modelService.triggerTraining(request);


            String status = "COMPLETED".equals(result.get("status"))
                    ? "SUCCESS" : "PARTIAL";
            String message = String.format(
                    "Training round %s completed. Accuracy: %s",
                    result.get("trainingRound"),
                    result.get("globalAccuracy")
            );

            updateState(status, message, startTime);
            successfulRuns++;

            logger.info("Scheduled training completed successfully: {}", message);

            Map<String, Object> response = new HashMap<>(result);
            response.put("triggerType", triggerType);
            response.put("scheduledAt", startTime.toString());
            response.put("completedAt", LocalDateTime.now().toString());
            return response;

        } catch (Exception e) {
            String errorMsg = "Training failed: " + e.getMessage();
            logger.error("Scheduled training failed: {}", e.getMessage(), e);
            updateState("FAILED", errorMsg, startTime);
            failedRuns++;

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("status", "FAILED");
            response.put("error", e.getMessage());
            response.put("message", errorMsg);
            response.put("triggerType", triggerType);
            return response;

        } finally {
            isRunning.set(false);
        }
    }

    private void updateState(String status, String message, LocalDateTime startTime) {
        lastRunTime = startTime;
        lastRunStatus = status;
        lastRunMessage = message;

        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("runTime", startTime.toString());
        historyEntry.put("status", status);
        historyEntry.put("message", message);
        historyEntry.put("duration", java.time.Duration.between(
                startTime, LocalDateTime.now()).getSeconds() + "s");

        runHistory.add(0, historyEntry); // Add to front
        if (runHistory.size() > 20) {
            runHistory.remove(runHistory.size() - 1); // Remove oldest
        }
    }

    public Map<String, Object> getStatus() {

        Map<String, Object> status = new HashMap<>();

        status.put("enabled",           schedulerEnabled);
        status.put("isRunning",         isRunning.get());
        status.put("cronExpression",    "${app.scheduler.cron:0 0 2 * * MON}");
        status.put("cronDescription",   "Every Monday at 2:00 AM");


        status.put("lastRunTime",
                lastRunTime != null ? lastRunTime.toString() : null);


        status.put("lastRunStatus",
                lastRunStatus != null ? lastRunStatus : "NEVER");
        status.put("lastRunMessage",
                lastRunMessage != null ? lastRunMessage
                        : "No training has been scheduled yet");

        status.put("totalScheduledRuns", totalRuns);
        status.put("successfulRuns",     successfulRuns);
        status.put("failedRuns",         failedRuns);


        status.put("runHistory",
                runHistory != null ? runHistory
                        : new java.util.ArrayList<>());


        Map<String, Object> config = new HashMap<>();
        config.put("epochs",       defaultEpochs);
        config.put("learningRate", defaultLearningRate);
        config.put("batchSize",    defaultBatchSize);
        config.put("engine",
                defaultEngine != null ? defaultEngine : "custom");
        config.put("numRounds",    defaultNumRounds);
        status.put("config", config);

        return status;
    }
    public boolean isRunning() { return isRunning.get(); }
    public String getLastRunStatus() { return lastRunStatus; }
    public LocalDateTime getLastRunTime() { return lastRunTime; }
}