package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.InterviewQuestionRequest;
import com.example.auth.dto.InterviewQuestionResponse;
import com.example.auth.service.InterviewQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewQuestionController {

    private final InterviewQuestionService questionService;

    /**
     * KN-187: Tìm kiếm và lọc ngân hàng câu hỏi phỏng vấn theo:
     * - Chức danh tuyển dụng (positionId)
     * - Khung năng lực (frameworkId)
     * - Tiêu chí năng lực (criterionId)
     * - Mức độ khó: EASY, MEDIUM, HARD (difficulty)
     * - Từ khóa tìm kiếm nội dung / gợi ý câu trả lời (search)
     * - Trạng thái kích hoạt (activeOnly)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'INTERVIEWER', 'RECRUITER')")
    public ResponseEntity<ApiResponse<List<InterviewQuestionResponse>>> searchQuestions(
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) Long frameworkId,
            @RequestParam(required = false) Long criterionId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly) {

        List<InterviewQuestionResponse> list = questionService.searchQuestions(
                positionId,
                frameworkId,
                criterionId,
                difficulty,
                search,
                activeOnly
        );

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi phỏng vấn thành công", list));
    }

    /**
     * Lấy chi tiết một câu hỏi phỏng vấn
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'INTERVIEWER', 'RECRUITER')")
    public ResponseEntity<ApiResponse<InterviewQuestionResponse>> getQuestionById(@PathVariable Long id) {
        InterviewQuestionResponse res = questionService.getQuestionById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết câu hỏi thành công", res));
    }

    /**
     * KN-186: Lấy danh sách câu hỏi theo tiêu chí năng lực
     */
    @GetMapping("/criterion/{criterionId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'INTERVIEWER', 'RECRUITER')")
    public ResponseEntity<ApiResponse<List<InterviewQuestionResponse>>> getQuestionsByCriterion(
            @PathVariable Long criterionId) {
        List<InterviewQuestionResponse> list = questionService.getQuestionsByCriterionId(criterionId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi theo tiêu chí thành công", list));
    }

    /**
     * Lấy danh sách câu hỏi theo khung năng lực
     */
    @GetMapping("/framework/{frameworkId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'INTERVIEWER', 'RECRUITER')")
    public ResponseEntity<ApiResponse<List<InterviewQuestionResponse>>> getQuestionsByFramework(
            @PathVariable Long frameworkId) {
        List<InterviewQuestionResponse> list = questionService.getQuestionsByFrameworkId(frameworkId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi theo khung năng lực thành công", list));
    }

    /**
     * Lấy danh sách câu hỏi gợi ý cho chức danh tuyển dụng
     */
    @GetMapping("/position/{positionId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN', 'INTERVIEWER', 'RECRUITER')")
    public ResponseEntity<ApiResponse<List<InterviewQuestionResponse>>> getQuestionsByPosition(
            @PathVariable Long positionId) {
        List<InterviewQuestionResponse> list = questionService.getQuestionsByPositionId(positionId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi cho chức danh thành công", list));
    }

    /**
     * KN-186: Thêm mới câu hỏi phỏng vấn gắn với tiêu chí năng lực và độ khó (Dễ / Trung bình / Khó)
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewQuestionResponse>> createQuestion(
            @Valid @RequestBody InterviewQuestionRequest request) {
        InterviewQuestionResponse created = questionService.createQuestion(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm câu hỏi phỏng vấn mới thành công", created));
    }

    /**
     * KN-186: Cập nhật nội dung câu hỏi, độ khó, gợi ý câu trả lời tốt
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewQuestionResponse>> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody InterviewQuestionRequest request) {
        InterviewQuestionResponse updated = questionService.updateQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi phỏng vấn thành công", updated));
    }

    /**
     * Đổi trạng thái kích hoạt câu hỏi
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewQuestionResponse>> toggleStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active) {
        InterviewQuestionResponse res = questionService.toggleQuestionStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái câu hỏi thành công", res));
    }

    /**
     * Xóa câu hỏi khỏi ngân hàng câu hỏi
     * Quyền: Trưởng phòng Nhân sự, Admin
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa câu hỏi thành công", null));
    }
}
