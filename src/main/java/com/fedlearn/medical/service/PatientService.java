package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.PatientRequest;
import com.fedlearn.medical.dto.response.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PatientService {

    PatientResponse createPatient(PatientRequest request);

    List<PatientResponse> getAllPatients();

    Page<PatientResponse> getAllPatientsPaginated(Pageable pageable);

    PatientResponse getPatientById(Long id);

    List<PatientResponse> getPatientsByHospital(Long hospitalId);

    Page<PatientResponse> getPatientsByHospitalPaginated(Long hospitalId, Pageable pageable);

    PatientResponse updatePatient(Long id, PatientRequest request);

    void deletePatient(Long id);
}