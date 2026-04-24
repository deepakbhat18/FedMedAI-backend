package com.fedlearn.medical.service;

import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.entity.Prediction;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.HospitalRepository;
import com.fedlearn.medical.repository.PatientRepository;
import com.fedlearn.medical.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    public byte[] exportPatientsCsv(Long hospitalId) {
        logger.info("Exporting patients CSV. HospitalId filter: {}", hospitalId);

        List<Patient> patients;

        if (hospitalId != null) {

            if (!hospitalRepository.existsById(hospitalId)) {
                throw new ResourceNotFoundException("Hospital", "id", hospitalId);
            }
            patients = patientRepository.findByHospitalId(hospitalId);
        } else {
            patients = patientRepository.findAll();
        }

        logger.info("Exporting {} patients", patients.size());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {


            writer.println(
                    "ID,Patient Code,Hospital,Age,Gender," +
                            "Diagnosis,Actual Label,Email," +
                            "Blood Pressure,Cholesterol,Heart Rate,BMI,Glucose," +
                            "Smoking,Diabetes History,Family History," +
                            "Creatinine,Hemoglobin,Bilirubin,ALT Enzyme," +
                            "Albumin,Potassium,Sodium,Urea," +
                            "Created At"
            );


            for (Patient patient : patients) {
                StringBuilder row = new StringBuilder();

                row.append(escapeCsv(patient.getId())).append(",");
                row.append(escapeCsv(patient.getPatientCode())).append(",");
                row.append(escapeCsv(patient.getHospital().getName())).append(",");
                row.append(escapeCsv(patient.getAge())).append(",");
                row.append(escapeCsv(patient.getGender() != null
                        ? patient.getGender().name() : "")).append(",");
                row.append(escapeCsv(patient.getDiagnosis())).append(",");
                row.append(escapeCsv(patient.getActualLabel())).append(",");



                if (patient.getFeatures() != null) {
                    row.append(escapeCsv(getFeature(patient, "bloodPressure"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "cholesterol"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "heartRate"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "bmi"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "glucose"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "smoking"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "diabetes"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "familyHistory"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "creatinine"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "hemoglobin"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "bilirubin"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "alt_enzyme"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "albumin"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "potassium"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "sodium"))).append(",");
                    row.append(escapeCsv(getFeature(patient, "urea"))).append(",");
                } else {

                    row.append(",,,,,,,,,,,,,,,,");
                }

                row.append(escapeCsv(patient.getCreatedAt() != null
                        ? patient.getCreatedAt().format(DATE_FORMATTER) : ""));

                writer.println(row);
            }

            writer.flush();
            logger.info("Patients CSV generated: {} bytes", out.size());
            return out.toByteArray();

        } catch (IOException e) {
            logger.error("Failed to generate patients CSV: {}", e.getMessage());
            throw new RuntimeException("CSV generation failed: " + e.getMessage());
        }
    }

    public byte[] exportPredictionsCsv(Long hospitalId) {
        logger.info("Exporting predictions CSV. HospitalId filter: {}", hospitalId);

        List<Prediction> predictions;

        if (hospitalId != null) {
            if (!hospitalRepository.existsById(hospitalId)) {
                throw new ResourceNotFoundException("Hospital", "id", hospitalId);
            }
            predictions = predictionRepository.findByHospitalId(hospitalId);
        } else {
            predictions = predictionRepository.findAll();
        }

        logger.info("Exporting {} predictions", predictions.size());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {


            writer.println(
                    "ID,Patient Code,Hospital,Prediction,Prediction Label," +
                            "Confidence,Risk Level,Model Version,Created At"
            );


            for (Prediction prediction : predictions) {
                StringBuilder row = new StringBuilder();

                row.append(escapeCsv(prediction.getId())).append(",");
                row.append(escapeCsv(prediction.getPatient().getPatientCode())).append(",");
                row.append(escapeCsv(prediction.getHospital().getName())).append(",");
                row.append(escapeCsv(prediction.getPrediction())).append(",");
                row.append(escapeCsv(prediction.getPredictionLabel())).append(",");


                Double confidence = prediction.getConfidence();
                if (confidence != null) {
                    row.append(String.format("%.2f%%", confidence * 100)).append(",");

                    String riskLevel = getRiskLevel(confidence);
                    row.append(escapeCsv(riskLevel)).append(",");
                } else {
                    row.append(",").append(",");
                }

                row.append(escapeCsv(prediction.getModelVersion() != null
                        ? prediction.getModelVersion().getVersionName() : "N/A")).append(",");
                row.append(escapeCsv(prediction.getCreatedAt() != null
                        ? prediction.getCreatedAt().format(DATE_FORMATTER) : ""));

                writer.println(row);
            }

            writer.flush();
            logger.info("Predictions CSV generated: {} bytes", out.size());
            return out.toByteArray();

        } catch (IOException e) {
            logger.error("Failed to generate predictions CSV: {}", e.getMessage());
            throw new RuntimeException("CSV generation failed: " + e.getMessage());
        }
    }

    public java.util.Map<String, Object> getExportStats(Long hospitalId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        if (hospitalId != null) {
            stats.put("patientCount",
                    patientRepository.countByHospitalId(hospitalId));
            stats.put("predictionCount",
                    predictionRepository.countByHospitalId(hospitalId));
            stats.put("hospitalId", hospitalId);
        } else {
            stats.put("patientCount", patientRepository.count());
            stats.put("predictionCount", predictionRepository.count());
            stats.put("hospitalId", "all");
        }

        stats.put("exportFormats", List.of("CSV"));
        return stats;
    }
    private String getFeature(Patient patient, String key) {
        if (patient.getFeatures() == null) return "";
        Object value = patient.getFeatures().get(key);
        return value != null ? value.toString() : "";
    }

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString();

        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            str = "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private String getRiskLevel(Double confidence) {
        if (confidence >= 0.75) return "HIGH";
        if (confidence >= 0.50) return "MODERATE";
        if (confidence >= 0.25) return "LOW";
        return "MINIMAL";
    }
}