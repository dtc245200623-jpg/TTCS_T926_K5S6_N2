package com.example.auth.repository;

import com.example.auth.entity.CompetencyCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetencyCriterionRepository extends JpaRepository<CompetencyCriterion, Long> {

    List<CompetencyCriterion> findByFrameworkIdOrderByOrderIndexAsc(Long frameworkId);

    void deleteByFrameworkId(Long frameworkId);
}
