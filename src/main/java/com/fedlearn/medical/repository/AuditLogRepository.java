package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);

    List<AuditLog> findByHospitalIdOrderByTimestampDesc(Long hospitalId);

    Page<AuditLog> findByHospitalId(Long hospitalId, Pageable pageable);
}
