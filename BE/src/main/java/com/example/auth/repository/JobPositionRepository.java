package com.example.auth.repository;

import com.example.auth.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {

    List<JobPosition> findAllByOrderByNameAsc();

    List<JobPosition> findByActiveTrueOrderByNameAsc();

    List<JobPosition> findByDepartmentId(Long departmentId);

    List<JobPosition> findByCompetencyFrameworkId(Long frameworkId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<JobPosition> findByCode(String code);

    Optional<JobPosition> findByName(String name);

    @Query("SELECT jp FROM JobPosition jp LEFT JOIN FETCH jp.department LEFT JOIN FETCH jp.competencyFramework ORDER BY jp.name ASC")
    List<JobPosition> findAllWithDepartment();
}
