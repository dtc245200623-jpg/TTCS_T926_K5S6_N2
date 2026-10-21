package com.example.auth.repository;

import com.example.auth.entity.DepartmentHeadcountPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentHeadcountPlanRepository extends JpaRepository<DepartmentHeadcountPlan, Long> {

    Optional<DepartmentHeadcountPlan> findByDepartmentIdAndYear(Long departmentId, Integer year);

    List<DepartmentHeadcountPlan> findByYearOrderByDepartmentNameAsc(Integer year);

    List<DepartmentHeadcountPlan> findByDepartmentIdOrderByYearDesc(Long departmentId);

    List<DepartmentHeadcountPlan> findAllByOrderByYearDescDepartmentNameAsc();

    boolean existsByDepartmentIdAndYear(Long departmentId, Integer year);

    boolean existsByDepartmentIdAndYearAndIdNot(Long departmentId, Integer year, Long id);
}
