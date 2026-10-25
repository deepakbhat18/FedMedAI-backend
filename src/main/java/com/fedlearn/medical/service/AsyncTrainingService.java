package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.TrainingTriggerRequest;
import com.fedlearn.medical.entity.Job;

import java.util.concurrent.CompletableFuture;

public interface AsyncTrainingService {

    CompletableFuture<Job> executeTrainingAsync(String jobId, TrainingTriggerRequest request);
}
