package com.example.auth.repository;

import com.example.auth.entity.JobRequestApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRequestApprovalStepRepository extends JpaRepository<JobRequestApprovalStep, Long> {
    List<JobRequestApprovalStep> findByJobRequestIdOrderByStepOrderAsc(Long jobRequestId);
    Optional<JobRequestApprovalStep> findByJobRequestIdAndStepOrder(Long jobRequestId, Integer stepOrder);
    void deleteByJobRequestId(Long jobRequestId);
}
