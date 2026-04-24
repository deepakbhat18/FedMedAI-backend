
package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.TrainingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingLogRepository extends JpaRepository<TrainingLog, Long> {
    List<TrainingLog> findByTrainingRound(Integer trainingRound);
    List<TrainingLog> findByHospitalId(Long hospitalId);
    List<TrainingLog> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT t.trainingRound FROM TrainingLog t ORDER BY t.trainingRound DESC")
    List<Integer> findDistinctTrainingRounds();

    @Query("SELECT t FROM TrainingLog t WHERE t.hospital.id = :hospitalId " +
            "ORDER BY t.trainingRound DESC")
    List<TrainingLog> findByHospitalIdOrderByRoundDesc(Long hospitalId);
}