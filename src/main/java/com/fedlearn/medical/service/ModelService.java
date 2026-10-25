package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.dto.response.JobResponse;
import com.fedlearn.medical.dto.response.ModelVersionResponse;

import java.util.List;
import java.util.Map;

public interface ModelService {

    List<ModelVersionResponse> getAllModels();

    ModelVersionResponse getModelById(Long id);

    ModelVersionResponse getLatestModel();

    Map<String, Object> triggerTraining(TrainingTriggerRequest request);

    JobResponse triggerTrainingAsync(TrainingTriggerRequest request);

    JobResponse getJobStatus(String jobId);
}