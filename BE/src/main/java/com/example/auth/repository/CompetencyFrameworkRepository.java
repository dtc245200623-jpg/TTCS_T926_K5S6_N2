package com.example.auth.repository;

import com.example.auth.entity.CompetencyFramework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetencyFrameworkRepository extends JpaRepository<CompetencyFramework, Long> {

    Optional<CompetencyFramework> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("SELECT DISTINCT f FROM CompetencyFramework f LEFT JOIN FETCH f.criteria c ORDER BY f.createdAt DESC")
    List<CompetencyFramework> findAllWithCriteria();

    @Query("SELECT f FROM CompetencyFramework f LEFT JOIN FETCH f.criteria c WHERE f.id = :id")
    Optional<CompetencyFramework> findByIdWithCriteria(@Param("id") Long id);
}
