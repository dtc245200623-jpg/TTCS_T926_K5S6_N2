package com.example.auth;

import com.example.auth.dto.*;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.CompetencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CompetencyServiceTest {

    @Autowired
    private CompetencyService competencyService;

    @Autowired
    private CompetencyFrameworkRepository frameworkRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRequestRepository jobRequestRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Mock HR_MANAGER authentication
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr_manager", null,
                        List.of(new SimpleGrantedAuthority("ROLE_HR_MANAGER")))
        );
    }

    @Test
    @DisplayName("KN-182 & KN-183: Tạo khung năng lực hợp lệ khi tổng trọng số đúng 100%")
    void testCreateFramework_Success_TotalWeight100Percent() {
        String uniqueCode = "KNL_TEST_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CompetencyFrameworkRequest req = CompetencyFrameworkRequest.builder()
                .code(uniqueCode)
                .name("Khung năng lực kiểm thử phần mềm (QA/QC)")
                .description("Dùng cho QA và QC Tester")
                .active(true)
                .criteria(List.of(
                        CompetencyCriterionDto.builder().name("Kỹ năng thiết kế Test Case").weight(40.0).scaleMax(10).orderIndex(1).build(),
                        CompetencyCriterionDto.builder().name("Kỹ năng kiểm thử tự động (Automation)").weight(35.0).scaleMax(10).orderIndex(2).build(),
                        CompetencyCriterionDto.builder().name("Kỹ năng báo cáo bug & Teamwork").weight(25.0).scaleMax(10).orderIndex(3).build()
                ))
                .build();

        CompetencyFrameworkResponse response = competencyService.createFramework(req);

        assertNotNull(response.getId());
        assertEquals(uniqueCode, response.getCode());
        assertEquals(3, response.getCriteriaCount());
        assertEquals(100.0, response.getTotalWeight());
    }

    @Test
    @DisplayName("KN-183: Từ chối tạo khung năng lực khi tổng trọng số khác 100% (Ví dụ 85% hoặc 110%)")
    void testCreateFramework_Fail_TotalWeightNot100Percent() {
        String uniqueCode = "KNL_INVALID_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Case 1: Tổng trọng số = 85% (thiếu 15%)
        CompetencyFrameworkRequest reqUnder = CompetencyFrameworkRequest.builder()
                .code(uniqueCode)
                .name("Khung năng lực không hợp lệ - thiếu trọng số")
                .criteria(List.of(
                        CompetencyCriterionDto.builder().name("Tiêu chí 1").weight(50.0).build(),
                        CompetencyCriterionDto.builder().name("Tiêu chí 2").weight(35.0).build()
                ))
                .build();

        AppException exUnder = assertThrows(AppException.class, () -> competencyService.createFramework(reqUnder));
        assertTrue(exUnder.getMessage().contains("Tổng trọng số của khung năng lực phải bằng 100%"));

        // Case 2: Tổng trọng số = 115% (dư 15%)
        CompetencyFrameworkRequest reqOver = CompetencyFrameworkRequest.builder()
                .code(uniqueCode + "_2")
                .name("Khung năng lực không hợp lệ - thừa trọng số")
                .criteria(List.of(
                        CompetencyCriterionDto.builder().name("Tiêu chí 1").weight(60.0).build(),
                        CompetencyCriterionDto.builder().name("Tiêu chí 2").weight(55.0).build()
                ))
                .build();

        AppException exOver = assertThrows(AppException.class, () -> competencyService.createFramework(reqOver));
        assertTrue(exOver.getMessage().contains("Tổng trọng số của khung năng lực phải bằng 100%"));
    }

    @Test
    @DisplayName("KN-182 & KN-184: Khung năng lực dùng chung cho nhiều chức danh (Reusability)")
    void testFrameworkReuse_MultiplePositions() {
        String uniqueCode = "KNL_MULTI_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CompetencyFrameworkRequest req = CompetencyFrameworkRequest.builder()
                .code(uniqueCode)
                .name("Khung năng lực Kỹ thuật đa chức danh")
                .criteria(List.of(
                        CompetencyCriterionDto.builder().name("Năng lực chuyên môn").weight(50.0).build(),
                        CompetencyCriterionDto.builder().name("Kỹ năng giải quyết vấn đề").weight(50.0).build()
                ))
                .build();

        CompetencyFrameworkResponse fw = competencyService.createFramework(req);

        // Tạo 2 chức danh khác nhau
        JobPosition pos1 = jobPositionRepository.save(JobPosition.builder()
                .code("POS1_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .name("Lập trình viên ReactJS")
                .level("Junior")
                .build());

        JobPosition pos2 = jobPositionRepository.save(JobPosition.builder()
                .code("POS2_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .name("Lập trình viên NodeJS")
                .level("Middle")
                .build());

        // Ánh xạ cả 2 chức danh vào cùng 1 khung năng lực
        CompetencyFrameworkResponse updatedFw = competencyService.assignPositions(fw.getId(), List.of(pos1.getId(), pos2.getId()));

        assertNotNull(updatedFw);
        assertEquals(2, updatedFw.getPositions().size());

        // Kiểm tra trực tiếp trên thực thể chức danh
        JobPosition reloadedPos1 = jobPositionRepository.findById(pos1.getId()).orElseThrow();
        JobPosition reloadedPos2 = jobPositionRepository.findById(pos2.getId()).orElseThrow();

        assertEquals(fw.getId(), reloadedPos1.getCompetencyFramework().getId());
        assertEquals(fw.getId(), reloadedPos2.getCompetencyFramework().getId());
    }

    @Test
    @DisplayName("KN-184: Sinh phiếu đánh giá phỏng vấn và tính điểm trọng số chính xác")
    void testGenerateEvaluationTemplate_And_SubmitEvaluation() {
        // 1. Tạo Khung năng lực với 2 tiêu chí: Chuyên môn (60%) và Thái độ (40%)
        String uniqueCode = "KNL_EVAL_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CompetencyFrameworkRequest fwReq = CompetencyFrameworkRequest.builder()
                .code(uniqueCode)
                .name("Khung đánh giá Tuyển dụng Dev")
                .criteria(List.of(
                        CompetencyCriterionDto.builder().name("Chuyên môn kỹ thuật").weight(60.0).scaleMax(10).orderIndex(1).build(),
                        CompetencyCriterionDto.builder().name("Thái độ & Giao tiếp").weight(40.0).scaleMax(10).orderIndex(2).build()
                ))
                .build();
        CompetencyFrameworkResponse fw = competencyService.createFramework(fwReq);

        // 2. Tạo chức danh gán với khung năng lực
        JobPosition pos = jobPositionRepository.save(JobPosition.builder()
                .code("POS_EVAL_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .name("Senior Java Engineer")
                .level("Senior")
                .competencyFramework(frameworkRepository.findById(fw.getId()).orElseThrow())
                .build());

        // 3. Kiểm tra sinh template theo chức danh
        EvaluationTemplateResponse posTemplate = competencyService.generateEvaluationTemplateForPosition(pos.getId());
        assertEquals("Senior Java Engineer", posTemplate.getPositionName());
        assertEquals(2, posTemplate.getCriteria().size());
        assertEquals(100.0, posTemplate.getTotalWeight());

        // 4. Tạo User Interviewer, Ứng viên, Yêu cầu tuyển dụng, Hồ sơ, Buổi phỏng vấn
        User interviewer = userRepository.save(User.builder()
                .username("interviewer_" + UUID.randomUUID().toString().substring(0, 5))
                .password("123456")
                .email("interviewer@test.com")
                .fullName("Người Phỏng Vấn")
                .build());

        JobRequest jobRequest = jobRequestRepository.save(JobRequest.builder()
                .title("Tuyển Senior Java Backend")
                .position(pos)
                .build());

        CandidateProfile candidate = candidateProfileRepository.save(CandidateProfile.builder()
                .fullName("Nguyễn Văn Ứng Viên")
                .phone("0987654321")
                .build());

        Application app = applicationRepository.save(Application.builder()
                .candidateProfile(candidate)
                .jobRequest(jobRequest)
                .status("INTERVIEWING")
                .build());

        Interview interview = interviewRepository.save(Interview.builder()
                .application(app)
                .interviewer(interviewer)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status("SCHEDULED")
                .build());

        // 5. Sinh phiếu đánh giá phỏng vấn từ buổi phỏng vấn (KN-184)
        EvaluationTemplateResponse interviewTemplate = competencyService.generateEvaluationTemplateForInterview(interview.getId());
        assertEquals(interview.getId(), interviewTemplate.getInterviewId());
        assertEquals("Nguyễn Văn Ứng Viên", interviewTemplate.getCandidateName());
        assertEquals("Senior Java Engineer", interviewTemplate.getPositionName());
        assertEquals(2, interviewTemplate.getCriteria().size());

        // 6. Chấm điểm và nộp phiếu đánh giá:
        // Tiêu chí 1 (Chuyên môn - 60%): Chấm 9.0 điểm -> Đóng góp: 9.0 * 0.6 = 5.4
        // Tiêu chí 2 (Thái độ - 40%): Chấm 8.0 điểm -> Đóng góp: 8.0 * 0.4 = 3.2
        // Tổng điểm có trọng số: 5.4 + 3.2 = 8.60 / 10
        CompetencyCriterionDto crit1 = interviewTemplate.getCriteria().get(0);
        CompetencyCriterionDto crit2 = interviewTemplate.getCriteria().get(1);

        EvaluationSubmitRequest submitReq = EvaluationSubmitRequest.builder()
                .interviewId(interview.getId())
                .frameworkId(fw.getId())
                .feedback("Ứng viên có kỹ năng thuật toán rất tốt, giao tiếp lưu loát.")
                .recommendedAction("HIRE")
                .criteriaScores(List.of(
                        EvaluationSubmitRequest.CriterionScoreInputDto.builder()
                                .criterionId(crit1.getId())
                                .criterionName(crit1.getName())
                                .weight(crit1.getWeight())
                                .score(9.0)
                                .comment("Trả lời xuất sắc các câu hỏi về Java Concurrency và Microservices.")
                                .build(),
                        EvaluationSubmitRequest.CriterionScoreInputDto.builder()
                                .criterionId(crit2.getId())
                                .criterionName(crit2.getName())
                                .weight(crit2.getWeight())
                                .score(8.0)
                                .comment("Thái độ tích cực, hòa đồng, phù hợp văn hóa.")
                                .build()
                ))
                .build();

        EvaluationDetailResponse evalResult = competencyService.submitEvaluation(submitReq, interviewer.getUsername());

        assertNotNull(evalResult.getId());
        assertEquals(interview.getId(), evalResult.getInterviewId());
        assertEquals("HIRE", evalResult.getRecommendedAction());
        assertEquals(8.6, evalResult.getWeightedScore(), 0.01, "Điểm trọng số phải là 8.6");
        assertEquals(9, evalResult.getScore(), "Điểm làm tròn thang 10 là 9");

        // Kiểm tra trạng thái buổi phỏng vấn đã được cập nhật thành COMPLETED
        Interview updatedInterview = interviewRepository.findById(interview.getId()).orElseThrow();
        assertEquals("COMPLETED", updatedInterview.getStatus());

        // Kiểm tra lấy lại phiếu đã chấm
        EvaluationDetailResponse fetchedEval = competencyService.getEvaluationByInterviewId(interview.getId());
        assertEquals(8.6, fetchedEval.getWeightedScore(), 0.01);
        assertEquals(2, fetchedEval.getCriterionScores().size());
    }
}
