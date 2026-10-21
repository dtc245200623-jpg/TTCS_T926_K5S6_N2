package com.example.auth.repository;

import com.example.auth.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByInterviewId(Long interviewId);

    boolean existsByInterviewId(Long interviewId);

    @Query("SELECT e FROM Evaluation e LEFT JOIN FETCH e.criterionScores WHERE e.interview.id = :interviewId")
    Optional<Evaluation> findByInterviewIdWithCriterionScores(@Param("interviewId") Long interviewId);
}
