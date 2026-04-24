package com.fedlearn.medical.entity;

import com.fedlearn.medical.enums.TrainingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "training_round", nullable = false)
    private Integer trainingRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_version_id")
    private ModelVersion modelVersion;

    @Column(name = "local_accuracy")
    private Double localAccuracy;

    @Column(name = "local_loss")
    private Double localLoss;

    @Column(name = "local_precision")
    private Double localPrecision;

    @Column(name = "local_recall")
    private Double localRecall;

    @Column(name = "local_f1_score")
    private Double localF1Score;

    @Column(name = "samples_used")
    private Integer samplesUsed;

    private Integer epochs;

    @Column(name = "learning_rate")
    private Double learningRate;

    @Column(name = "batch_size")
    private Integer batchSize;

    @Column(name = "training_duration_seconds")
    private Double trainingDurationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TrainingStatus status = TrainingStatus.COMPLETED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}