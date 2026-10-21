package com.example.auth.service.impl;

import com.example.auth.dto.CareerPortalConfigRequest;
import com.example.auth.dto.CareerPortalConfigResponse;
import com.example.auth.dto.JobRequestDto;
import com.example.auth.entity.CareerPortalConfig;
import com.example.auth.exception.AppException;
import com.example.auth.repository.CareerPortalConfigRepository;
import com.example.auth.service.CareerPortalService;
import com.example.auth.service.JobRequestService;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerPortalServiceImpl implements CareerPortalService {

    private final CareerPortalConfigRepository portalConfigRepository;
    private final JobRequestService jobRequestService;

    @Override
    @Transactional
    public CareerPortalConfigResponse getPortalConfig() {
        CareerPortalConfig config = portalConfigRepository.findTopByOrderByIdAsc()
                .orElseGet(this::initDefaultConfig);
        return mapToResponse(config);
    }

    private CareerPortalConfig initDefaultConfig() {
        CareerPortalConfig defaultConfig = CareerPortalConfig.builder()
                .companyName("Tập đoàn Công nghệ TTCS")
                .tagline("Đồng hành cùng nhân tài - Kiến tạo tương lai số")
                .introduction("Chào mừng bạn đến với Cổng tuyển dụng và cơ hội nghề nghiệp của TTCS Group. Chúng tôi luôn tìm kiếm những nhân sự tài năng, nhiệt huyết và khát khao bứt phá để cùng nhau xây dựng những giải pháp công nghệ hàng đầu.")
                .aboutCulture("Môi trường làm việc cởi mở, minh bạch, đề cao tinh thần đổi mới sáng tạo và phát triển bản thân. Từng thành viên đều được trao quyền và tạo điều kiện tối đa để tỏa sáng.")
                .benefits("• Thu nhập cạnh tranh theo năng lực, thưởng hiệu quả dự án hấp dẫn\n• Lộ trình thăng tiến rõ ràng, đánh giá năng lực định kỳ 2 lần/năm\n• Bảo hiểm sức khỏe cao cấp, khám sức khỏe tổng quát định kỳ\n• Tham gia các khóa đào tạo chuyên sâu và chứng chỉ quốc tế\n• Du lịch thường niên, teambuilding, hoạt động thể thao & văn hóa sôi nổi\n• Thiết bị làm việc hiện đại, môi trường làm việc Hybrid linh hoạt")
                .logoUrl("images/logo-company.png")
                .bannerUrl("images/banner-career.jpg")
                .email("recruitment@ttcs-group.vn")
                .phone("024.3888.9999")
                .address("Tòa nhà TTCS Tower, Khu Công nghệ cao, Hà Nội")
                .website("https://ttcs-group.vn")
                .workingHours("Thứ Hai - Thứ Sáu: 08:30 - 17:30")
                .isPublished(true)
                .updatedAt(LocalDateTime.now())
                .updatedBy("Hệ thống")
                .build();
        return portalConfigRepository.save(defaultConfig);
    }

    @Override
    @Transactional
    public CareerPortalConfigResponse updatePortalConfig(CareerPortalConfigRequest request, String updatedBy) {
        if (request == null) {
            throw new AppException("Dữ liệu cấu hình không hợp lệ");
        }
        if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
            throw new AppException("Tên công ty không được để trống");
        }
        if (request.getIntroduction() == null || request.getIntroduction().trim().isEmpty()) {
            throw new AppException("Nội dung giới thiệu công ty không được để trống");
        }

        CareerPortalConfig config = portalConfigRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> CareerPortalConfig.builder().build());

        config.setCompanyName(request.getCompanyName().trim());
        config.setTagline(request.getTagline() != null ? request.getTagline().trim() : null);
        config.setIntroduction(request.getIntroduction().trim());
        config.setAboutCulture(request.getAboutCulture());
        config.setBenefits(request.getBenefits());
        if (request.getLogoUrl() != null && !request.getLogoUrl().trim().isEmpty()) {
            config.setLogoUrl(request.getLogoUrl().trim());
        }
        if (request.getBannerUrl() != null && !request.getBannerUrl().trim().isEmpty()) {
            config.setBannerUrl(request.getBannerUrl().trim());
        }
        config.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        config.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        config.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        config.setWebsite(request.getWebsite() != null ? request.getWebsite().trim() : null);
        config.setWorkingHours(request.getWorkingHours() != null ? request.getWorkingHours().trim() : null);
        config.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : true);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(updatedBy != null ? updatedBy : "HR Manager");

        CareerPortalConfig saved = portalConfigRepository.save(config);
        log.info("Cập nhật thành công cấu hình trang việc làm bởi: {}", updatedBy);
        return mapToResponse(saved);
    }

    @Override
    public String uploadLogo(MultipartFile file) {
        validateFile(file, 5 * 1024 * 1024, "5MB", Arrays.asList("jpg", "jpeg", "png", "webp", "svg"));
        return saveMediaFile(file, "logo");
    }

    @Override
    public String uploadBanner(MultipartFile file) {
        validateFile(file, 10 * 1024 * 1024, "10MB", Arrays.asList("jpg", "jpeg", "png", "webp"));
        return saveMediaFile(file, "banner");
    }

    @Override
    public List<JobRequestDto> getPublicJobs() {
        List<JobRequestDto> all = jobRequestService.getAllJobRequests();
        if (all == null) return List.of();
        return all.stream()
                .filter(job -> "APPROVED".equalsIgnoreCase(job.getStatus()) || "OPEN".equalsIgnoreCase(job.getStatus()))
                .collect(Collectors.toList());
    }

    private void validateFile(MultipartFile file, long maxBytes, String maxStr, List<String> allowedExts) {
        if (file == null || file.isEmpty()) {
            throw new AppException("Vui lòng chọn tệp hình ảnh để tải lên.");
        }
        if (file.getSize() > maxBytes) {
            throw new AppException("Dung lượng tệp vượt quá " + maxStr + ". Vui lòng chọn tệp nhỏ hơn.");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!allowedExts.contains(ext)) {
            throw new AppException("Định dạng tệp không được hỗ trợ. Chỉ chấp nhận các định dạng: " + String.join(", ", allowedExts));
        }
    }

    private String saveMediaFile(MultipartFile file, String prefix) {
        try {
            Path uploadDir = Paths.get("FE/uploads/portal").toAbsolutePath();
            if (!Files.exists(uploadDir.getParent().getParent())) {
                uploadDir = Paths.get("../FE/uploads/portal").toAbsolutePath();
            }
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFilename = file.getOriginalFilename();
            String ext = "png";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            String fileName = prefix + "_" + System.currentTimeMillis() + "." + ext;
            Path targetPath = uploadDir.resolve(fileName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Lưu tệp media thành công: {}", targetPath);
            return "uploads/portal/" + fileName;
        } catch (Exception e) {
            log.error("Lỗi khi lưu tệp hình ảnh: ", e);
            throw new AppException("Lỗi khi lưu trữ tệp hình ảnh: " + e.getMessage());
        }
    }

    private CareerPortalConfigResponse mapToResponse(CareerPortalConfig config) {
        return CareerPortalConfigResponse.builder()
                .id(config.getId())
                .companyName(config.getCompanyName())
                .tagline(config.getTagline())
                .introduction(config.getIntroduction())
                .aboutCulture(config.getAboutCulture())
                .benefits(config.getBenefits())
                .logoUrl(config.getLogoUrl())
                .bannerUrl(config.getBannerUrl())
                .email(config.getEmail())
                .phone(config.getPhone())
                .address(config.getAddress())
                .website(config.getWebsite())
                .workingHours(config.getWorkingHours())
                .isPublished(config.getIsPublished())
                .updatedAt(config.getUpdatedAt())
                .updatedBy(config.getUpdatedBy())
                .build();
    }
}
