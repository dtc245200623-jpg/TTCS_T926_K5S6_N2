package com.example.auth.service.impl;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.CompetencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetencyServiceImpl implements CompetencyService {

    private final CompetencyFrameworkRepository frameworkRepository;
    private final CompetencyCriterionRepository criterionRepository;
    private final JobPositionRepository positionRepository;
    private final InterviewRepository interviewRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationCriterionScoreRepository criterionScoreRepository;
    private final InterviewQuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompetencyFrameworkResponse> getAllFrameworks(Boolean activeOnly, String search) {
        List<CompetencyFramework> list = frameworkRepository.findAllWithCriteria();

        return list.stream()
                .filter(f -> {
                    if (Boolean.TRUE.equals(activeOnly) && !Boolean.TRUE.equals(f.getActive())) {
                        return false;
                    }
                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.trim().toLowerCase();
                        boolean matchName = f.getName() != null && f.getName().toLowerCase().contains(s);
                        boolean matchCode = f.getCode() != null && f.getCode().toLowerCase().contains(s);
                        return matchName || matchCode;
                    }
                    return true;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompetencyFrameworkResponse getFrameworkById(Long id) {
        CompetencyFramework framework = frameworkRepository.findByIdWithCriteria(id)
                .orElseThrow(() -> new AppException("Không tìm thấy khung năng lực với ID: " + id));
        return toResponse(framework);
    }

    @Override
    @Transactional
    public CompetencyFrameworkResponse createFramework(CompetencyFrameworkRequest request) {
        validateFrameworkRequest(request, null);

        CompetencyFramework framework = CompetencyFramework.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        // Xử lý tiêu chí đánh giá (KN-183)
        if (request.getCriteria() != null && !request.getCriteria().isEmpty()) {
            int order = 1;
            for (CompetencyCriterionDto cDto : request.getCriteria()) {
                CompetencyCriterion criterion = CompetencyCriterion.builder()
                        .framework(framework)
                        .name(cDto.getName().trim())
                        .description(cDto.getDescription())
                        .weight(cDto.getWeight())
                        .scaleMax(cDto.getScaleMax() != null ? cDto.getScaleMax() : 10)
                        .orderIndex(cDto.getOrderIndex() != null ? cDto.getOrderIndex() : order++)
                        .build();
                framework.getCriteria().add(criterion);
            }
        }

        CompetencyFramework saved = frameworkRepository.save(framework);

        // Ánh xạ chức danh nếu có truyền positionIds (KN-184)
        if (request.getPositionIds() != null && !request.getPositionIds().isEmpty()) {
            mapPositionsToFramework(saved, request.getPositionIds());
        }

        log.info("Đã tạo mới khung năng lực: {} [{}]", saved.getName(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CompetencyFrameworkResponse updateFramework(Long id, CompetencyFrameworkRequest request) {
        CompetencyFramework framework = frameworkRepository.findByIdWithCriteria(id)
                .orElseThrow(() -> new AppException("Không tìm thấy khung năng lực với ID: " + id));

        validateFrameworkRequest(request, id);

        framework.setCode(request.getCode().trim().toUpperCase());
        framework.setName(request.getName().trim());
        framework.setDescription(request.getDescription());
        if (request.getActive() != null) {
            framework.setActive(request.getActive());
        }

        // Cập nhật lại danh sách tiêu chí (KN-183)
        framework.getCriteria().clear();
        if (request.getCriteria() != null && !request.getCriteria().isEmpty()) {
            int order = 1;
            for (CompetencyCriterionDto cDto : request.getCriteria()) {
                CompetencyCriterion criterion = CompetencyCriterion.builder()
                        .framework(framework)
                        .name(cDto.getName().trim())
                        .description(cDto.getDescription())
                        .weight(cDto.getWeight())
                        .scaleMax(cDto.getScaleMax() != null ? cDto.getScaleMax() : 10)
                        .orderIndex(cDto.getOrderIndex() != null ? cDto.getOrderIndex() : order++)
                        .build();
                framework.getCriteria().add(criterion);
            }
        }

        CompetencyFramework saved = frameworkRepository.save(framework);

        // Cập nhật danh sách chức danh ánh xạ nếu có cung cấp
        if (request.getPositionIds() != null) {
            // Gỡ các chức danh cũ đang map với framework này mà không nằm trong danh sách mới
            List<JobPosition> currentMapped = positionRepository.findByCompetencyFrameworkId(saved.getId());
            for (JobPosition pos : currentMapped) {
                if (!request.getPositionIds().contains(pos.getId())) {
                    pos.setCompetencyFramework(null);
                    positionRepository.save(pos);
                }
            }
            // Map danh sách mới
            mapPositionsToFramework(saved, request.getPositionIds());
        }

        log.info("Đã cập nhật khung năng lực: {} [{}]", saved.getName(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CompetencyFrameworkResponse toggleFrameworkStatus(Long id, Boolean active) {
        CompetencyFramework framework = frameworkRepository.findByIdWithCriteria(id)
                .orElseThrow(() -> new AppException("Không tìm thấy khung năng lực với ID: " + id));

        boolean newActive = active != null ? active : !Boolean.TRUE.equals(framework.getActive());
        framework.setActive(newActive);
        CompetencyFramework saved = frameworkRepository.save(framework);
        log.info("Đã đổi trạng thái khung năng lực {} sang {}", saved.getCode(), newActive ? "Áp dụng" : "Ngừng áp dụng");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFramework(Long id) {
        CompetencyFramework framework = frameworkRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy khung năng lực với ID: " + id));

        // Gỡ bỏ ánh xạ của các chức danh trước khi xóa
        List<JobPosition> mappedPositions = positionRepository.findByCompetencyFrameworkId(id);
        for (JobPosition pos : mappedPositions) {
            pos.setCompetencyFramework(null);
            positionRepository.save(pos);
        }

        frameworkRepository.delete(framework);
        log.info("Đã xóa khung năng lực: {} [{}]", framework.getName(), framework.getCode());
    }

    @Override
    @Transactional
    public CompetencyFrameworkResponse assignPositions(Long frameworkId, List<Long> positionIds) {
        CompetencyFramework framework = frameworkRepository.findByIdWithCriteria(frameworkId)
                .orElseThrow(() -> new AppException("Không tìm thấy khung năng lực với ID: " + frameworkId));

        if (positionIds != null && !positionIds.isEmpty()) {
            mapPositionsToFramework(framework, positionIds);
        }

        return toResponse(framework);
    }

    @Override
    @Transactional
    public void unassignPosition(Long positionId) {
        JobPosition pos = positionRepository.findById(positionId)
                .orElseThrow(() -> new AppException("Không tìm thấy chức danh với ID: " + positionId));
        pos.setCompetencyFramework(null);
        positionRepository.save(pos);
        log.info("Đã hủy ánh xạ khung năng lực khỏi chức danh: {}", pos.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationTemplateResponse generateEvaluationTemplateForPosition(Long positionId) {
        JobPosition position = positionRepository.findById(positionId)
                .orElseThrow(() -> new AppException("Không tìm thấy chức danh với ID: " + positionId));

        CompetencyFramework framework = position.getCompetencyFramework();
        if (framework == null) {
            throw new AppException(String.format("Chức danh '%s' chưa được gán Khung năng lực nào. Vui lòng thiết lập khung năng lực cho chức danh trước!", position.getName()));
        }

        return buildTemplateResponse(position, framework, null);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationTemplateResponse generateEvaluationTemplateForInterview(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new AppException("Không tìm thấy buổi phỏng vấn với ID: " + interviewId));

        Application application = interview.getApplication();
        JobRequest jobRequest = application != null ? application.getJobRequest() : null;
        JobPosition position = jobRequest != null ? jobRequest.getPosition() : null;

        CompetencyFramework framework = null;
        if (position != null && position.getCompetencyFramework() != null) {
            framework = position.getCompetencyFramework();
        } else {
            // Nếu vị trí chưa gán khung năng lực riêng, tìm khung năng lực mặc định đang hoạt động
            List<CompetencyFramework> activeFws = frameworkRepository.findAllWithCriteria().stream()
                    .filter(f -> Boolean.TRUE.equals(f.getActive()))
                    .collect(Collectors.toList());
            if (!activeFws.isEmpty()) {
                framework = activeFws.get(0);
            }
        }

        if (framework == null) {
            throw new AppException("Hệ thống chưa có Khung năng lực nào được kích hoạt để sinh phiếu đánh giá.");
        }

        return buildTemplateResponse(position, framework, interview);
    }

    @Override
    @Transactional
    public EvaluationDetailResponse submitEvaluation(EvaluationSubmitRequest request, String currentUsername) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new AppException("Không tìm thấy buổi phỏng vấn với ID: " + request.getInterviewId()));

        if (request.getCriteriaScores() == null || request.getCriteriaScores().isEmpty()) {
            throw new AppException("Phiếu đánh giá phải có điểm đánh giá cho các tiêu chí năng lực.");
        }

        // Tính điểm tổng hợp có trọng số
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        for (EvaluationSubmitRequest.CriterionScoreInputDto item : request.getCriteriaScores()) {
            double weight = item.getWeight() != null ? item.getWeight() : 0.0;
            double score = item.getScore() != null ? item.getScore() : 0.0;
            totalWeightedScore += score * (weight / 100.0);
            totalWeight += weight;
        }

        // Lấy hoặc tạo Evaluation
        Evaluation evaluation = evaluationRepository.findByInterviewId(interview.getId())
                .orElse(new Evaluation());

        CompetencyFramework framework = null;
        if (request.getFrameworkId() != null) {
            framework = frameworkRepository.findById(request.getFrameworkId()).orElse(null);
        }

        evaluation.setInterview(interview);
        evaluation.setFramework(framework);
        evaluation.setFeedback(request.getFeedback());
        evaluation.setRecommendedAction(request.getRecommendedAction());
        evaluation.setWeightedScore(Math.round(totalWeightedScore * 100.0) / 100.0);
        evaluation.setScore((int) Math.round(totalWeightedScore));
        evaluation.setEvaluatedAt(LocalDateTime.now());

        // Xóa điểm chi tiết cũ nếu có
        if (evaluation.getId() != null) {
            criterionScoreRepository.deleteAll(criterionScoreRepository.findByEvaluationId(evaluation.getId()));
        }

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        // Lưu chi tiết điểm từng tiêu chí
        List<EvaluationCriterionScore> scoreDetails = new ArrayList<>();
        for (EvaluationSubmitRequest.CriterionScoreInputDto item : request.getCriteriaScores()) {
            CompetencyCriterion criterion = null;
            if (item.getCriterionId() != null) {
                criterion = criterionRepository.findById(item.getCriterionId()).orElse(null);
            }

            EvaluationCriterionScore scoreItem = EvaluationCriterionScore.builder()
                    .evaluation(savedEvaluation)
                    .criterion(criterion)
                    .criterionName(item.getCriterionName())
                    .weight(item.getWeight())
                    .score(item.getScore())
                    .comment(item.getComment())
                    .build();
            scoreDetails.add(criterionScoreRepository.save(scoreItem));
        }

        // Cập nhật trạng thái buổi phỏng vấn sang COMPLETED
        interview.setStatus("COMPLETED");
        interview.setFeedback(request.getFeedback());
        interviewRepository.save(interview);

        log.info("Đã lưu phiếu đánh giá phỏng vấn #{} bởi {}: Điểm trọng số = {}/10, Kết luận = {}",
                interview.getId(), currentUsername, savedEvaluation.getWeightedScore(), savedEvaluation.getRecommendedAction());

        return toEvaluationDetailResponse(savedEvaluation, scoreDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationDetailResponse getEvaluationByInterviewId(Long interviewId) {
        Evaluation evaluation = evaluationRepository.findByInterviewIdWithCriterionScores(interviewId)
                .orElseThrow(() -> new AppException("Chưa có phiếu đánh giá cho buổi phỏng vấn với ID: " + interviewId));

        List<EvaluationCriterionScore> details = criterionScoreRepository.findByEvaluationId(evaluation.getId());
        return toEvaluationDetailResponse(evaluation, details);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void validateFrameworkRequest(CompetencyFrameworkRequest request, Long currentId) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new AppException("Mã khung năng lực không được để trống.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException("Tên khung năng lực không được để trống.");
        }

        String code = request.getCode().trim().toUpperCase();
        boolean exists = currentId == null
                ? frameworkRepository.existsByCodeIgnoreCase(code)
                : frameworkRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (exists) {
            throw new AppException("Mã khung năng lực '" + code + "' đã tồn tại trên hệ thống.");
        }

        // KN-183: Quy tắc nghiệp vụ bắt buộc: Tổng trọng số của một khung bằng 100%
        if (request.getCriteria() != null && !request.getCriteria().isEmpty()) {
            double totalWeight = 0.0;
            for (CompetencyCriterionDto c : request.getCriteria()) {
                if (c.getName() == null || c.getName().trim().isEmpty()) {
                    throw new AppException("Tên tiêu chí đánh giá không được để trống.");
                }
                if (c.getWeight() == null || c.getWeight() <= 0) {
                    throw new AppException(String.format("Trọng số của tiêu chí '%s' phải lớn hơn 0%%.", c.getName()));
                }
                totalWeight += c.getWeight();
            }

            if (Math.abs(totalWeight - 100.0) > 0.001) {
                throw new AppException(String.format(
                        "Tổng trọng số của khung năng lực phải bằng 100%% (Hiện tại: %.1f%%). Vui lòng điều chỉnh lại trọng số của các tiêu chí!",
                        totalWeight
                ));
            }
        }
    }

    private void mapPositionsToFramework(CompetencyFramework framework, List<Long> positionIds) {
        for (Long posId : positionIds) {
            positionRepository.findById(posId).ifPresent(pos -> {
                pos.setCompetencyFramework(framework);
                positionRepository.save(pos);
            });
        }
    }

    private CompetencyFrameworkResponse toResponse(CompetencyFramework f) {
        List<CompetencyCriterionDto> criteriaDtos = f.getCriteria().stream()
                .map(c -> {
                    long count = questionRepository.countByCriterionId(c.getId());
                    return CompetencyCriterionDto.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .description(c.getDescription())
                            .weight(c.getWeight())
                            .scaleMax(c.getScaleMax())
                            .orderIndex(c.getOrderIndex())
                            .questionCount((int) count)
                            .build();
                })
                .collect(Collectors.toList());

        double totalWeight = criteriaDtos.stream().mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0).sum();

        // Lấy danh sách chức danh dùng chung khung năng lực này (KN-182)
        List<JobPosition> mappedPositions = positionRepository.findByCompetencyFrameworkId(f.getId());
        List<PositionSummaryDto> positionDtos = mappedPositions.stream()
                .map(p -> PositionSummaryDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .level(p.getLevel())
                        .departmentName(p.getDepartment() != null ? p.getDepartment().getName() : null)
                        .build())
                .collect(Collectors.toList());

        return CompetencyFrameworkResponse.builder()
                .id(f.getId())
                .code(f.getCode())
                .name(f.getName())
                .description(f.getDescription())
                .active(f.getActive())
                .totalWeight(Math.round(totalWeight * 10.0) / 10.0)
                .criteriaCount(criteriaDtos.size())
                .criteria(criteriaDtos)
                .positions(positionDtos)
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }

    private EvaluationTemplateResponse buildTemplateResponse(JobPosition position, CompetencyFramework framework, Interview interview) {
        List<CompetencyCriterionDto> criteriaDtos = framework.getCriteria().stream()
                .map(c -> {
                    List<InterviewQuestionResponse> qDtos = questionRepository.findByCriterionId(c.getId()).stream()
                            .filter(q -> Boolean.TRUE.equals(q.getActive()))
                            .map(q -> InterviewQuestionResponse.builder()
                                    .id(q.getId())
                                    .criterionId(c.getId())
                                    .criterionName(c.getName())
                                    .content(q.getContent())
                                    .difficulty(q.getDifficulty())
                                    .difficultyLabel("EASY".equalsIgnoreCase(q.getDifficulty()) ? "Dễ" : ("HARD".equalsIgnoreCase(q.getDifficulty()) ? "Khó" : "Trung bình"))
                                    .goodAnswerGuideline(q.getGoodAnswerGuideline())
                                    .build())
                            .collect(Collectors.toList());

                    return CompetencyCriterionDto.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .description(c.getDescription())
                            .weight(c.getWeight())
                            .scaleMax(c.getScaleMax())
                            .orderIndex(c.getOrderIndex())
                            .questionCount(qDtos.size())
                            .questions(qDtos)
                            .build();
                })
                .collect(Collectors.toList());

        double totalWeight = criteriaDtos.stream().mapToDouble(c -> c.getWeight() != null ? c.getWeight() : 0.0).sum();

        EvaluationTemplateResponse.EvaluationTemplateResponseBuilder builder = EvaluationTemplateResponse.builder()
                .frameworkId(framework.getId())
                .frameworkCode(framework.getCode())
                .frameworkName(framework.getName())
                .frameworkDescription(framework.getDescription())
                .criteria(criteriaDtos)
                .totalWeight(totalWeight)
                .instructions("Người phỏng vấn chấm điểm từng tiêu chí từ 1 đến 10. Hệ thống sẽ tự động tính điểm tổng kết dựa trên trọng số của từng tiêu chí (Tổng trọng số = 100%).");

        if (position != null) {
            builder.positionId(position.getId())
                    .positionName(position.getName())
                    .positionLevel(position.getLevel())
                    .departmentName(position.getDepartment() != null ? position.getDepartment().getName() : null);
        }

        if (interview != null) {
            builder.interviewId(interview.getId())
                    .scheduledAt(interview.getScheduledAt())
                    .interviewerName(interview.getInterviewer() != null ? interview.getInterviewer().getUsername() : null);

            Application app = interview.getApplication();
            if (app != null) {
                if (app.getCandidateProfile() != null) {
                    builder.candidateName(app.getCandidateProfile().getFullName());
                    if (app.getCandidateProfile().getUser() != null) {
                        builder.candidateEmail(app.getCandidateProfile().getUser().getEmail());
                    }
                }
                if (app.getJobRequest() != null) {
                    builder.jobRequestId(app.getJobRequest().getId())
                            .jobRequestTitle(app.getJobRequest().getTitle());
                }
            }
        }

        return builder.build();
    }

    private EvaluationDetailResponse toEvaluationDetailResponse(Evaluation evaluation, List<EvaluationCriterionScore> details) {
        Interview interview = evaluation.getInterview();
        String candidateName = null;
        String interviewerName = null;
        String positionName = null;

        if (interview != null) {
            interviewerName = interview.getInterviewer() != null ? interview.getInterviewer().getUsername() : null;
            if (interview.getApplication() != null) {
                Application app = interview.getApplication();
                if (app.getCandidateProfile() != null) {
                    candidateName = app.getCandidateProfile().getFullName();
                }
                if (app.getJobRequest() != null && app.getJobRequest().getPosition() != null) {
                    positionName = app.getJobRequest().getPosition().getName();
                }
            }
        }

        List<EvaluationDetailResponse.CriterionScoreDetailDto> scoreDtos = details.stream()
                .map(d -> EvaluationDetailResponse.CriterionScoreDetailDto.builder()
                        .id(d.getId())
                        .criterionId(d.getCriterion() != null ? d.getCriterion().getId() : null)
                        .criterionName(d.getCriterionName())
                        .weight(d.getWeight())
                        .score(d.getScore())
                        .comment(d.getComment())
                        .build())
                .collect(Collectors.toList());

        return EvaluationDetailResponse.builder()
                .id(evaluation.getId())
                .interviewId(interview != null ? interview.getId() : null)
                .candidateName(candidateName)
                .interviewerName(interviewerName)
                .positionName(positionName)
                .frameworkName(evaluation.getFramework() != null ? evaluation.getFramework().getName() : null)
                .score(evaluation.getScore())
                .weightedScore(evaluation.getWeightedScore())
                .feedback(evaluation.getFeedback())
                .recommendedAction(evaluation.getRecommendedAction())
                .evaluatedAt(evaluation.getEvaluatedAt())
                .criterionScores(scoreDtos)
                .build();
    }
}
