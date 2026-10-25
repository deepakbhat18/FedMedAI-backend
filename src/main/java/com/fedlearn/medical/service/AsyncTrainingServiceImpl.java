package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.entity.Hospital;
import com.fedlearn.medical.entity.Job;
import com.fedlearn.medical.entity.ModelVersion;
import com.fedlearn.medical.entity.TrainingLog;
import com.fedlearn.medical.enums.JobStatus;
import com.fedlearn.medical.enums.TrainingStatus;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.HospitalRepository;
import com.fedlearn.medical.repository.JobRepository;
import com.fedlearn.medical.repository.ModelVersionRepository;
import com.fedlearn.medical.repository.TrainingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AsyncTrainingServiceImpl implements AsyncTrainingService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTrainingServiceImpl.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ModelVersionRepository modelVersionRepository;

    @Autowired
    private TrainingLogRepository trainingLogRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Override
    @Async("taskExecutor")
    @SuppressWarnings("unchecked")
    public CompletableFuture<Job> executeTrainingAsync(String jobId, TrainingTriggerRequest request) {
        MDC.put("jobId", jobId);
        logger.info("[Job: {}] Starting background FL training job execution", jobId);

        Job job = jobRepository.findByJobId(jobId).orElse(null);
        if (job == null) {
            logger.error("[Job: {}] Job record not found in repository", jobId);
            MDC.remove("jobId");
            return CompletableFuture.completedFuture(null);
        }

        job.setStatus(JobStatus.RUNNING);
        job.setStartTime(LocalDateTime.now());
        jobRepository.save(job);

        int newRound = modelVersionRepository.findMaxTrainingRound().orElse(0) + 1;

        List<Hospital> hospitals = new ArrayList<>();
        for (Long hospitalId : request.getHospitalIds()) {
            Hospital hospital = hospitalRepository.findById(hospitalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hospital", "id", hospitalId));
            hospitals.add(hospital);
        }

        try {
            Map<String, Object> result = mlServiceClient.runFullFLRound(
                    request.getHospitalIds(),
                    request.getEpochs(),
                    request.getLearningRate(),
                    request.getBatchSize(),
                    request.getEngine(),
                    request.getNumRounds()
            );

            logger.info("[Job: {}] ML FL round completed cleanly: {}", jobId, result);

            Map<String, Object> diseaseMetrics = (Map<String, Object>) result.getOrDefault("disease_metrics", new HashMap<>());

            double avgAccuracy = diseaseMetrics.values().stream()
                    .filter(m -> m instanceof Map)
                    .mapToDouble(m -> {
                        Object acc = ((Map<?, ?>) m).get("accuracy");
                        return acc instanceof Number ? ((Number) acc).doubleValue() : 0.0;
                    })
                    .average()
                    .orElse(0.0);

            double avgLoss = diseaseMetrics.values().stream()
                    .filter(m -> m instanceof Map)
                    .mapToDouble(m -> {
                        Object loss = ((Map<?, ?>) m).get("loss");
                        return loss instanceof Number ? ((Number) loss).doubleValue() : 0.0;
                    })
                    .average()
                    .orElse(0.0);

            modelVersionRepository.deactivateAllModels();

            String versionName = "v" + newRound + ".0";
            String participatingIds = hospitals.stream()
                    .map(h -> h.getId().toString())
                    .collect(Collectors.joining(","));

            ModelVersion newModel = ModelVersion.builder()
                    .versionName(versionName)
                    .modelType("logistic_regression_multi_disease")
                    .accuracy(avgAccuracy)
                    .loss(avgLoss)
                    .isActive(true)
                    .trainingRound(newRound)
                    .participatingHospitals(participatingIds)
                    .aggregationMethod("fedavg")
                    .build();

            newModel = modelVersionRepository.save(newModel);

            List<TrainingLog> logs = new ArrayList<>();
            for (Hospital hospital : hospitals) {
                TrainingLog log = TrainingLog.builder()
                        .trainingRound(newRound)
                        .hospital(hospital)
                        .modelVersion(newModel)
                        .localAccuracy(avgAccuracy)
                        .localLoss(avgLoss)
                        .epochs(request.getEpochs())
                        .learningRate(request.getLearningRate())
                        .batchSize(request.getBatchSize())
                        .status(TrainingStatus.COMPLETED)
                        .build();
                logs.add(log);
            }
            trainingLogRepository.saveAll(logs);

            Map<String, Object> resultPayload = new HashMap<>();
            resultPayload.put("trainingRound", newRound);
            resultPayload.put("modelVersion", versionName);
            resultPayload.put("globalAccuracy", Math.round(avgAccuracy * 10000.0) / 10000.0);
            resultPayload.put("globalLoss", Math.round(avgLoss * 10000.0) / 10000.0);
            resultPayload.put("participatingHospitals", hospitals.size());
            resultPayload.put("diseaseMetrics", diseaseMetrics);

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletionTime(LocalDateTime.now());
            job.setResultData(resultPayload);
            jobRepository.save(job);

            logger.info("[Job: {}] Background FL training job completed successfully", jobId);

        } catch (Exception e) {
            logger.error("[Job: {}] Background FL training job failed: {}", jobId, e.getMessage(), e);

            job.setStatus(JobStatus.FAILED);
            job.setCompletionTime(LocalDateTime.now());
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        } finally {
            MDC.remove("jobId");
        }

        return CompletableFuture.completedFuture(job);
    }
}
