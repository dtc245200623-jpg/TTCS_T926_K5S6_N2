package com.example.auth.service;

import com.example.auth.dto.InterviewQuestionRequest;
import com.example.auth.dto.InterviewQuestionResponse;

import java.util.List;

public interface InterviewQuestionService {

    // KN-187: Tìm kiếm và lọc câu hỏi theo chức danh, khung năng lực, tiêu chí, độ khó, từ khóa
    List<InterviewQuestionResponse> searchQuestions(
            Long positionId,
            Long frameworkId,
            Long criterionId,
            String difficulty,
            String search,
            Boolean activeOnly
    );

    InterviewQuestionResponse getQuestionById(Long id);

    // KN-186: Lấy danh sách câu hỏi theo tiêu chí
    List<InterviewQuestionResponse> getQuestionsByCriterionId(Long criterionId);

    // Lấy câu hỏi theo khung năng lực
    List<InterviewQuestionResponse> getQuestionsByFrameworkId(Long frameworkId);

    // Lấy câu hỏi theo chức danh tuyển dụng (thông qua khung năng lực của chức danh)
    List<InterviewQuestionResponse> getQuestionsByPositionId(Long positionId);

    // Thêm mới câu hỏi phỏng vấn gắn với tiêu chí năng lực và độ khó
    InterviewQuestionResponse createQuestion(InterviewQuestionRequest request);

    // Cập nhật câu hỏi
    InterviewQuestionResponse updateQuestion(Long id, InterviewQuestionRequest request);

    // Đổi trạng thái kích hoạt
    InterviewQuestionResponse toggleQuestionStatus(Long id, Boolean active);

    // Xóa câu hỏi
    void deleteQuestion(Long id);
}
