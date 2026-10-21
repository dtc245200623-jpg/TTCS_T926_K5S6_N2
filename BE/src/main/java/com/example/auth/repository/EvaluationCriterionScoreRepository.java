package com.example.auth.repository;

import com.example.auth.entity.EvaluationCriterionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationCriterionScoreRepository extends JpaRepository<EvaluationCriterionScore, Long> {

    List<EvaluationCriterionScore> findByEvaluationId(Long evaluationId);
}
