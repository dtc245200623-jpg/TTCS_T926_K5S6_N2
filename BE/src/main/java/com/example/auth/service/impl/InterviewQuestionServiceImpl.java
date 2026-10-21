package com.example.auth.service.impl;

import com.example.auth.dto.InterviewQuestionRequest;
import com.example.auth.dto.InterviewQuestionResponse;
import com.example.auth.entity.CompetencyCriterion;
import com.example.auth.entity.InterviewQuestion;
import com.example.auth.entity.JobPosition;
import com.example.auth.exception.AppException;
import com.example.auth.repository.CompetencyCriterionRepository;
import com.example.auth.repository.CompetencyFrameworkRepository;
import com.example.auth.repository.InterviewQuestionRepository;
import com.example.auth.repository.JobPositionRepository;
import com.example.auth.service.InterviewQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private final InterviewQuestionRepository questionRepository;
    private final CompetencyCriterionRepository criterionRepository;
    private final CompetencyFrameworkRepository frameworkRepository;
    private final JobPositionRepository positionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> searchQuestions(
            Long positionId,
            Long frameworkId,
            Long criterionId,
            String difficulty,
            String search,
            Boolean activeOnly
    ) {
        // KN-187: Lọc theo chức danh -> suy ra khung năng lực dùng chung của chức danh đó
        if (positionId != null) {
            JobPosition position = positionRepository.findById(positionId)
                    .orElseThrow(() -> new AppException("Không tìm thấy chức danh với ID: " + positionId));

            if (position.getCompetencyFramework() == null) {
                // Chức danh chưa được gắn khung năng lực
                return Collections.emptyList();
            }

            Long posFrameworkId = position.getCompetencyFramework().getId();
            if (frameworkId != null && !frameworkId.equals(posFrameworkId)) {
                // Khung năng lực được chọn không khớp với chức danh
                return Collections.emptyList();
            }
            frameworkId = posFrameworkId;
        }

        // Chuẩn hóa độ khó
        String normalizedDifficulty = null;
        if (difficulty != null && !difficulty.trim().isEmpty() && !"ALL".equalsIgnoreCase(difficulty.trim())) {
            normalizedDifficulty = difficulty.trim().toUpperCase();
            if (!normalizedDifficulty.matches("^(EASY|MEDIUM|HARD)$")) {
                throw new AppException("Mức độ khó không hợp lệ: " + difficulty + ". Chấp nhận EASY, MEDIUM, HARD.");
            }
        }

        // Chuẩn hóa từ khóa tìm kiếm
        String normalizedSearch = null;
        if (search != null && !search.trim().isEmpty()) {
            normalizedSearch = search.trim();
        }

        List<InterviewQuestion> questions = questionRepository.searchQuestions(
                criterionId,
                frameworkId,
                normalizedDifficulty,
                activeOnly,
                normalizedSearch
        );

        return questions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewQuestionResponse getQuestionById(Long id) {
        InterviewQuestion question = questionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException("Không tìm thấy câu hỏi phỏng vấn với ID: " + id));
        return toResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> getQuestionsByCriterionId(Long criterionId) {
        if (!criterionRepository.existsById(criterionId)) {
            throw new AppException("Không tìm thấy tiêu chí năng lực với ID: " + criterionId);
        }
        return questionRepository.findByCriterionId(criterionId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> getQuestionsByFrameworkId(Long frameworkId) {
        if (!frameworkRepository.existsById(frameworkId)) {
            throw new AppException("Không tìm thấy khung năng lực với ID: " + frameworkId);
        }
        return questionRepository.findByCriterionFrameworkId(frameworkId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> getQuestionsByPositionId(Long positionId) {
        return searchQuestions(positionId, null, null, null, null, true);
    }

    @Override
    @Transactional
    public InterviewQuestionResponse createQuestion(InterviewQuestionRequest request) {
        log.info("Creating interview question for criterionId: {}", request.getCriterionId());

        CompetencyCriterion criterion = criterionRepository.findById(request.getCriterionId())
                .orElseThrow(() -> new AppException("Không tìm thấy tiêu chí năng lực với ID: " + request.getCriterionId()));

        String difficulty = request.getDifficulty().trim().toUpperCase();
        if (!difficulty.matches("^(EASY|MEDIUM|HARD)$")) {
            throw new AppException("Mức độ khó phải là EASY, MEDIUM hoặc HARD");
        }

        InterviewQuestion question = InterviewQuestion.builder()
                .criterion(criterion)
                .content(request.getContent().trim())
                .difficulty(difficulty)
                .goodAnswerGuideline(request.getGoodAnswerGuideline() != null ? request.getGoodAnswerGuideline().trim() : null)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        InterviewQuestion saved = questionRepository.save(question);
        log.info("Created interview question successfully: ID {}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InterviewQuestionResponse updateQuestion(Long id, InterviewQuestionRequest request) {
        log.info("Updating interview question ID: {}", id);

        InterviewQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy câu hỏi phỏng vấn với ID: " + id));

        if (request.getCriterionId() != null && !request.getCriterionId().equals(question.getCriterion().getId())) {
            CompetencyCriterion criterion = criterionRepository.findById(request.getCriterionId())
                    .orElseThrow(() -> new AppException("Không tìm thấy tiêu chí năng lực với ID: " + request.getCriterionId()));
            question.setCriterion(criterion);
        }

        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            question.setContent(request.getContent().trim());
        }

        if (request.getDifficulty() != null && !request.getDifficulty().trim().isEmpty()) {
            String difficulty = request.getDifficulty().trim().toUpperCase();
            if (!difficulty.matches("^(EASY|MEDIUM|HARD)$")) {
                throw new AppException("Mức độ khó phải là EASY, MEDIUM hoặc HARD");
            }
            question.setDifficulty(difficulty);
        }

        if (request.getGoodAnswerGuideline() != null) {
            question.setGoodAnswerGuideline(request.getGoodAnswerGuideline().trim());
        }

        if (request.getActive() != null) {
            question.setActive(request.getActive());
        }

        InterviewQuestion updated = questionRepository.save(question);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public InterviewQuestionResponse toggleQuestionStatus(Long id, Boolean active) {
        InterviewQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy câu hỏi phỏng vấn với ID: " + id));

        boolean newStatus = (active != null) ? active : !Boolean.TRUE.equals(question.getActive());
        question.setActive(newStatus);
        InterviewQuestion saved = questionRepository.save(question);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        InterviewQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy câu hỏi phỏng vấn với ID: " + id));
        questionRepository.delete(question);
        log.info("Deleted interview question ID: {}", id);
    }

    private InterviewQuestionResponse toResponse(InterviewQuestion q) {
        CompetencyCriterion c = q.getCriterion();
        String diff = q.getDifficulty() != null ? q.getDifficulty().toUpperCase() : "MEDIUM";
        String label;
        switch (diff) {
            case "EASY":
                label = "Dễ";
                break;
            case "HARD":
                label = "Khó";
                break;
            case "MEDIUM":
            default:
                label = "Trung bình";
                break;
        }

        return InterviewQuestionResponse.builder()
                .id(q.getId())
                .criterionId(c != null ? c.getId() : null)
                .criterionName(c != null ? c.getName() : null)
                .criterionWeight(c != null ? c.getWeight() : null)
                .frameworkId(c != null && c.getFramework() != null ? c.getFramework().getId() : null)
                .frameworkCode(c != null && c.getFramework() != null ? c.getFramework().getCode() : null)
                .frameworkName(c != null && c.getFramework() != null ? c.getFramework().getName() : null)
                .content(q.getContent())
                .difficulty(diff)
                .difficultyLabel(label)
                .goodAnswerGuideline(q.getGoodAnswerGuideline())
                .active(q.getActive())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}
