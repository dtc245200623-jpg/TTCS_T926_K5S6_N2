package com.example.auth.repository;

import com.example.auth.entity.ApprovalRuleStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRuleStepRepository extends JpaRepository<ApprovalRuleStep, Long> {
    List<ApprovalRuleStep> findByRuleIdOrderByStepOrderAsc(Long ruleId);
    void deleteByRuleId(Long ruleId);
}
