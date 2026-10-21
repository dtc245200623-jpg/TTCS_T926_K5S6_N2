package com.example.auth.repository;

import com.example.auth.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByParentIsNullOrderByNameAsc();

    List<Department> findByParentIdOrderByNameAsc(Long parentId);

    List<Department> findAllByOrderByNameAsc();

    List<Department> findByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByParentId(Long parentId);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.manager LEFT JOIN FETCH d.parent WHERE d.parent IS NULL ORDER BY d.name ASC")
    List<Department> findRootDepartmentsWithDetails();

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.manager LEFT JOIN FETCH d.parent ORDER BY d.name ASC")
    List<Department> findAllWithDetails();
}
