package com.fedlearn.medical.repository;

import com.fedlearn.medical.entity.Job;
import com.fedlearn.medical.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByJobId(String jobId);

    List<Job> findByStatus(JobStatus status);

    List<Job> findTop10ByOrderByCreatedAtDesc();
}
