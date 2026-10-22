package com.example.auth.service.impl;

import com.example.auth.dto.ApplicationDto;
import com.example.auth.dto.ApplicationNoteDto;
import com.example.auth.dto.CandidateDetailDto;
import com.example.auth.dto.CandidateHistoryDto;
import com.example.auth.entity.*;
import com.example.auth.exception.AppException;
import com.example.auth.repository.*;
import com.example.auth.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRequestRepository jobRequestRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final ApplicationNoteRepository applicationNoteRepository;

    @Override
    public ApplicationDto applyForJob(Long jobRequestId, String username, String cvUrl) {
        JobRequest job = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy Vị trí tuyển dụng với ID này"));
        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy thông tin ứng viên"));
        
        CandidateProfile profile = candidateProfileRepository.findByUserId(candidate.getId())
                .orElseGet(() -> {
                    CandidateProfile newProfile = new CandidateProfile();
                    newProfile.setUser(candidate);
                    newProfile.setFullName(candidate.getUsername());
                    return candidateProfileRepository.save(newProfile);
                });
        
        if (cvUrl != null) {
            profile.setCvUrl(cvUrl);
            candidateProfileRepository.save(profile);
        }

        Application app = new Application();
        app.setJobRequest(job);
        app.setCandidateProfile(profile);
        app.setStatus("APPLIED"); // APPLIED, SCREENING, INTERVIEWING, OFFERED, REJECTED
        app.setAppliedAt(LocalDateTime.now());
        
        Application saved = applicationRepository.save(app);
        return mapToDto(saved);
    }

    @Override
    public List<ApplicationDto> getApplicationsByCandidate(String username) {
        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy thông tin ứng viên"));
        return applicationRepository.findAll().stream()
                .filter(a -> a.getCandidateProfile() != null && a.getCandidateProfile().getUser().getId().equals(candidate.getId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getApplicationsByJobRequest(Long jobRequestId) {
        return applicationRepository.findAll().stream()
                .filter(a -> a.getJobRequest().getId().equals(jobRequestId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ApplicationDto updateStatus(Long id, String status) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new com.example.auth.exception.AppException("Không tìm thấy hồ sơ ứng tuyển"));
        app.setStatus(status);
        Application saved = applicationRepository.save(app);
        return mapToDto(saved);
    }

    private ApplicationDto mapToDto(Application a) {
        String name = a.getCandidateName();
        if (name == null && a.getCandidateProfile() != null && a.getCandidateProfile().getUser() != null) {
            name = a.getCandidateProfile().getUser().getFullName() != null ? a.getCandidateProfile().getUser().getFullName() : a.getCandidateProfile().getUser().getUsername();
        }

        return ApplicationDto.builder()
                .id(a.getId())
                .candidateId(a.getCandidateProfile() != null && a.getCandidateProfile().getUser() != null ? a.getCandidateProfile().getUser().getId() : null)
                .candidateName(name != null ? name : "Ứng viên")
                .candidateEmail(a.getCandidateEmail() != null ? a.getCandidateEmail() : (a.getCandidateProfile() != null && a.getCandidateProfile().getUser() != null ? a.getCandidateProfile().getUser().getEmail() : null))
                .candidatePhone(a.getCandidatePhone() != null ? a.getCandidatePhone() : (a.getCandidateProfile() != null ? a.getCandidateProfile().getPhone() : null))
                .jobRequestId(a.getJobRequest() != null ? a.getJobRequest().getId() : null)
                .jobTitle(a.getJobRequest() != null ? a.getJobRequest().getTitle() : null)
                .status(a.getStatus())
                .appliedAt(a.getAppliedAt())
                .interviewScore("85/100") // Mock data cho phỏng vấn
                .cvUrl(a.getCvUrl() != null ? a.getCvUrl() : (a.getCandidateProfile() != null ? a.getCandidateProfile().getCvUrl() : null))
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

    // =========================================================================
    // KN-304, KN-306, KN-308: CHI TIẾT HỒ SƠ ỨNG VIÊN, LỊCH SỬ & PHÂN QUYỀN
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public CandidateDetailDto getCandidateDetail(Long applicationId, String currentUsername) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ ứng tuyển với ID: " + applicationId));

        // KN-308: Thiết lập phân quyền truy cập hồ sơ ứng viên theo vị trí phụ trách
        checkAccessPermission(app, currentUsername);

        // 1. Thông tin cơ bản
        String candidateName = app.getCandidateName();
        String candidateEmail = app.getCandidateEmail();
        String candidatePhone = app.getCandidatePhone();
        String address = "";
        String currentTitle = "";
        String cvUrl = app.getCvUrl();

        if (app.getCandidateProfile() != null) {
            CandidateProfile cp = app.getCandidateProfile();
            if (candidateName == null) candidateName = cp.getFullName();
            if (candidatePhone == null) candidatePhone = cp.getPhone();
            if (address.isEmpty() && cp.getAddress() != null) address = cp.getAddress();
            if (currentTitle.isEmpty() && cp.getCurrentTitle() != null) currentTitle = cp.getCurrentTitle();
            if (cvUrl == null) cvUrl = cp.getCvUrl();
            if (candidateEmail == null && cp.getUser() != null) candidateEmail = cp.getUser().getEmail();
        }

        // 2. KN-307: Danh sách nhãn (Tags)
        List<String> tagList = parseTags(app.getTags());

        // 3. KN-307: Danh sách ghi chú (Notes)
        List<ApplicationNoteDto> noteDtos = applicationNoteRepository.findByApplicationIdOrderByCreatedAtDesc(app.getId())
                .stream()
                .map(this::mapNoteToDto)
                .collect(Collectors.toList());

        // 4. KN-306: Hiển thị đầy đủ các lần ứng tuyển trước đây của cùng người này
        List<Application> allCandidateApps = new ArrayList<>();
        if (candidateEmail != null && !candidateEmail.trim().isEmpty()) {
            allCandidateApps.addAll(applicationRepository.findByCandidateEmailIgnoreCase(candidateEmail.trim()));
        } else if (app.getCandidateProfile() != null) {
            allCandidateApps.addAll(applicationRepository.findByCandidateProfileId(app.getCandidateProfile().getId()));
        }

        List<CandidateHistoryDto> historyList = allCandidateApps.stream()
                .sorted((a1, a2) -> {
                    LocalDateTime t1 = a1.getAppliedAt() != null ? a1.getAppliedAt() : LocalDateTime.MIN;
                    LocalDateTime t2 = a2.getAppliedAt() != null ? a2.getAppliedAt() : LocalDateTime.MIN;
                    return t2.compareTo(t1);
                })
                .map(h -> CandidateHistoryDto.builder()
                        .applicationId(h.getId())
                        .jobRequestId(h.getJobRequest() != null ? h.getJobRequest().getId() : null)
                        .jobTitle(h.getJobRequest() != null ? h.getJobRequest().getTitle() : "Vị trí không xác định")
                        .departmentName(h.getJobRequest() != null && h.getJobRequest().getDepartment() != null ? h.getJobRequest().getDepartment().getName() : "")
                        .appliedAt(h.getAppliedAt())
                        .status(h.getStatus())
                        .generalStatus(mapGeneralStatus(h.getStatus()))
                        .cvUrl(h.getCvUrl())
                        .rejectionNote(h.getRejectionNote())
                        .withdrawalReason(h.getWithdrawalReason())
                        .build())
                .collect(Collectors.toList());

        JobRequest job = app.getJobRequest();
        String recruiterUser = job != null && job.getRecruiter() != null ? job.getRecruiter().getUsername() : null;
        String recruiterName = job != null && job.getRecruiter() != null ? (job.getRecruiter().getFullName() != null ? job.getRecruiter().getFullName() : job.getRecruiter().getUsername()) : null;
        String recruiterEmail = job != null && job.getRecruiter() != null ? job.getRecruiter().getEmail() : null;

        String hmUser = job != null && job.getHiringManager() != null ? job.getHiringManager().getUsername() : null;
        String hmName = job != null && job.getHiringManager() != null ? (job.getHiringManager().getFullName() != null ? job.getHiringManager().getFullName() : job.getHiringManager().getUsername()) : null;
        String hmEmail = job != null && job.getHiringManager() != null ? job.getHiringManager().getEmail() : null;

        return CandidateDetailDto.builder()
                .id(app.getId())
                .candidateName(candidateName != null ? candidateName : "Ứng viên")
                .candidateEmail(candidateEmail)
                .candidatePhone(candidatePhone)
                .address(address)
                .currentTitle(currentTitle)
                .coverLetter(app.getCoverLetter())
                .cvUrl(cvUrl)
                .cvOriginalFilename(app.getCvOriginalFilename())
                .trackingCode(app.getTrackingCode())
                .status(app.getStatus())
                .statusDisplayName(mapGeneralStatus(app.getStatus()))
                .appliedAt(app.getAppliedAt())
                .sourceId(app.getSource() != null ? app.getSource().getId() : null)
                .sourceName(app.getSource() != null ? app.getSource().getName() : null)
                .jobRequestId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .departmentId(job != null && job.getDepartment() != null ? job.getDepartment().getId() : null)
                .departmentName(job != null && job.getDepartment() != null ? job.getDepartment().getName() : null)
                .recruiterUsername(recruiterUser)
                .recruiterFullName(recruiterName)
                .recruiterEmail(recruiterEmail)
                .hiringManagerUsername(hmUser)
                .hiringManagerFullName(hmName)
                .hiringManagerEmail(hmEmail)
                .tags(tagList)
                .notes(noteDtos)
                .history(historyList)
                .parsedFullName(app.getParsedFullName())
                .parsedEmail(app.getParsedEmail())
                .parsedPhone(app.getParsedPhone())
                .cvParsingStatus(app.getCvParsingStatus())
                .cvParsingNote(app.getCvParsingNote())
                .parsedDataConfirmed(app.getParsedDataConfirmed())
                .withdrawalReason(app.getWithdrawalReason())
                .withdrawnAt(app.getWithdrawnAt())
                .canEditNotes(true)
                .canUpdateStatus(true)
                .build();
    }

    // =========================================================================
    // KN-307: QUẢN LÝ GHI CHÚ (NOTES)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationNoteDto> getApplicationNotes(Long applicationId, String currentUsername) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ"));
        checkAccessPermission(app, currentUsername);

        return applicationNoteRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId).stream()
                .map(this::mapNoteToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApplicationNoteDto addApplicationNote(Long applicationId, String content, String currentUsername) {
        if (content == null || content.trim().isEmpty()) {
            throw new AppException("Nội dung ghi chú không được để trống!");
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ ứng tuyển với ID: " + applicationId));
        checkAccessPermission(app, currentUsername);

        User author = userRepository.findByUsername(currentUsername).orElse(null);
        String authorFullName = (author != null && author.getFullName() != null && !author.getFullName().trim().isEmpty())
                ? author.getFullName() : currentUsername;

        ApplicationNote note = ApplicationNote.builder()
                .application(app)
                .content(content.trim())
                .authorUsername(currentUsername)
                .authorFullName(authorFullName)
                .createdAt(LocalDateTime.now())
                .build();

        ApplicationNote saved = applicationNoteRepository.save(note);
        log.info("Người dùng '{}' vừa thêm ghi chú vào hồ sơ ID={}", currentUsername, applicationId);
        return mapNoteToDto(saved);
    }

    @Override
    @Transactional
    public void deleteApplicationNote(Long noteId, String currentUsername) {
        ApplicationNote note = applicationNoteRepository.findById(noteId)
                .orElseThrow(() -> new AppException("Không tìm thấy ghi chú"));

        checkAccessPermission(note.getApplication(), currentUsername);

        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRoles() != null &&
                currentUser.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r) || "ROLE_HR_MANAGER".equalsIgnoreCase(r));

        if (!isAdmin && !currentUsername.equalsIgnoreCase(note.getAuthorUsername())) {
            throw new AppException("Bạn chỉ có thể xoá ghi chú do chính mình tạo ra!");
        }

        applicationNoteRepository.delete(note);
    }

    // =========================================================================
    // KN-307: QUẢN LÝ NHÃN (TAGS)
    // =========================================================================

    @Override
    @Transactional
    public CandidateDetailDto updateApplicationTags(Long applicationId, List<String> tags, String currentUsername) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ"));
        checkAccessPermission(app, currentUsername);

        if (tags == null || tags.isEmpty()) {
            app.setTags(null);
        } else {
            String joined = tags.stream()
                    .filter(t -> t != null && !t.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.joining(", "));
            app.setTags(joined);
        }
        app.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(app);

        return getCandidateDetail(applicationId, currentUsername);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * KN-308: Kiểm tra phân quyền truy cập hồ sơ ứng viên
     * Chỉ Recruiter phụ trách và Hiring Manager của vị trí đó mới được mở.
     * Admin và HR_MANAGER có toàn quyền truy cập.
     */
    private void checkAccessPermission(Application app, String username) {
        if (username == null) {
            throw new AppException("Vui lòng đăng nhập để truy cập thông tin hồ sơ ứng viên.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Tài khoản người dùng không tồn tại"));

        // Admin & HR Manager được phép xem toàn bộ hồ sơ
        boolean isAdminOrHr = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r) || "ROLE_HR_MANAGER".equalsIgnoreCase(r));
        if (isAdminOrHr) {
            return;
        }

        JobRequest job = app.getJobRequest();
        if (job == null) {
            return;
        }

        boolean isAssignedRecruiter = job.getRecruiter() != null && username.equalsIgnoreCase(job.getRecruiter().getUsername());
        boolean isAssignedHiringManager = job.getHiringManager() != null && username.equalsIgnoreCase(job.getHiringManager().getUsername());

        if (!isAssignedRecruiter && !isAssignedHiringManager) {
            throw new AppException("Từ chối truy cập (403): Bạn không có quyền truy cập hồ sơ ứng viên này! "
                    + "Chỉ Chuyên viên Tuyển dụng và Trưởng bộ phận phụ trách vị trí '" + job.getTitle() + "' mới được phép mở (KN-308).");
        }
    }

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private ApplicationNoteDto mapNoteToDto(ApplicationNote note) {
        return ApplicationNoteDto.builder()
                .id(note.getId())
                .applicationId(note.getApplication() != null ? note.getApplication().getId() : null)
                .content(note.getContent())
                .authorUsername(note.getAuthorUsername())
                .authorFullName(note.getAuthorFullName())
                .createdAt(note.getCreatedAt())
                .build();
    }

    private String mapGeneralStatus(String rawStatus) {
        if (rawStatus == null) return "Chờ xử lý";
        switch (rawStatus.toUpperCase(Locale.ROOT)) {
            case "WITHDRAWN":
            case "CANDIDATE_WITHDRAWN":
                return "Ứng viên rút";
            case "APPLIED":
            case "NEW":
                return "Mới nộp (Applied)";
            case "SCREENING":
            case "REVIEWING":
                return "Sơ tuyển (Screening)";
            case "INTERVIEWING":
            case "INTERVIEW_SCHEDULED":
                return "Phỏng vấn (Interviewing)";
            case "OFFERED":
            case "OFFERING":
                return "Đề xuất (Offering)";
            case "HIRED":
                return "Trúng tuyển (Hired)";
            case "REJECTED":
                return "Từ chối (Rejected)";
            case "CLOSED":
                return "Đã đóng";
            default:
                return rawStatus;
        }
    }
}
