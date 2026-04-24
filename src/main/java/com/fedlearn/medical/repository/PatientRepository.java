
package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByHospitalId(Long hospitalId);

    Optional<Patient> findByHospitalIdAndPatientCode(Long hospitalId, String patientCode);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.hospital.id = :hospitalId")
    Long countByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("SELECT p FROM Patient p WHERE p.hospital.id IN :hospitalIds")
    List<Patient> findByHospitalIds(@Param("hospitalIds") List<Long> hospitalIds);
}