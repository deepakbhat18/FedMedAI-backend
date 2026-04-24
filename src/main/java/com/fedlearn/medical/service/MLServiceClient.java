package com.fedlearn.medical.service;

import com.fedlearn.medical.exception.MLServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class MLServiceClient {

    private static final Logger logger =
            LoggerFactory.getLogger(MLServiceClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.ml-service.base-url}")
    private String mlServiceBaseUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> predictAllDiseases(List<Double> features) {
        String url = mlServiceBaseUrl + "/ml/predict";
        logger.info("Calling ML predict (all diseases): {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("features", features);
            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                logger.info("ML multi-disease prediction successful");
                return response.getBody();
            }

            throw new MLServiceException("ML Service returned empty response");

        } catch (RestClientException e) {
            logger.error("ML Service error: {}", e.getMessage());
            throw new MLServiceException(
                    "Cannot connect to ML service: " + e.getMessage(), e);
        }
    }
    public Map<String, Object> predictSingleDisease(
            List<Double> features, String diseaseType) {
        String url = mlServiceBaseUrl + "/ml/predict/single"
                + "?disease_type=" + diseaseType;
        logger.info("Calling ML predict single disease: {}", diseaseType);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("features", features);
            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                return response.getBody();
            }

            throw new MLServiceException("Empty response from ML service");

        } catch (RestClientException e) {
            logger.error("ML predict single error: {}", e.getMessage());
            throw new MLServiceException(
                    "ML prediction failed: " + e.getMessage(), e);
        }
    }
    @SuppressWarnings("unchecked")
    public Map<String, Object> trainLocal(
            Long hospitalId, List<List<Double>> data,
            List<Integer> labels, int epochs,
            double learningRate, int batchSize,
            List<Double> globalWeights) {

        String url = mlServiceBaseUrl + "/ml/train";
        logger.info("Calling ML train for hospital: {}", hospitalId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "hospital_id", hospitalId,
                    "epochs", epochs,
                    "learning_rate", learningRate,
                    "batch_size", batchSize
            );

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                logger.info("ML training successful for hospital: {}", hospitalId);
                return response.getBody();
            }

            throw new MLServiceException("ML training returned empty response");

        } catch (RestClientException e) {
            logger.error("ML training error for hospital {}: {}",
                    hospitalId, e.getMessage());
            throw new MLServiceException(
                    "ML training failed: " + e.getMessage(), e);
        }
    }


    @SuppressWarnings("unchecked")
    public Map<String, Object> runFullFLRound(
            List<Long> hospitalIds, int epochs,
            double learningRate, int batchSize,
            String engine, int numRounds) {

        String url = mlServiceBaseUrl + "/ml/train/all";
        logger.info("Triggering full FL round. Engine: {}, Hospitals: {}",
                engine, hospitalIds);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("hospital_ids", hospitalIds);
            requestBody.put("epochs", epochs);
            requestBody.put("learning_rate", learningRate);
            requestBody.put("batch_size", batchSize);
            requestBody.put("engine", engine != null ? engine : "custom");
            requestBody.put("num_rounds", numRounds > 0 ? numRounds : 3);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                logger.info("FL round completed. Engine: {}", engine);
                return response.getBody();
            }

            throw new MLServiceException("FL round returned empty response");

        } catch (RestClientException e) {
            logger.error("FL round error: {}", e.getMessage());
            throw new MLServiceException(
                    "FL round failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> aggregate(
            List<Map<String, Object>> hospitalWeights) {

        String url = mlServiceBaseUrl + "/ml/aggregate";
        logger.info("Calling ML aggregate service");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "hospital_weights", hospitalWeights,
                    "aggregation_method", "fedavg"
            );

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                logger.info("ML aggregation successful");
                return response.getBody();
            }

            throw new MLServiceException("Aggregation returned empty response");

        } catch (RestClientException e) {
            logger.error("ML aggregation error: {}", e.getMessage());
            throw new MLServiceException(
                    "ML aggregation failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getModelStatus() {
        String url = mlServiceBaseUrl + "/ml/model/status";

        try {
            ResponseEntity<Map> response =
                    restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.warn("Could not get model status: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    public boolean isHealthy() {
        try {
            String url = mlServiceBaseUrl + "/ml/health";
            ResponseEntity<Map> response =
                    restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("ML Service health check failed: {}", e.getMessage());
            return false;
        }
    }
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFullAnalytics() {
        String url = mlServiceBaseUrl + "/ml/analytics/full";
        logger.info("Fetching full ML analytics");

        try {
            ResponseEntity<Map> response =
                    restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                logger.info("ML analytics fetched successfully");
                return response.getBody();
            }

            throw new MLServiceException("Empty analytics response");

        } catch (RestClientException e) {
            logger.error("ML analytics error: {}", e.getMessage());
            throw new MLServiceException(
                    "Cannot fetch ML analytics: " + e.getMessage(), e);
        }
    }
}