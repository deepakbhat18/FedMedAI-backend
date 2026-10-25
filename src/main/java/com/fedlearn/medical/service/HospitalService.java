package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.HospitalEmailRequest;
import com.fedlearn.medical.dto.request.HospitalRequest;
import com.fedlearn.medical.dto.response.HospitalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface HospitalService {

    HospitalResponse createHospital(HospitalRequest request);

    List<HospitalResponse> getAllHospitals();

    Page<HospitalResponse> getAllHospitalsPaginated(Pageable pageable);

    HospitalResponse getHospitalById(Long id);

    HospitalResponse getHospitalByAdminUser(Long userId);

    HospitalResponse updateHospital(Long id, HospitalRequest request);

    void deleteHospital(Long id);

    List<HospitalResponse> getActiveHospitals();

    Map<String, Object> emailPatientReport(Long hospitalId, HospitalEmailRequest request);
}