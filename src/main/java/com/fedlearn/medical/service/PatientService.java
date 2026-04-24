package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.PatientRequest;
import com.fedlearn.medical.dto.response.PatientResponse;
import com.fedlearn.medical.entity.Hospital;
import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.enums.Gender;
import com.fedlearn.medical.exception.BadRequestException;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.HospitalRepository;
import com.fedlearn.medical.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        Hospital hospital = hospitalRepository.findById(request.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hospital", "id", request.getHospitalId()));

        // Check duplicate patient code within hospital
        patientRepository.findByHospitalIdAndPatientCode(
                        request.getHospitalId(), request.getPatientCode())
                .ifPresent(p -> {
                    throw new BadRequestException(
                            "Patient code already exists in this hospital: "
                                    + request.getPatientCode());
                });

        Gender gender = null;
        if (request.getGender() != null) {
            try {
                gender = Gender.valueOf(request.getGender().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid gender: " + request.getGender());
            }
        }

//        Patient patient = Patient.builder()
//                .hospital(hospital)
//                .patientCode(request.getPatientCode())
//                .age(request.getAge())
//                .gender(gender)
//                .features(request.getFeatures())
//                .diagnosis(request.getDiagnosis())
//                .actualLabel(request.getActualLabel())
//                .build();
        Patient patient = Patient.builder()
                .hospital(hospital)
                .patientCode(request.getPatientCode())
           // ← ADD THIS
                .age(request.getAge())
                .gender(gender)
                .features(request.getFeatures())
                .diagnosis(request.getDiagnosis())
                .actualLabel(request.getActualLabel())
                .build();

        patient = patientRepository.save(patient);
        return mapToResponse(patient);
    }

    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PatientResponse getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        return mapToResponse(patient);
    }

    public List<PatientResponse> getPatientsByHospital(Long hospitalId) {
        // Verify hospital exists
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new ResourceNotFoundException("Hospital", "id", hospitalId);
        }
        return patientRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));

        if (request.getAge() != null) patient.setAge(request.getAge());
        if (request.getFeatures() != null) patient.setFeatures(request.getFeatures());
        if (request.getDiagnosis() != null) patient.setDiagnosis(request.getDiagnosis());
        if (request.getActualLabel() != null) patient.setActualLabel(request.getActualLabel());

        if (request.getGender() != null) {
            try {
                patient.setGender(Gender.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid gender: " + request.getGender());
            }
        }

        patient = patientRepository.save(patient);
        return mapToResponse(patient);
    }

    @Transactional
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient", "id", id);
        }
        patientRepository.deleteById(id);
    }

    private PatientResponse mapToResponse(Patient patient) {
//        return PatientResponse.builder()
//                .id(patient.getId())
//                .hospitalId(patient.getHospital().getId())
//                .hospitalName(patient.getHospital().getName())
//                .patientCode(patient.getPatientCode())
//                .age(patient.getAge())
//                .gender(patient.getGender() != null ? patient.getGender().name() : null)
//                .features(patient.getFeatures())
//                .diagnosis(patient.getDiagnosis())
//                .actualLabel(patient.getActualLabel())
//                .createdAt(patient.getCreatedAt())
//                .updatedAt(patient.getUpdatedAt())
//                .build();
        return PatientResponse.builder()
                .id(patient.getId())
                .hospitalId(patient.getHospital().getId())
                .hospitalName(patient.getHospital().getName())
                .patientCode(patient.getPatientCode())
                // ← ADD THIS
                .age(patient.getAge())
                .gender(patient.getGender() != null ? patient.getGender().name() : null)
                .features(patient.getFeatures())
                .diagnosis(patient.getDiagnosis())
                .actualLabel(patient.getActualLabel())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }
}