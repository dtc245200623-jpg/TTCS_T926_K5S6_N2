package com.example.auth;

import com.example.auth.dto.InterviewQuestionRequest;
import com.example.auth.dto.InterviewQuestionResponse;
import com.example.auth.entity.CompetencyCriterion;
import com.example.auth.entity.CompetencyFramework;
import com.example.auth.entity.JobPosition;
import com.example.auth.exception.AppException;
import com.example.auth.repository.CompetencyCriterionRepository;
import com.example.auth.repository.CompetencyFrameworkRepository;
import com.example.auth.repository.InterviewQuestionRepository;
import com.example.auth.repository.JobPositionRepository;
import com.example.auth.service.InterviewQuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class InterviewQuestionServiceTest {

    @Autowired
    private InterviewQuestionService questionService;

    @Autowired
    private InterviewQuestionRepository questionRepository;

    @Autowired
    private CompetencyCriterionRepository criterionRepository;

    @Autowired
    private CompetencyFrameworkRepository frameworkRepository;

    @Autowired
    private JobPositionRepository positionRepository;

    private CompetencyFramework testFramework;
    private CompetencyCriterion criterionJava;
    private CompetencyCriterion criterionSql;
    private JobPosition testPosition;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Tạo Khung năng lực kiểm thử (KN-185)
        testFramework = CompetencyFramework.builder()
                .code("CF_TEST_" + uniqueSuffix)
                .name("Khung Năng Lực Test " + uniqueSuffix)
                .description("Mô tả khung test")
                .active(true)
                .build();
        testFramework = frameworkRepository.save(testFramework);

        // 2. Tạo các tiêu chí năng lực (KN-185)
        criterionJava = CompetencyCriterion.builder()
                .framework(testFramework)
                .name("Kiến thức Java Backend")
                .description("Core Java, Spring Boot")
                .weight(60.0)
                .scaleMax(10)
                .orderIndex(1)
                .build();
        criterionJava = criterionRepository.save(criterionJava);

        criterionSql = CompetencyCriterion.builder()
                .framework(testFramework)
                .name("Kỹ năng Cơ sở dữ liệu")
                .description("SQL Server, indexing, transactions")
                .weight(40.0)
                .scaleMax(10)
                .orderIndex(2)
                .build();
        criterionSql = criterionRepository.save(criterionSql);

        // 3. Tạo chức danh gán với khung năng lực (KN-187)
        testPosition = JobPosition.builder()
                .code("POS_TEST_" + uniqueSuffix)
                .name("Lập trình viên Java " + uniqueSuffix)
                .level("SENIOR")
                .competencyFramework(testFramework)
                .minSalary(20000000.0)
                .maxSalary(35000000.0)
                .active(true)
                .build();
        testPosition = positionRepository.save(testPosition);
    }

    @Test
    @DisplayName("KN-186: Thêm câu hỏi phỏng vấn gắn với tiêu chí năng lực và độ khó thành công")
    void testCreateQuestion_Success() {
        InterviewQuestionRequest req = InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Giải thích cơ chế Garbage Collection trong JVM và các thế hệ bộ nhớ?")
                .difficulty("MEDIUM")
                .goodAnswerGuideline("Ứng viên nêu rõ Eden, Survivor, Old Gen và cách các GC algorithms hoạt động.")
                .active(true)
                .build();

        InterviewQuestionResponse res = questionService.createQuestion(req);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("Kiến thức Java Backend", res.getCriterionName());
        assertEquals(testFramework.getId(), res.getFrameworkId());
        assertEquals("MEDIUM", res.getDifficulty());
        assertEquals("Trung bình", res.getDifficultyLabel());
        assertTrue(res.getContent().contains("Garbage Collection"));
        assertTrue(res.getGoodAnswerGuideline().contains("Eden"));
    }

    @Test
    @DisplayName("KN-186: Báo lỗi khi độ khó không hợp lệ")
    void testCreateQuestion_InvalidDifficulty() {
        InterviewQuestionRequest req = InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Câu hỏi test độ khó sai")
                .difficulty("SUPER_HARD")
                .build();

        assertThrows(AppException.class, () -> questionService.createQuestion(req));
    }

    @Test
    @DisplayName("KN-186: Báo lỗi khi tiêu chí năng lực không tồn tại")
    void testCreateQuestion_CriterionNotFound() {
        InterviewQuestionRequest req = InterviewQuestionRequest.builder()
                .criterionId(999999L)
                .content("Câu hỏi với criterion ID ảo")
                .difficulty("EASY")
                .build();

        assertThrows(AppException.class, () -> questionService.createQuestion(req));
    }

    @Test
    @DisplayName("KN-187: Lọc câu hỏi theo chức danh tuyển dụng (thông qua khung năng lực của chức danh)")
    void testSearchQuestions_ByPosition() {
        // Tạo câu hỏi gắn với criterionJava thuộc testFramework của testPosition
        questionService.createQuestion(InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Spring Boot Starter hoạt động như thế nào?")
                .difficulty("EASY")
                .goodAnswerGuideline("Giải thích Auto-configuration và conditional beans.")
                .build());

        questionService.createQuestion(InterviewQuestionRequest.builder()
                .criterionId(criterionSql.getId())
                .content("Phân biệt Clustered và Non-clustered Index?")
                .difficulty("HARD")
                .goodAnswerGuideline("Nêu cấu trúc B-Tree và sắp xếp vật lý dữ liệu.")
                .build());

        // Lọc theo positionId
        List<InterviewQuestionResponse> results = questionService.searchQuestions(
                testPosition.getId(), null, null, null, null, true
        );

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("KN-187: Lọc câu hỏi theo mức độ khó (Dễ / Trung bình / Khó)")
    void testSearchQuestions_ByDifficulty() {
        questionService.createQuestion(InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Java question Easy")
                .difficulty("EASY")
                .build());

        questionService.createQuestion(InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Java question Hard")
                .difficulty("HARD")
                .build());

        List<InterviewQuestionResponse> easyQuestions = questionService.searchQuestions(
                null, testFramework.getId(), null, "EASY", null, true
        );

        assertEquals(1, easyQuestions.size());
        assertEquals("EASY", easyQuestions.get(0).getDifficulty());
        assertEquals("Dễ", easyQuestions.get(0).getDifficultyLabel());
    }

    @Test
    @DisplayName("KN-187: Tìm kiếm câu hỏi theo từ khóa trong nội dung hoặc hướng dẫn trả lời")
    void testSearchQuestions_ByKeyword() {
        questionService.createQuestion(InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Phân biệt HashMap và ConcurrentHashMap?")
                .difficulty("MEDIUM")
                .goodAnswerGuideline("Nêu cơ chế Segment locking và CAS trong Java Concurrent.")
                .build());

        // Tìm từ khóa "ConcurrentHashMap"
        List<InterviewQuestionResponse> results1 = questionService.searchQuestions(
                null, testFramework.getId(), null, null, "ConcurrentHashMap", true
        );
        assertEquals(1, results1.size());

        // Tìm từ khóa "Segment locking" trong goodAnswerGuideline
        List<InterviewQuestionResponse> results2 = questionService.searchQuestions(
                null, testFramework.getId(), null, null, "Segment locking", true
        );
        assertEquals(1, results2.size());
    }

    @Test
    @DisplayName("Cập nhật câu hỏi và đổi trạng thái kích hoạt")
    void testUpdateAndToggleStatus() {
        InterviewQuestionResponse created = questionService.createQuestion(InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Câu hỏi ban đầu")
                .difficulty("EASY")
                .goodAnswerGuideline("Hướng dẫn ban đầu")
                .build());

        // Cập nhật
        InterviewQuestionRequest updateReq = InterviewQuestionRequest.builder()
                .criterionId(criterionJava.getId())
                .content("Câu hỏi đã cập nhật")
                .difficulty("HARD")
                .goodAnswerGuideline("Hướng dẫn đã cập nhật")
                .build();
        InterviewQuestionResponse updated = questionService.updateQuestion(created.getId(), updateReq);

        assertEquals("Câu hỏi đã cập nhật", updated.getContent());
        assertEquals("HARD", updated.getDifficulty());

        // Đổi trạng thái
        InterviewQuestionResponse toggled = questionService.toggleQuestionStatus(created.getId(), false);
        assertFalse(toggled.getActive());

        // Xóa
        questionService.deleteQuestion(created.getId());
        assertThrows(AppException.class, () -> questionService.getQuestionById(created.getId()));
    }
}
