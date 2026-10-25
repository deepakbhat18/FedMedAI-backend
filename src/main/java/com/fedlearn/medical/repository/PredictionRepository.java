package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    @Override
    Optional<Prediction> findById(Long id);

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    List<Prediction> findByPatientId(Long patientId);

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    Page<Prediction> findByPatientId(Long patientId, Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    List<Prediction> findByHospitalId(Long hospitalId);

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    Page<Prediction> findByHospitalId(Long hospitalId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.hospital.id = :hospitalId")
    Long countByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("SELECT p.prediction, COUNT(p) FROM Prediction p GROUP BY p.prediction")
    List<Object[]> countByPredictionGrouped();

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    @Query("SELECT p FROM Prediction p ORDER BY p.createdAt DESC LIMIT 10")
    List<Prediction> findLatest10();

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    @Query("SELECT p FROM Prediction p WHERE p.patient.id = :patientId ORDER BY p.createdAt ASC")
    List<Prediction> findByPatientIdOrderByCreatedAtAsc(@Param("patientId") Long patientId);

    @EntityGraph(attributePaths = {"patient", "hospital", "modelVersion"})
    @Query("SELECT p FROM Prediction p WHERE p.patient.id = :patientId AND p.predictionLabel IS NOT NULL ORDER BY p.createdAt ASC")
    List<Prediction> findTimelineByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.patient.id = :patientId")
    Long countByPatientId(@Param("patientId") Long patientId);
}