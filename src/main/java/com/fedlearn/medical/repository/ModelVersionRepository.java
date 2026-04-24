
package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.ModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {
    Optional<ModelVersion> findByVersionName(String versionName);
    Optional<ModelVersion> findByIsActiveTrue();
    List<ModelVersion> findAllByOrderByTrainingRoundDesc();

    @Query("SELECT MAX(m.trainingRound) FROM ModelVersion m")
    Optional<Integer> findMaxTrainingRound();

    @Modifying
    @Transactional
    @Query("UPDATE ModelVersion m SET m.isActive = false WHERE m.isActive = true")
    void deactivateAllModels();
}