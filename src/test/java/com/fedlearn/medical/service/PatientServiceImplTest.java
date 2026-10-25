package com.fedlearn.medical.service;

import com.fedlearn.medical.dto.request.PatientRequest;
import com.fedlearn.medical.dto.response.PatientResponse;
import com.fedlearn.medical.entity.Hospital;
import com.fedlearn.medical.entity.Patient;
import com.fedlearn.medical.enums.Gender;
import com.fedlearn.medical.enums.HospitalStatus;
import com.fedlearn.medical.exception.ResourceNotFoundException;
import com.fedlearn.medical.repository.HospitalRepository;
import com.fedlearn.medical.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Hospital testHospital;
    private Patient testPatient;
    private PatientRequest patientRequest;

    @BeforeEach
    void setUp() {
        testHospital = Hospital.builder()
                .id(1L)
                .name("City General Hospital")
                .code("HOSP-001")
                .status(HospitalStatus.ACTIVE)
                .build();

        testPatient = Patient.builder()
                .id(100L)
                .hospital(testHospital)
                .patientCode("PAT-100")
                .age(45)
                .gender(Gender.MALE)
                .features(new HashMap<>())
                .diagnosis("Hypertension")
                .build();

        patientRequest = PatientRequest.builder()
                .hospitalId(1L)
                .patientCode("PAT-100")
                .age(45)
                .gender("MALE")
                .features(new HashMap<>())
                .diagnosis("Hypertension")
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new patient")
    void createPatient_Success() {
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(testHospital));
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        PatientResponse response = patientService.createPatient(patientRequest);

        assertNotNull(response);
        assertEquals("PAT-100", response.getPatientCode());
        assertEquals("City General Hospital", response.getHospitalName());
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating patient for invalid hospital")
    void createPatient_HospitalNotFound_ThrowsException() {
        when(hospitalRepository.findById(99L)).thenReturn(Optional.empty());
        patientRequest.setHospitalId(99L);

        assertThrows(ResourceNotFoundException.class, () -> patientService.createPatient(patientRequest));
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return patient by ID")
    void getPatientById_Success() {
        when(patientRepository.findById(100L)).thenReturn(Optional.of(testPatient));

        PatientResponse response = patientService.getPatientById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("PAT-100", response.getPatientCode());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for missing patient ID")
    void getPatientById_NotFound_ThrowsException() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getPatientById(999L));
    }

    @Test
    @DisplayName("Should return patients by hospital ID")
    void getPatientsByHospital_Success() {
        when(hospitalRepository.existsById(1L)).thenReturn(true);
        when(patientRepository.findByHospitalId(1L)).thenReturn(List.of(testPatient));

        List<PatientResponse> results = patientService.getPatientsByHospital(1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("PAT-100", results.get(0).getPatientCode());
    }
}
