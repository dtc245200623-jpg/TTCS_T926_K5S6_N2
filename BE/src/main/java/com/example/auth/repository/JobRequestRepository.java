package com.example.auth.repository;

import com.example.auth.entity.JobRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JobRequestRepository extends JpaRepository<JobRequest, Long> {
    List<JobRequest> findByStatus(String status);
    List<JobRequest> findByHiringManagerId(Long hiringManagerId);
    List<JobRequest> findByHiringManagerIdOrderByCreatedAtDesc(Long hiringManagerId);
    List<JobRequest> findAllByOrderByCreatedAtDesc();
    List<JobRequest> findByDepartmentId(Long departmentId);
    boolean existsByDepartmentIdAndStatusIn(Long departmentId, Collection<String> statuses);
    long countByDepartmentIdAndStatusIn(Long departmentId, Collection<String> statuses);
    long countByDepartmentId(Long departmentId);
}
