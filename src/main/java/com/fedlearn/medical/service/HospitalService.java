
package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.HospitalEmailRequest;
import com.fedlearn.medical.dto.request.HospitalRequest;
import com.fedlearn.medical.dto.response.HospitalResponse;
import com.fedlearn.medical.entity.Hospital;
import com.fedlearn.medical.entity.User;
import com.fedlearn.medical.enums.HospitalStatus;
import com.fedlearn.medical.exception.BadRequestException;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.HospitalRepository;
import com.fedlearn.medical.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

import java.util.List;

import java.util.stream.Collectors;

@Service
public class HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;
    private static final Logger logger =
            LoggerFactory.getLogger(HospitalService.class);
    @Transactional
    public HospitalResponse createHospital(HospitalRequest request) {

        if (hospitalRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Hospital code already exists: " + request.getCode());
        }

        Hospital hospital = Hospital.builder()
                .name(request.getName())
                .code(request.getCode())
                .address(request.getAddress())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .status(HospitalStatus.ACTIVE)
                .build();


        if (request.getAdminUserId() != null) {
            User adminUser = userRepository.findById(request.getAdminUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", request.getAdminUserId()));
            hospital.setAdminUser(adminUser);
        }

        hospital = hospitalRepository.save(hospital);
        return mapToResponse(hospital);
    }

    public List<HospitalResponse> getAllHospitals() {
        return hospitalRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public HospitalResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", "id", id));
        return mapToResponse(hospital);
    }

    public HospitalResponse getHospitalByAdminUser(Long userId) {
        Hospital hospital = hospitalRepository.findByAdminUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hospital", "adminUserId", userId));
        return mapToResponse(hospital);
    }

    @Transactional
    public HospitalResponse updateHospital(Long id, HospitalRequest request) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", "id", id));

        if (request.getName() != null) hospital.setName(request.getName());
        if (request.getAddress() != null) hospital.setAddress(request.getAddress());
        if (request.getContactEmail() != null) hospital.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) hospital.setContactPhone(request.getContactPhone());

        if (request.getAdminUserId() != null) {
            User adminUser = userRepository.findById(request.getAdminUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", request.getAdminUserId()));
            hospital.setAdminUser(adminUser);
        }

        hospital = hospitalRepository.save(hospital);
        return mapToResponse(hospital);
    }

    @Transactional
    public void deleteHospital(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", "id", id));
        hospital.setStatus(HospitalStatus.INACTIVE);
        hospitalRepository.save(hospital);
    }

    public List<HospitalResponse> getActiveHospitals() {
        return hospitalRepository.findByStatus(HospitalStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    public Map<String, Object> emailPatientReport(
            Long hospitalId,
            HospitalEmailRequest request) {

        logger.info("Email report requested from hospital: {} to: {}",
                hospitalId, request.getPatientEmail());


        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hospital", "id", hospitalId));

        if (hospital.getStatus() != HospitalStatus.ACTIVE) {
            throw new BadRequestException(
                    "Hospital is not active: " + hospital.getName());
        }


        return emailService.sendReportFromHospital(hospitalId, request);
    }

    private HospitalResponse mapToResponse(Hospital hospital) {
        Long patientCount = hospitalRepository.countPatientsByHospitalId(hospital.getId());

        return HospitalResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .code(hospital.getCode())
                .address(hospital.getAddress())
                .contactEmail(hospital.getContactEmail())
                .contactPhone(hospital.getContactPhone())
                .status(hospital.getStatus().name())
                .adminUserId(hospital.getAdminUser() != null
                        ? hospital.getAdminUser().getId() : null)
                .adminUserName(hospital.getAdminUser() != null
                        ? hospital.getAdminUser().getFullName() : null)
                .patientCount(patientCount)
                .createdAt(hospital.getCreatedAt())
                .updatedAt(hospital.getUpdatedAt())
                .build();
    }
}