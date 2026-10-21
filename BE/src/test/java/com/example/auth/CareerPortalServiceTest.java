package com.example.auth;

import com.example.auth.dto.CareerPortalConfigRequest;
import com.example.auth.dto.CareerPortalConfigResponse;
import com.example.auth.dto.JobRequestDto;
import com.example.auth.exception.AppException;
import com.example.auth.repository.CareerPortalConfigRepository;
import com.example.auth.service.CareerPortalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CareerPortalServiceTest {

    @Autowired
    private CareerPortalService careerPortalService;

    @Autowired
    private CareerPortalConfigRepository portalConfigRepository;

    private byte[] createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("KN-202: Lấy cấu hình trang việc làm mặc định khi chưa có dữ liệu")
    void testGetPortalConfig_DefaultInit() {
        CareerPortalConfigResponse config = careerPortalService.getPortalConfig();
        assertNotNull(config);
        assertNotNull(config.getCompanyName());
        assertTrue(config.getCompanyName().contains("TTCS"));
        assertNotNull(config.getIntroduction());
        assertTrue(config.getIsPublished());
    }

    @Test
    @DisplayName("KN-202: Cập nhật nội dung giới thiệu tin và trang việc làm thành công")
    void testUpdatePortalConfig_Success() {
        CareerPortalConfigRequest request = CareerPortalConfigRequest.builder()
                .companyName("Công ty Cổ phần Công nghệ TTCS Global")
                .tagline("Nâng tầm sự nghiệp - Vươn tầm quốc tế")
                .introduction("Nội dung giới thiệu doanh nghiệp tuyển dụng mới cập nhật.")
                .aboutCulture("Văn hóa doanh nghiệp gắn kết, đổi mới và sáng tạo.")
                .benefits("Lương thưởng hấp dẫn, bảo hiểm quốc tế, teambuilding 2 lần/năm.")
                .email("hr@ttcsglobal.com")
                .phone("0987654321")
                .address("Tòa nhà Landmark 81, TP. Hồ Chí Minh")
                .website("https://ttcsglobal.com")
                .workingHours("Thứ Hai - Thứ Sáu: 08:00 - 17:00")
                .isPublished(true)
                .build();

        CareerPortalConfigResponse updated = careerPortalService.updatePortalConfig(request, "hrmanager");
        assertNotNull(updated);
        assertEquals("Công ty Cổ phần Công nghệ TTCS Global", updated.getCompanyName());
        assertEquals("Nâng tầm sự nghiệp - Vươn tầm quốc tế", updated.getTagline());
        assertEquals("Nội dung giới thiệu doanh nghiệp tuyển dụng mới cập nhật.", updated.getIntroduction());
        assertEquals("hrmanager", updated.getUpdatedBy());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    @DisplayName("KN-202: Kiểm tra validation khi thiếu tên công ty hoặc lời giới thiệu")
    void testUpdatePortalConfig_ValidationFailures() {
        CareerPortalConfigRequest emptyCompanyReq = CareerPortalConfigRequest.builder()
                .companyName("")
                .introduction("Giới thiệu hợp lệ")
                .build();
        assertThrows(AppException.class, () -> careerPortalService.updatePortalConfig(emptyCompanyReq, "hrmanager"));

        CareerPortalConfigRequest emptyIntroReq = CareerPortalConfigRequest.builder()
                .companyName("Tên công ty hợp lệ")
                .introduction("   ")
                .build();
        assertThrows(AppException.class, () -> careerPortalService.updatePortalConfig(emptyIntroReq, "hrmanager"));
    }

    @Test
    @DisplayName("KN-203: Tải lên logo hợp lệ cho trang công khai")
    void testUploadLogo_Success() throws IOException {
        byte[] imageBytes = createTestImage(200, 200, "png");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-logo.png",
                "image/png",
                imageBytes
        );

        String logoUrl = careerPortalService.uploadLogo(file);
        assertNotNull(logoUrl);
        assertTrue(logoUrl.startsWith("uploads/portal/logo_"));
        assertTrue(logoUrl.endsWith(".png"));
    }

    @Test
    @DisplayName("KN-203: Từ chối tải lên logo vượt quá kích thước 5MB")
    void testUploadLogo_TooLarge() {
        byte[] bigData = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile bigFile = new MockMultipartFile(
                "file",
                "big-logo.png",
                "image/png",
                bigData
        );

        AppException ex = assertThrows(AppException.class, () -> careerPortalService.uploadLogo(bigFile));
        assertTrue(ex.getMessage().contains("5MB"));
    }

    @Test
    @DisplayName("KN-203: Từ chối tải lên tệp logo định dạng không hợp lệ")
    void testUploadLogo_InvalidFormat() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "malicious data".getBytes()
        );

        AppException ex = assertThrows(AppException.class, () -> careerPortalService.uploadLogo(invalidFile));
        assertTrue(ex.getMessage().contains("không được hỗ trợ"));
    }

    @Test
    @DisplayName("KN-203: Tải lên banner hợp lệ cho trang công khai")
    void testUploadBanner_Success() throws IOException {
        byte[] imageBytes = createTestImage(1200, 400, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hero-banner.jpg",
                "image/jpeg",
                imageBytes
        );

        String bannerUrl = careerPortalService.uploadBanner(file);
        assertNotNull(bannerUrl);
        assertTrue(bannerUrl.startsWith("uploads/portal/banner_"));
        assertTrue(bannerUrl.endsWith(".jpg"));
    }

    @Test
    @DisplayName("KN-204: Lấy danh sách việc làm công khai hiển thị cho ứng viên")
    void testGetPublicJobs() {
        List<JobRequestDto> jobs = careerPortalService.getPublicJobs();
        assertNotNull(jobs);
        for (JobRequestDto j : jobs) {
            assertTrue("APPROVED".equalsIgnoreCase(j.getStatus()) || "OPEN".equalsIgnoreCase(j.getStatus()));
        }
    }
}
