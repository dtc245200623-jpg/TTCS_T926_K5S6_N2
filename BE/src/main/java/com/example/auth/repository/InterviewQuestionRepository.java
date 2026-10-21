package com.example.auth.repository;

import com.example.auth.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    @Query("SELECT q FROM InterviewQuestion q JOIN FETCH q.criterion c JOIN FETCH c.framework f WHERE q.id = :id")
    Optional<InterviewQuestion> findByIdWithDetails(@Param("id") Long id);

    List<InterviewQuestion> findByCriterionId(Long criterionId);

    List<InterviewQuestion> findByCriterionFrameworkId(Long frameworkId);

    long countByCriterionId(Long criterionId);

    long countByCriterionFrameworkId(Long frameworkId);

    // KN-187: Lọc câu hỏi theo tiêu chí, khung năng lực, độ khó, trạng thái, và từ khóa tìm kiếm
    @Query("SELECT q FROM InterviewQuestion q " +
           "JOIN FETCH q.criterion c " +
           "JOIN FETCH c.framework f " +
           "WHERE (:criterionId IS NULL OR c.id = :criterionId) " +
           "AND (:frameworkId IS NULL OR f.id = :frameworkId) " +
           "AND (:difficulty IS NULL OR UPPER(q.difficulty) = UPPER(:difficulty)) " +
           "AND (:active IS NULL OR q.active = :active) " +
           "AND (:search IS NULL OR LOWER(q.content) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(q.goodAnswerGuideline) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY f.name ASC, c.orderIndex ASC, q.id DESC")
    List<InterviewQuestion> searchQuestions(
            @Param("criterionId") Long criterionId,
            @Param("frameworkId") Long frameworkId,
            @Param("difficulty") String difficulty,
            @Param("active") Boolean active,
            @Param("search") String search
    );
}
