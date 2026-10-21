package com.example.auth.repository;

import com.example.auth.entity.HeadcountOverrideAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeadcountOverrideAuditRepository extends JpaRepository<HeadcountOverrideAudit, Long> {

    List<HeadcountOverrideAudit> findByJobRequestIdOrderByCreatedAtDesc(Long jobRequestId);

    List<HeadcountOverrideAudit> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    List<HeadcountOverrideAudit> findByYearOrderByCreatedAtDesc(Integer year);

    List<HeadcountOverrideAudit> findAllByOrderByCreatedAtDesc();
}
