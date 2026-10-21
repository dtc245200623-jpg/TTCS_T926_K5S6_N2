package com.example.auth.repository;

import com.example.auth.entity.CategoryType;
import com.example.auth.entity.RecruitmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruitmentCategoryRepository extends JpaRepository<RecruitmentCategory, Long> {

    // KN-193: Lấy danh mục theo loại sắp xếp theo thứ tự hiển thị
    List<RecruitmentCategory> findByTypeOrderByDisplayOrderAsc(CategoryType type);

    List<RecruitmentCategory> findByTypeAndActiveTrueOrderByDisplayOrderAsc(CategoryType type);

    Optional<RecruitmentCategory> findByTypeAndCodeIgnoreCase(CategoryType type, String code);

    boolean existsByTypeAndCodeIgnoreCase(CategoryType type, String code);

    boolean existsByTypeAndCodeIgnoreCaseAndIdNot(CategoryType type, String code, Long id);

    @Query("SELECT c FROM RecruitmentCategory c " +
           "WHERE (:type IS NULL OR c.type = :type) " +
           "AND (:active IS NULL OR c.active = :active) " +
           "AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.type ASC, c.displayOrder ASC, c.id ASC")
    List<RecruitmentCategory> searchCategories(
            @Param("type") CategoryType type,
            @Param("active") Boolean active,
            @Param("search") String search
    );

    // KN-192: Kiểm tra các ràng buộc tham chiếu
    @Query("SELECT COUNT(a) FROM Application a WHERE a.source.id = :categoryId")
    long countApplicationSourceReferences(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.rejectionReason.id = :categoryId")
    long countApplicationRejectionReasonReferences(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(j) FROM JobRequest j WHERE j.location.id = :categoryId")
    long countJobRequestLocationReferences(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(j) FROM JobRequest j WHERE j.employmentType.id = :categoryId")
    long countJobRequestEmploymentTypeReferences(@Param("categoryId") Long categoryId);
}
