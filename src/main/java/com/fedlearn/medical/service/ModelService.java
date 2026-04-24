package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.dto.response.ModelVersionResponse;
import com.fedlearn.medical.entity.*;
import com.fedlearn.medical.enums.TrainingStatus;
import com.fedlearn.medical.exception.MLServiceException;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    @Autowired
    private ModelVersionRepository modelVersionRepository;

    @Autowired
    private TrainingLogRepository trainingLogRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private MLServiceClient mlServiceClient;

    public List<ModelVersionResponse> getAllModels() {
        return modelVersionRepository.findAllByOrderByTrainingRoundDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ModelVersionResponse getModelById(Long id) {
        ModelVersion model = modelVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ModelVersion", "id", id));
        return mapToResponse(model);
    }

    public ModelVersionResponse getLatestModel() {
        ModelVersion model = modelVersionRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active model found. Please trigger a training round first."));
        return mapToResponse(model);
    }

    /**
     * Trigger a full federated learning round:
     * 1. Each hospital trains locally
     * 2. Weights are sent to central server
     * 3. FedAvg aggregation
     * 4. New global model saved
     */
//    @Transactional
//    public Map<String, Object> triggerTraining(TrainingTriggerRequest request) {
//        logger.info("=== STARTING FEDERATED LEARNING ROUND ===");
//
//        // Determine new round number
//        int newRound = modelVersionRepository.findMaxTrainingRound().orElse(0) + 1;
//        logger.info("Training Round: {}", newRound);
//
//        // Validate hospitals exist
//        List<Hospital> hospitals = new ArrayList<>();
//        for (Long hospitalId : request.getHospitalIds()) {
//            Hospital hospital = hospitalRepository.findById(hospitalId)
//                    .orElseThrow(() -> new ResourceNotFoundException(
//                            "Hospital", "id", hospitalId));
//            hospitals.add(hospital);
//        }
//
//        // Collect results from each hospital
//        List<Map<String, Object>> allHospitalWeights = new ArrayList<>();
//        List<TrainingLog> trainingLogs = new ArrayList<>();
//        int totalSamples = 0;
//
//        for (Hospital hospital : hospitals) {
//            logger.info("Training at hospital: {} ({})", hospital.getName(), hospital.getId());
//
//            // Get hospital's patient data
//            List<Patient> patients = patientRepository.findByHospitalId(hospital.getId());
//            if (patients.isEmpty()) {
//                logger.warn("Hospital {} has no patients, skipping", hospital.getId());
//                continue;
//            }
//
//            // Prepare training data
//            List<List<Double>> trainingData = new ArrayList<>();
//            List<Integer> labels = new ArrayList<>();
//
//            for (Patient patient : patients) {
//                if (patient.getFeatures() != null && patient.getActualLabel() != null) {
//                    List<Double> featureVector = extractFeatureVector(patient.getFeatures());
//                    trainingData.add(featureVector);
//                    labels.add(patient.getActualLabel());
//                }
//            }
//
//            if (trainingData.isEmpty()) {
//                logger.warn("Hospital {} has no valid training data, skipping", hospital.getId());
//                continue;
//            }
//
//            try {
//                // Call ML service for local training
//                Map<String, Object> trainResult = mlServiceClient.trainLocal(
//                        hospital.getId(),
//                        trainingData,
//                        labels,
//                        request.getEpochs(),
//                        request.getLearningRate(),
//                        request.getBatchSize(),
//                        null // Will be global weights in later rounds
//                );
//
//                // Store hospital weights for aggregation
//                Map<String, Object> hospitalWeight = new HashMap<>();
//                hospitalWeight.put("hospital_id", hospital.getId());
//                hospitalWeight.put("weights", trainResult.get("local_weights"));
//                hospitalWeight.put("samples_count", trainingData.size());
//                allHospitalWeights.add(hospitalWeight);
//                totalSamples += trainingData.size();
//
//                // Create training log
//                Map<String, Object> metrics = (Map<String, Object>) trainResult
//                        .getOrDefault("metrics", new HashMap<>());
//
//                TrainingLog log = TrainingLog.builder()
//                        .trainingRound(newRound)
//                        .hospital(hospital)
//                        .localAccuracy(getDouble(metrics, "accuracy"))
//                        .localLoss(getDouble(metrics, "loss"))
//                        .localPrecision(getDouble(metrics, "precision"))
//                        .localRecall(getDouble(metrics, "recall"))
//                        .localF1Score(getDouble(metrics, "f1_score"))
//                        .samplesUsed(trainingData.size())
//                        .epochs(request.getEpochs())
//                        .learningRate(request.getLearningRate())
//                        .batchSize(request.getBatchSize())
//                        .trainingDurationSeconds(getDouble(trainResult, "training_time_seconds"))
//                        .status(TrainingStatus.COMPLETED)
//                        .build();
//
//                trainingLogs.add(log);
//                logger.info("Hospital {} training complete. Accuracy: {}",
//                        hospital.getId(), getDouble(metrics, "accuracy"));
//
//            } catch (MLServiceException e) {
//                logger.error("Training failed for hospital {}: {}",
//                        hospital.getId(), e.getMessage());
//
//                TrainingLog failedLog = TrainingLog.builder()
//                        .trainingRound(newRound)
//                        .hospital(hospital)
//                        .samplesUsed(trainingData.size())
//                        .epochs(request.getEpochs())
//                        .learningRate(request.getLearningRate())
//                        .batchSize(request.getBatchSize())
//                        .status(TrainingStatus.FAILED)
//                        .errorMessage(e.getMessage())
//                        .build();
//
//                trainingLogs.add(failedLog);
//            }
//        }
//
//        // Aggregate if we have weights from at least one hospital
//        ModelVersion newModel = null;
//        if (!allHospitalWeights.isEmpty()) {
//            try {
//                Map<String, Object> aggregationResult = mlServiceClient.aggregate(
//                        allHospitalWeights);
//
//                // Calculate average metrics
//                double avgAccuracy = trainingLogs.stream()
//                        .filter(l -> l.getLocalAccuracy() != null)
//                        .mapToDouble(TrainingLog::getLocalAccuracy)
//                        .average().orElse(0.0);
//                double avgLoss = trainingLogs.stream()
//                        .filter(l -> l.getLocalLoss() != null)
//                        .mapToDouble(TrainingLog::getLocalLoss)
//                        .average().orElse(0.0);
//
//                // Deactivate current active model
//                modelVersionRepository.deactivateAllModels();
//
//                // Save new model version
//                String versionName = "v" + newRound + ".0";
//                String participatingIds = hospitals.stream()
//                        .map(h -> h.getId().toString())
//                        .collect(Collectors.joining(","));
//
//                newModel = ModelVersion.builder()
//                        .versionName(versionName)
//                        .modelType("logistic_regression")
//                        .accuracy(avgAccuracy)
//                        .loss(avgLoss)
//                        .isActive(true)
//                        .trainingRound(newRound)
//                        .participatingHospitals(participatingIds)
//                        .totalSamples(totalSamples)
//                        .aggregationMethod("fedavg")
//                        .build();
//
//                newModel = modelVersionRepository.save(newModel);
//                logger.info("New global model saved: {}", versionName);
//
//                // Update training logs with model version
//                for (TrainingLog log : trainingLogs) {
//                    log.setModelVersion(newModel);
//                }
//
//            } catch (MLServiceException e) {
//                logger.error("Aggregation failed: {}", e.getMessage());
//                // Still save training logs even if aggregation fails
//            }
//        }
//
//        // Save all training logs
//        trainingLogRepository.saveAll(trainingLogs);
//
//        // Build response
//        Map<String, Object> response = new HashMap<>();
//        response.put("trainingRound", newRound);
//        response.put("status", newModel != null ? "COMPLETED" : "PARTIAL_FAILURE");
//        response.put("participatingHospitals", hospitals.size());
//        response.put("successfulTrainings", trainingLogs.stream()
//                .filter(l -> l.getStatus() == TrainingStatus.COMPLETED).count());
//        response.put("totalSamples", totalSamples);
//        if (newModel != null) {
//            response.put("modelVersion", newModel.getVersionName());
//            response.put("globalAccuracy", newModel.getAccuracy());
//        }
//        response.put("message", "Federated learning round " + newRound + " completed");
//
//        logger.info("=== FEDERATED LEARNING ROUND {} COMPLETE ===", newRound);
//        return response;
//    }
    @Transactional
    public Map<String, Object> triggerTraining(TrainingTriggerRequest request) {
        logger.info("=== STARTING FEDERATED LEARNING ROUND ===");

        // Determine round number
        int newRound = modelVersionRepository
                .findMaxTrainingRound().orElse(0) + 1;
        logger.info("Training Round: {}", newRound);

        // Validate hospitals
        List<Hospital> hospitals = new ArrayList<>();
        for (Long hospitalId : request.getHospitalIds()) {
            Hospital hospital = hospitalRepository.findById(hospitalId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Hospital", "id", hospitalId));
            hospitals.add(hospital);
        }

        Map<String, Object> result;

        try {
            // Call Python ML service for FULL FL round
//            result = mlServiceClient.runFullFLRound(
//                    request.getHospitalIds(),
//                    request.getEpochs(),
//                    request.getLearningRate(),
//                    request.getBatchSize()
//            );
            result = mlServiceClient.runFullFLRound(
                    request.getHospitalIds(),
                    request.getEpochs(),
                    request.getLearningRate(),
                    request.getBatchSize(),
                    request.getEngine(),
                    request.getNumRounds()
            );

            logger.info("FL round completed: {}", result);

            // Extract metrics from ML service response
            Map<String, Object> diseaseMetrics =
                    (Map<String, Object>) result.getOrDefault(
                            "disease_metrics", new HashMap<>());

            // Calculate average accuracy across diseases
            double avgAccuracy = diseaseMetrics.values().stream()
                    .filter(m -> m instanceof Map)
                    .mapToDouble(m -> {
                        Object acc = ((Map<?, ?>) m).get("accuracy");
                        return acc instanceof Number
                                ? ((Number) acc).doubleValue() : 0.0;
                    })
                    .average()
                    .orElse(0.0);

            double avgLoss = diseaseMetrics.values().stream()
                    .filter(m -> m instanceof Map)
                    .mapToDouble(m -> {
                        Object loss = ((Map<?, ?>) m).get("loss");
                        return loss instanceof Number
                                ? ((Number) loss).doubleValue() : 0.0;
                    })
                    .average()
                    .orElse(0.0);

            // Deactivate old model
            modelVersionRepository.deactivateAllModels();

            // Save new model version
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

            // Save training logs for each hospital
            Integer diseasesUpdated = (Integer) result
                    .getOrDefault("diseases_updated", 0);
            Boolean success = (Boolean) result.getOrDefault("success", false);

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
                        .status(Boolean.TRUE.equals(success)
                                ? TrainingStatus.COMPLETED
                                : TrainingStatus.FAILED)
                        .build();
                logs.add(log);
            }
            trainingLogRepository.saveAll(logs);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("trainingRound", newRound);
            response.put("status", "COMPLETED");
            response.put("participatingHospitals", hospitals.size());
            response.put("successfulTrainings", hospitals.size());
            response.put("modelVersion", versionName);
            response.put("globalAccuracy", Math.round(avgAccuracy * 10000.0) / 10000.0);
            response.put("globalLoss", Math.round(avgLoss * 10000.0) / 10000.0);
            response.put("diseasesUpdated", diseasesUpdated);
            response.put("diseaseMetrics", diseaseMetrics);
            response.put("message", "Federated learning round "
                    + newRound + " completed successfully");

            logger.info("=== FL ROUND {} COMPLETE. Accuracy: {} ===",
                    newRound, avgAccuracy);

            return response;

        } catch (Exception e) {
            logger.error("FL round failed: {}", e.getMessage());

            // Save failed logs
            List<TrainingLog> failedLogs = new ArrayList<>();
            for (Hospital hospital : hospitals) {
                TrainingLog log = TrainingLog.builder()
                        .trainingRound(newRound)
                        .hospital(hospital)
                        .epochs(request.getEpochs())
                        .learningRate(request.getLearningRate())
                        .batchSize(request.getBatchSize())
                        .status(TrainingStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .build();
                failedLogs.add(log);
            }
            trainingLogRepository.saveAll(failedLogs);

            Map<String, Object> response = new HashMap<>();
            response.put("trainingRound", newRound);
            response.put("status", "FAILED");
            response.put("error", e.getMessage());
            response.put("message", "FL round failed: " + e.getMessage());

            return response;
        }
    }


    private List<Double> extractFeatureVector(Map<String, Object> features) {
        String[] keys = {"bloodPressure", "cholesterol", "heartRate", "bmi",
                "glucose", "smoking", "diabetes", "familyHistory"};
        List<Double> vector = new ArrayList<>();
        for (String key : keys) {
            Object value = features.get(key);
            if (value instanceof Number) {
                vector.add(((Number) value).doubleValue());
            } else {
                vector.add(0.0);
            }
        }
        return vector;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
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