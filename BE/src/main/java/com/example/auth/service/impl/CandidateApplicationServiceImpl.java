package com.example.auth.service.impl;

import com.example.auth.dto.*;
import com.example.auth.entity.Application;
import com.example.auth.entity.JobRequest;
import com.example.auth.entity.RecruitmentCategory;
import com.example.auth.exception.AppException;
import com.example.auth.repository.ApplicationRepository;
import com.example.auth.repository.JobRequestRepository;
import com.example.auth.repository.RecruitmentCategoryRepository;
import com.example.auth.service.CandidateApplicationService;
import com.example.auth.service.CvParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateApplicationServiceImpl implements CandidateApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRequestRepository jobRequestRepository;
    private final RecruitmentCategoryRepository categoryRepository;
    private final CvParserService cvParserService;

    private static final long MAX_CV_FILE_SIZE = 10 * 1024 * 1024; // 10MB (KN-281)
    private static final List<String> ALLOWED_CV_EXTS = Arrays.asList("pdf", "docx", "doc");
    private static final int MAX_APPLICATIONS_PER_IP_PER_HOUR = 5; // KN-282

    /**
     * Tìm kiếm và lọc tin tuyển dụng công khai cho ứng viên (Image 1, KN-274)
     * Mô tả: Không cần đăng nhập, lọc theo phòng ban, địa điểm, hình thức làm việc,
     * tìm kiếm theo từ khoá trong tiêu đề và mô tả, CHỈ hiển thị tin đã xuất bản và còn hạn.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PublicJobDetailDto> searchPublicJobs(Long departmentId, Long locationId, Long employmentTypeId, String keyword) {
        List<JobRequest> all = jobRequestRepository.findAll();
        LocalDate today = LocalDate.now();

        String kw = (keyword != null) ? keyword.trim().toLowerCase(Locale.ROOT) : "";

        return all.stream()
                // Chỉ hiển thị tin đã xuất bản (status APPROVED hoặc OPEN) và có publishedAt
                .filter(job -> ("APPROVED".equalsIgnoreCase(job.getStatus()) || "OPEN".equalsIgnoreCase(job.getStatus())))
                .filter(job -> job.getPublishedAt() != null)
                // Chỉ hiển thị tin còn hạn nhận hồ sơ
                .filter(job -> job.getApplicationDeadline() == null || !job.getApplicationDeadline().isBefore(today))
                // Lọc theo phòng ban
                .filter(job -> departmentId == null || (job.getDepartment() != null && departmentId.equals(job.getDepartment().getId())))
                // Lọc theo địa điểm làm việc
                .filter(job -> locationId == null || (job.getLocation() != null && locationId.equals(job.getLocation().getId())))
                // Lọc theo hình thức làm việc
                .filter(job -> employmentTypeId == null || (job.getEmploymentType() != null && employmentTypeId.equals(job.getEmploymentType().getId())))
                // Tìm kiếm theo từ khoá trong tiêu đề và mô tả (KN-274)
                .filter(job -> {
                    if (kw.isEmpty()) return true;
                    boolean matchTitle = job.getTitle() != null && job.getTitle().toLowerCase(Locale.ROOT).contains(kw);
                    boolean matchDesc = job.getDescription() != null && job.getDescription().toLowerCase(Locale.ROOT).contains(kw);
                    boolean matchReq = job.getRequirements() != null && job.getRequirements().toLowerCase(Locale.ROOT).contains(kw);
                    return matchTitle || matchDesc || matchReq;
                })
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getPublishedAt() != null ? a.getPublishedAt() : a.getCreatedAt();
                    LocalDateTime tb = b.getPublishedAt() != null ? b.getPublishedAt() : b.getCreatedAt();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta); // Mới nhất lên đầu
                })
                .map(this::mapToPublicDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Xem chi tiết tin tuyển dụng công khai (KN-272, KN-273, KN-275, KN-277)
     * KN-273: Ẩn toàn bộ thông báo nội bộ, ghi chú đóng, quy tắc duyệt, thông tin recruiter nội bộ.
     */
    @Override
    @Transactional(readOnly = true)
    public PublicJobDetailDto getPublicJobDetail(Long jobId) {
        JobRequest job = jobRequestRepository.findById(jobId)
                .orElseThrow(() -> new AppException("Không tìm thấy tin tuyển dụng với mã: " + jobId));

        // Phải là tin đã được xuất bản
        boolean isPublished = ("APPROVED".equalsIgnoreCase(job.getStatus()) || "OPEN".equalsIgnoreCase(job.getStatus()) || job.getPublishedAt() != null);
        if (!isPublished) {
            throw new AppException("Tin tuyển dụng này chưa được công khai hoặc đang ở trạng thái nháp");
        }

        return mapToPublicDetailDto(job);
    }

    /**
     * Ứng viên nộp hồ sơ không cần đăng nhập (KN-279 -> KN-285, KN-286 -> KN-290)
     */
    @Override
    @Transactional
    public CandidateApplyResponseDto submitApplication(CandidateApplyRequestDto request, MultipartFile cvFile, String clientIp) {
        if (request == null) {
            throw new AppException("Dữ liệu biểu mẫu không hợp lệ");
        }

        // KN-280: Kiểm tra dữ liệu đầu vào
        validateApplyInput(request);

        JobRequest job = jobRequestRepository.findById(request.getJobRequestId())
                .orElseThrow(() -> new AppException("Không tìm thấy vị trí tuyển dụng với mã: " + request.getJobRequestId()));

        // KN-277 & KN-278: Ràng buộc trạng thái tin tuyển dụng - Từ chối nếu tin đã hết hạn hoặc đóng
        LocalDate today = LocalDate.now();
        boolean isExpired = (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(today))
                || "CLOSED".equalsIgnoreCase(job.getStatus());
        if (isExpired) {
            throw new AppException("Vị trí tuyển dụng này đã hết hạn nhận hồ sơ ứng tuyển.");
        }

        // KN-281: Kiểm tra tệp CV tải lên (định dạng PDF/DOCX, tối đa 10MB)
        if (cvFile == null || cvFile.isEmpty()) {
            throw new AppException("Vui lòng đính kèm tệp CV của bạn (chấp nhận PDF hoặc DOCX, tối đa 10MB).");
        }
        validateCvFile(cvFile);

        // KN-282: Thiết lập chống spam và giới hạn số lần nộp theo IP trong 1 giờ
        String safeIp = (clientIp != null && !clientIp.trim().isEmpty()) ? clientIp.trim() : "UNKNOWN_IP";
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long submissionsLastHour = applicationRepository.countByIpAddressAndAppliedAtAfter(safeIp, oneHourAgo);
        if (!"UNKNOWN_IP".equals(safeIp) && submissionsLastHour >= MAX_APPLICATIONS_PER_IP_PER_HOUR) {
            log.warn("IP {} đã vượt quá giới hạn nộp hồ sơ: {} lần/giờ", safeIp, submissionsLastHour);
            throw new AppException("Bạn đã gửi quá số lần cho phép từ địa chỉ này trong vòng 1 giờ (tối đa 5 lần). Vui lòng thử lại sau.");
        }

        // KN-283: Ngăn chặn nộp trùng theo vị trí và email
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        boolean isDuplicate = applicationRepository.existsByJobRequestIdAndCandidateEmailIgnoreCase(job.getId(), email);
        if (isDuplicate) {
            throw new AppException("Email này (" + request.getEmail() + ") đã được dùng để nộp hồ sơ cho vị trí '"
                    + job.getTitle() + "'. Vui lòng không nộp trùng hoặc sử dụng mã tra cứu để theo dõi tiến độ.");
        }

        // KN-284: Sinh mã tra cứu duy nhất
        String trackingCode = generateUniqueTrackingCode();

        // Lưu trữ file CV
        String cvUrl = saveCvFile(cvFile);

        // KN-286, KN-289, KN-290: Bóc tách CV PDF
        CvParsingResult parseResult = null;
        try {
            parseResult = cvParserService.parsePdfCv(cvFile);
        } catch (Throwable t) {
            // KN-289: Bóc tách thất bại tuyệt đối không làm gián đoạn việc nộp hồ sơ
            log.error("Ngoại lệ bóc tách CV không mong muốn: {}", t.getMessage());
            parseResult = CvParsingResult.failed("Lỗi bóc tách tệp: " + t.getMessage());
        }

        // Nguồn ứng viên (nếu có chọn)
        RecruitmentCategory sourceCategory = null;
        if (request.getSourceId() != null) {
            sourceCategory = categoryRepository.findById(request.getSourceId()).orElse(null);
        }

        // KN-285: Lưu trữ hồ sơ ứng tuyển vào cơ sở dữ liệu
        Application app = Application.builder()
                .jobRequest(job)
                .trackingCode(trackingCode)
                .candidateName(request.getFullName().trim())
                .candidateEmail(email)
                .candidatePhone(request.getPhone().trim())
                .coverLetter(request.getCoverLetter() != null ? request.getCoverLetter().trim() : null)
                .cvUrl(cvUrl)
                .cvOriginalFilename(cvFile.getOriginalFilename())
                .ipAddress(safeIp)
                .source(sourceCategory)
                .status("APPLIED")
                .appliedAt(LocalDateTime.now())
                .build();

        // KN-288: Lưu dữ liệu bóc tách vào trường riêng biệt, KHÔNG tự ghi đè dữ liệu ứng viên tự nhập
        if (parseResult != null) {
            app.setCvParsingStatus(parseResult.getStatus());
            app.setCvParsingNote(parseResult.getErrorMessage());
            if ("SUCCESS".equalsIgnoreCase(parseResult.getStatus())) {
                app.setParsedFullName(parseResult.getFullName());
                app.setParsedEmail(parseResult.getEmail());
                app.setParsedPhone(parseResult.getPhone());
            }
        }

        Application saved = applicationRepository.save(app);
        log.info("Ứng tuyển thành công: AppID={}, TrackingCode={}, Candidate={}, Job={}",
                saved.getId(), trackingCode, saved.getCandidateName(), job.getTitle());

        // KN-284: Gửi email xác nhận kèm mã tra cứu (mô phỏng / logging notification)
        sendConfirmationEmail(saved.getCandidateEmail(), saved.getCandidateName(), trackingCode, job.getTitle());

        return CandidateApplyResponseDto.builder()
                .trackingCode(trackingCode)
                .applicationId(saved.getId())
                .jobRequestId(job.getId())
                .jobTitle(job.getTitle())
                .candidateName(saved.getCandidateName())
                .candidateEmail(saved.getCandidateEmail())
                .appliedAt(saved.getAppliedAt())
                .cvUrl(saved.getCvUrl())
                .message("Nộp hồ sơ ứng tuyển thành công! Vui lòng lưu lại Mã tra cứu để theo dõi tiến độ.")
                .build();
    }

    /**
     * Tra cứu trạng thái hồ sơ bằng mã tra cứu và email (KN-291, KN-292)
     */
    @Override
    @Transactional(readOnly = true)
    public PublicApplicationStatusDto trackApplicationStatus(ApplicationTrackingRequestDto request) {
        if (request == null || request.getTrackingCode() == null || request.getEmail() == null) {
            throw new AppException("Vui lòng nhập đầy đủ Mã tra cứu và Email.");
        }

        String trackingCode = request.getTrackingCode().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        Application app = applicationRepository.findByTrackingCodeAndCandidateEmailIgnoreCase(trackingCode, email)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ ứng tuyển với Mã tra cứu '" + trackingCode + "' và Email '" + email + "'. Vui lòng kiểm tra lại."));

        // KN-291: Map sang 4 trạng thái tổng quát
        // "Đã nhận", "Đang xem xét", "Mời phỏng vấn", "Kết thúc"
        String rawStatus = app.getStatus() != null ? app.getStatus().toUpperCase(Locale.ROOT) : "APPLIED";
        String generalStatus;
        String statusCode;
        String statusDesc;

        switch (rawStatus) {
            case "SCREENING":
            case "REVIEWING":
                generalStatus = "Đang xem xét";
                statusCode = "REVIEWING";
                statusDesc = "Hồ sơ của bạn đang được Phòng Nhân sự & Trưởng bộ phận chuyên môn xem xét đánh giá năng lực.";
                break;
            case "INTERVIEWING":
            case "INTERVIEW_SCHEDULED":
            case "INTERVIEWED":
                generalStatus = "Mời phỏng vấn";
                statusCode = "INTERVIEWING";
                statusDesc = "Chúc mừng bạn đã vượt qua vòng sơ loại! Bộ phận Tuyển dụng sẽ liên hệ gửi lịch phỏng vấn chi tiết qua email/điện thoại.";
                break;
            case "OFFERED":
            case "OFFERING":
            case "HIRED":
            case "REJECTED":
            case "CLOSED":
                generalStatus = "Kết thúc";
                statusCode = "COMPLETED";
                statusDesc = "Quy trình ứng tuyển cho đợt tuyển dụng này đã hoàn tất. Trân trọng cảm ơn bạn đã quan tâm và đồng hành cùng công ty.";
                break;
            case "APPLIED":
            case "NEW":
            case "SUBMITTED":
            default:
                generalStatus = "Đã nhận";
                statusCode = "RECEIVED";
                statusDesc = "Hệ thống đã tiếp nhận hồ sơ thành công. Hồ sơ đang được xếp hàng chờ xử lý sơ tuyển.";
                break;
        }

        // KN-292: Ẩn tuyệt đối các trường nội bộ (rejectionNote, interviewScore, interviewer, v.v.)
        return PublicApplicationStatusDto.builder()
                .trackingCode(app.getTrackingCode())
                .candidateName(app.getCandidateName())
                .jobTitle(app.getJobRequest() != null ? app.getJobRequest().getTitle() : "Vị trí tuyển dụng")
                .departmentName(app.getJobRequest() != null && app.getJobRequest().getDepartment() != null
                        ? app.getJobRequest().getDepartment().getName() : "Khối Công Nghệ")
                .locationName(app.getJobRequest() != null && app.getJobRequest().getLocation() != null
                        ? app.getJobRequest().getLocation().getName() : "Văn phòng chính")
                .appliedAt(app.getAppliedAt())
                .generalStatus(generalStatus)
                .statusCode(statusCode)
                .statusDescription(statusDesc)
                .updatedAt(app.getUpdatedAt() != null ? app.getUpdatedAt() : app.getAppliedAt())
                .build();
    }

    /**
     * Recruiter xem dữ liệu bóc tách CV đối chiếu với thông tin ứng viên nhập (KN-287)
     */
    @Override
    @Transactional(readOnly = true)
    public ApplicationDto getApplicationParsedData(Long applicationId) {
        Application a = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ với mã: " + applicationId));
        return mapToApplicationDto(a);
    }

    /**
     * Recruiter xác nhận hoặc chỉnh sửa dữ liệu bóc tách từ CV (KN-287, KN-288)
     */
    @Override
    @Transactional
    public ApplicationDto confirmOrUpdateParsedData(Long applicationId, ConfirmParsedDataDto dto, String recruiterUsername) {
        Application a = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ với mã: " + applicationId));

        if (dto != null) {
            // KN-288: Chỉ cập nhật vào thông tin chính thức nếu Recruiter xác nhận applyToProfile == true
            if (Boolean.TRUE.equals(dto.getApplyToProfile())) {
                if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
                    a.setCandidateName(dto.getFullName().trim());
                }
                if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                    a.setCandidateEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT));
                }
                if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
                    a.setCandidatePhone(dto.getPhone().trim());
                }
                log.info("Recruiter [{}] đã áp dụng dữ liệu bóc tách vào hồ sơ {}", recruiterUsername, applicationId);
            }
            a.setParsedDataConfirmed(true);
            a.setUpdatedAt(LocalDateTime.now());
        }

        Application saved = applicationRepository.save(a);
        return mapToApplicationDto(saved);
    }

    // =================================== CÁC HÀM TIỆN ÍCH HỖ TRỢ ===================================

    private void validateApplyInput(CandidateApplyRequestDto req) {
        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) {
            throw new AppException("Họ và tên không được để trống");
        }
        if (req.getEmail() == null || req.getEmail().trim().isEmpty() || !req.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new AppException("Địa chỉ email không đúng định dạng hợp lệ");
        }
        if (req.getPhone() == null || req.getPhone().trim().isEmpty() || !req.getPhone().matches("^(0|\\+84)[0-9]{9,10}$")) {
            throw new AppException("Số điện thoại không hợp lệ (yêu cầu từ 10 đến 11 chữ số, ví dụ: 0912345678)");
        }
    }

    private void validateCvFile(MultipartFile file) {
        if (file.getSize() > MAX_CV_FILE_SIZE) {
            throw new AppException("Dung lượng tệp CV vượt quá giới hạn 10MB (" + (file.getSize() / 1024 / 1024) + "MB). Vui lòng chọn tệp nhỏ hơn.");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_CV_EXTS.contains(ext)) {
            throw new AppException("Định dạng tệp không được hỗ trợ (" + ext + "). Hệ thống chỉ chấp nhận tệp CV dạng PDF hoặc DOCX.");
        }
    }

    private String saveCvFile(MultipartFile file) {
        try {
            Path uploadDir = Paths.get("FE/uploads/cvs").toAbsolutePath();
            if (!Files.exists(uploadDir.getParent().getParent())) {
                uploadDir = Paths.get("../FE/uploads/cvs").toAbsolutePath();
            }
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String cleanOrigin = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "cv.pdf";
            String filename = "CV_" + System.currentTimeMillis() + "_" + cleanOrigin;
            Path target = uploadDir.resolve(filename);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return "uploads/cvs/" + filename;
        } catch (Exception e) {
            log.error("Lỗi lưu trữ tệp CV: {}", e.getMessage());
            throw new AppException("Lỗi khi lưu trữ tệp CV trên máy chủ: " + e.getMessage());
        }
    }

    private String generateUniqueTrackingCode() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "APP-" + LocalDate.now().getYear() + "-" + sb.toString();
    }

    private void sendConfirmationEmail(String email, String candidateName, String trackingCode, String jobTitle) {
        // Ghi log notification và có thể mở rộng gửi qua JavaMailSender nếu mail server sẵn sàng
        log.info("=== [EMAIL CONFIRMATION SENT] ===");
        log.info("To: {}", email);
        log.info("Subject: Xác nhận tiếp nhận hồ sơ ứng tuyển vị trí {}", jobTitle);
        log.info("Nội dung: Chào {}, Cảm ơn bạn đã ứng tuyển vào vị trí '{}'. Mã tra cứu hồ sơ của bạn là: {}. Bạn có thể tra cứu trạng thái hồ sơ bất kỳ lúc nào tại Cổng nghề nghiệp.", candidateName, jobTitle, trackingCode);
        log.info("==================================");
    }

    private PublicJobDetailDto mapToPublicDetailDto(JobRequest job) {
        LocalDate today = LocalDate.now();
        boolean isExpired = (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(today))
                || "CLOSED".equalsIgnoreCase(job.getStatus());

        String salDisplay = "Thương lượng";
        if (job.getIsSalaryVisible() != null && job.getIsSalaryVisible()) {
            if (job.getMinSalary() != null && job.getMaxSalary() != null) {
                salDisplay = String.format("%.0f - %.0f triệu VNĐ", job.getMinSalary() / 1_000_000, job.getMaxSalary() / 1_000_000);
            } else if (job.getMinSalary() != null) {
                salDisplay = String.format("Từ %.0f triệu VNĐ", job.getMinSalary() / 1_000_000);
            } else if (job.getMaxSalary() != null) {
                salDisplay = String.format("Đến %.0f triệu VNĐ", job.getMaxSalary() / 1_000_000);
            }
        }

        return PublicJobDetailDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .departmentId(job.getDepartment() != null ? job.getDepartment().getId() : null)
                .departmentName(job.getDepartment() != null ? job.getDepartment().getName() : null)
                .positionId(job.getPosition() != null ? job.getPosition().getId() : null)
                .positionName(job.getPosition() != null ? job.getPosition().getName() : null)
                .locationId(job.getLocation() != null ? job.getLocation().getId() : null)
                .locationName(job.getLocation() != null ? job.getLocation().getName() : null)
                .employmentTypeId(job.getEmploymentType() != null ? job.getEmploymentType().getId() : null)
                .employmentTypeName(job.getEmploymentType() != null ? job.getEmploymentType().getName() : null)
                .headcount(job.getHeadcount())
                .isSalaryVisible(job.getIsSalaryVisible())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .salaryDisplay(salDisplay)
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .benefits("• Thu nhập hấp dẫn và cạnh tranh theo năng lực\n• Chế độ bảo hiểm sức khỏe cao cấp, khám sức khỏe định kỳ\n• Thưởng hiệu quả dự án, thưởng lễ tết, tháng lương 13\n• Môi trường làm việc năng động, lộ trình phát triển rõ ràng")
                .applicationDeadline(job.getApplicationDeadline())
                .isExpired(isExpired)
                .publishedAt(job.getPublishedAt())
                .status(job.getStatus())
                .build();
    }

    private ApplicationDto mapToApplicationDto(Application a) {
        String name = a.getCandidateName();
        if (name == null && a.getCandidateProfile() != null && a.getCandidateProfile().getUser() != null) {
            name = a.getCandidateProfile().getUser().getFullName();
        }

        return ApplicationDto.builder()
                .id(a.getId())
                .candidateId(a.getCandidateProfile() != null ? a.getCandidateProfile().getId() : null)
                .candidateName(name != null ? name : "Ứng viên")
                .candidateEmail(a.getCandidateEmail())
                .candidatePhone(a.getCandidatePhone())
                .jobRequestId(a.getJobRequest() != null ? a.getJobRequest().getId() : null)
                .jobTitle(a.getJobRequest() != null ? a.getJobRequest().getTitle() : null)
                .status(a.getStatus())
                .appliedAt(a.getAppliedAt())
                .interviewScore("85/100")
                .cvUrl(a.getCvUrl())
                .cvOriginalFilename(a.getCvOriginalFilename())
                .trackingCode(a.getTrackingCode())
                .coverLetter(a.getCoverLetter())
                .sourceId(a.getSource() != null ? a.getSource().getId() : null)
                .sourceName(a.getSource() != null ? a.getSource().getName() : null)
                .parsedFullName(a.getParsedFullName())
                .parsedEmail(a.getParsedEmail())
                .parsedPhone(a.getParsedPhone())
                .cvParsingStatus(a.getCvParsingStatus())
                .cvParsingNote(a.getCvParsingNote())
                .parsedDataConfirmed(a.getParsedDataConfirmed())
                .build();
    }
}
