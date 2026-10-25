
package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.Hospital;
import com.fedlearn.medical.enums.HospitalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByCode(String code);
    boolean existsByCode(String code);
    List<Hospital> findByStatus(HospitalStatus status);
    Optional<Hospital> findByAdminUserId(Long adminUserId);

    @Query("SELECT h FROM Hospital h WHERE h.adminUser.username = :username")
    Optional<Hospital> findByAdminUserUsername(String username);

    @Query("SELECT h FROM Hospital h WHERE h.adminUser.id = :userId")
    Optional<Hospital> findByAdminUser(Long userId);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.hospital.id = :hospitalId")
    Long countPatientsByHospitalId(Long hospitalId);
}