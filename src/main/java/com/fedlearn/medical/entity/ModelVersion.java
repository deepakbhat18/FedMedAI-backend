package com.fedlearn.medical.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_name", unique = true, nullable = false, length = 20)
    private String versionName;

    @Column(name = "model_type", length = 50)
    @Builder.Default
    private String modelType = "logistic_regression";

    private Double accuracy;

    private Double loss;

    @Column(name = "precision_score")
    private Double precisionScore;

    @Column(name = "recall_score")
    private Double recallScore;

    @Column(name = "f1_score")
    private Double f1Score;

    @Column(name = "weights_path", length = 500)
    private String weightsPath;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "training_round", nullable = false)
    private Integer trainingRound;

    @Column(name = "participating_hospitals", length = 200)
    private String participatingHospitals;

    @Column(name = "total_samples")
    private Integer totalSamples;

    @Column(name = "aggregation_method", length = 50)
    @Builder.Default
    private String aggregationMethod = "fedavg";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}