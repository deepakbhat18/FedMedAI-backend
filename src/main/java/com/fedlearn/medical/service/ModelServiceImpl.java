package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.dto.response.JobResponse;
import com.fedlearn.medical.dto.response.ModelVersionResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ModelServiceImpl implements ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelServiceImpl.class);

    @Autowired
    private ModelVersionRepository modelVersionRepository;

    @Autowired
    private TrainingLogRepository trainingLogRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    @Autowired
    private AsyncTrainingService asyncTrainingService;

    @Override
    @Transactional(readOnly = true)
    public List<ModelVersionResponse> getAllModels() {
        return modelVersionRepository.findAllByOrderByTrainingRoundDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ModelVersionResponse getModelById(Long id) {
        ModelVersion model = modelVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelVersion", "id", id));
        return mapToResponse(model);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelVersionResponse getLatestModel() {
        ModelVersion model = modelVersionRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active model found. Please trigger a training round first."));
        return mapToResponse(model);
    }

    @Override
    @Transactional
    public JobResponse triggerTrainingAsync(TrainingTriggerRequest request) {
        String jobId = UUID.randomUUID().toString();
        logger.info("Initializing async training job: {}", jobId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hospitalIds", request.getHospitalIds());
        payload.put("epochs", request.getEpochs());
        payload.put("learningRate", request.getLearningRate());
        payload.put("batchSize", request.getBatchSize());

        Job job = Job.builder()
                .jobId(jobId)
                .jobType("FL_TRAINING")
                .status(JobStatus.PENDING)
                .requestPayload(payload)
                .build();

        job = jobRepository.save(job);

        asyncTrainingService.executeTrainingAsync(jobId, request);

        return mapToJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobStatus(String jobId) {
        Job job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "jobId", jobId));
        return mapToJobResponse(job);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> triggerTraining(TrainingTriggerRequest request) {
        logger.info("=== STARTING FEDERATED LEARNING ROUND (SYNC FALLBACK) ===");

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

            Map<String, Object> diseaseMetrics = (Map<String, Object>) result.getOrDefault("disease_metrics", new HashMap<>());
            double avgAccuracy = diseaseMetrics.values().stream()
                    .filter(m -> m instanceof Map)
                    .mapToDouble(m -> {
                        Object acc = ((Map<?, ?>) m).get("accuracy");
                        return acc instanceof Number ? ((Number) acc).doubleValue() : 0.0;
                    })
                    .average().orElse(0.0);

            double avgLoss = diseaseMetrics.values().stream()
                    .filter(m -> m instanceof Map)
                    .mapToDouble(m -> {
                        Object loss = ((Map<?, ?>) m).get("loss");
                        return loss instanceof Number ? ((Number) loss).doubleValue() : 0.0;
                    })
                    .average().orElse(0.0);

            modelVersionRepository.deactivateAllModels();

            String versionName = "v" + newRound + ".0";
            ModelVersion newModel = ModelVersion.builder()
                    .versionName(versionName)
                    .modelType("logistic_regression_multi_disease")
                    .accuracy(avgAccuracy)
                    .loss(avgLoss)
                    .isActive(true)
                    .trainingRound(newRound)
                    .aggregationMethod("fedavg")
                    .build();

            newModel = modelVersionRepository.save(newModel);

            Map<String, Object> response = new HashMap<>();
            response.put("trainingRound", newRound);
            response.put("status", "COMPLETED");
            response.put("modelVersion", versionName);
            response.put("globalAccuracy", Math.round(avgAccuracy * 10000.0) / 10000.0);

            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAILED");
            response.put("error", e.getMessage());
            return response;
        }
    }

    private JobResponse mapToJobResponse(Job job) {
        return JobResponse.builder()
                .jobId(job.getJobId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .requestPayload(job.getRequestPayload())
                .resultData(job.getResultData())
                .errorMessage(job.getErrorMessage())
                .startTime(job.getStartTime())
                .completionTime(job.getCompletionTime())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private ModelVersionResponse mapToResponse(ModelVersion model) {
        return ModelVersionResponse.builder()
                .id(model.getId())
                .versionName(model.getVersionName())
                .modelType(model.getModelType())
                .accuracy(model.getAccuracy())
                .loss(model.getLoss())
                .precisionScore(model.getPrecisionScore())
                .recallScore(model.getRecallScore())
                .f1Score(model.getF1Score())
                .isActive(model.getIsActive())
                .trainingRound(model.getTrainingRound())
                .participatingHospitals(model.getParticipatingHospitals())
                .totalSamples(model.getTotalSamples())
                .aggregationMethod(model.getAggregationMethod())
                .createdAt(model.getCreatedAt())
                .build();
    }
}
