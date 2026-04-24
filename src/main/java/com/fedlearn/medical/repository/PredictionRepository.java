
package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByPatientId(Long patientId);
    List<Prediction> findByHospitalId(Long hospitalId);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.hospital.id = :hospitalId")
    Long countByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("SELECT p.prediction, COUNT(p) FROM Prediction p GROUP BY p.prediction")
    List<Object[]> countByPredictionGrouped();

    @Query("SELECT p FROM Prediction p ORDER BY p.createdAt DESC LIMIT 10")
    List<Prediction> findLatest10();


    @Query("SELECT p FROM Prediction p " +
            "WHERE p.patient.id = :patientId " +
            "ORDER BY p.createdAt ASC")
    List<Prediction> findByPatientIdOrderByCreatedAtAsc(
            @Param("patientId") Long patientId);

    @Query("SELECT p FROM Prediction p " +
            "WHERE p.patient.id = :patientId " +
            "AND p.predictionLabel IS NOT NULL " +
            "ORDER BY p.createdAt ASC")
    List<Prediction> findTimelineByPatientId(
            @Param("patientId") Long patientId);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.patient.id = :patientId")
    Long countByPatientId(@Param("patientId") Long patientId);
}