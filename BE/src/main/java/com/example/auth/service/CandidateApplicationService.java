package com.example.auth.service;

import com.example.auth.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CandidateApplicationService {

    /**
     * Tìm kiếm và lọc tin tuyển dụng công khai cho ứng viên (Image 1, KN-274)
     * Chỉ trả về các tin đã xuất bản và còn hạn nhận hồ sơ
     */
    List<PublicJobDetailDto> searchPublicJobs(Long departmentId, Long locationId, Long employmentTypeId, String keyword);

    /**
     * Xem chi tiết tin tuyển dụng công khai (KN-272, KN-273, KN-275, KN-277)
     * Tuyệt đối ẩn thông tin nội bộ
     */
    PublicJobDetailDto getPublicJobDetail(Long jobId);

    /**
     * Ứng viên nộp hồ sơ không cần đăng nhập (KN-279 -> KN-285, KN-286 -> KN-290)
     */
    CandidateApplyResponseDto submitApplication(CandidateApplyRequestDto request, MultipartFile cvFile, String clientIp);

    /**
     * Tra cứu trạng thái hồ sơ bằng mã tra cứu và email (KN-291, KN-292)
     */
    PublicApplicationStatusDto trackApplicationStatus(ApplicationTrackingRequestDto request);

    /**
     * Recruiter xem dữ liệu bóc tách CV đối chiếu với thông tin ứng viên nhập (KN-287)
     */
    ApplicationDto getApplicationParsedData(Long applicationId);

    /**
     * Recruiter xác nhận hoặc chỉnh sửa dữ liệu bóc tách từ CV (KN-287, KN-288)
     */
    ApplicationDto confirmOrUpdateParsedData(Long applicationId, ConfirmParsedDataDto dto, String recruiterUsername);

    /**
     * KN-293, KN-294: Lấy thông tin ứng tuyển để hiển thị màn hình xác nhận trước khi rút hồ sơ
     */
    CandidateWithdrawInfoDto getWithdrawalInfo(String trackingCode, String email);

    /**
     * KN-293, KN-294, KN-295, KN-296: Thực hiện rút hồ sơ ứng tuyển
     */
    PublicApplicationStatusDto withdrawApplication(CandidateWithdrawRequestDto request);
}
