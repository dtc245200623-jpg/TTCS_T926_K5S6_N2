package com.example.auth;

import com.example.auth.dto.UserResponse;
import com.example.auth.entity.User;
import com.example.auth.exception.AppException;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserAvatarTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private final String testUsername = "avatar_test_user";

    @BeforeEach
    void setUp() {
        userRepository.findByUsername(testUsername).ifPresent(u -> userRepository.delete(u));
        testUser = User.builder()
                .username(testUsername)
                .email("avatar_test@example.com")
                .fullName("Người Dùng Test Avatar")
                .password("Password123@")
                .tokenVersion(1L)
                .roles(new java.util.HashSet<>(java.util.List.of("ROLE_USER")))
                .build();
        testUser = userRepository.save(testUser);
    }

    private byte[] createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.drawString("AVATAR", width / 4, height / 2);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    // =========================================================================
    // KN-200: XÂY DỰNG CƠ SỞ DỮ LIỆU ĐỂ CHẤP NHẬN JPG/PNG TỐI ĐA 2MB
    // =========================================================================

    @Test
    @DisplayName("KN-200: Tải lên ảnh JPG hợp lệ (<= 2MB) thành công và lưu URL vào DB")
    void testKN200_UploadValidJpg() throws Exception {
        byte[] imgBytes = createTestImage(400, 400, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", imgBytes
        );

        UserResponse response = userService.updateAvatar(testUsername, file);

        assertNotNull(response);
        assertNotNull(response.getAvatarUrl());
        assertTrue(response.getAvatarUrl().startsWith("uploads/avatars/"));
        assertTrue(response.getAvatarUrl().endsWith(".jpg"));

        // Xác nhận đã lưu vào cơ sở dữ liệu
        User updatedInDb = userRepository.findByUsername(testUsername).orElseThrow();
        assertEquals(response.getAvatarUrl(), updatedInDb.getAvatarUrl());
    }

    @Test
    @DisplayName("KN-200: Tải lên ảnh PNG hợp lệ (<= 2MB) thành công và lưu URL vào DB")
    void testKN200_UploadValidPng() throws Exception {
        byte[] imgBytes = createTestImage(300, 300, "png");
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", imgBytes
        );

        UserResponse response = userService.updateAvatar(testUsername, file);

        assertNotNull(response);
        assertNotNull(response.getAvatarUrl());
        assertTrue(response.getAvatarUrl().startsWith("uploads/avatars/"));
        assertTrue(response.getAvatarUrl().endsWith(".png"));

        // Xác nhận trong DB
        User updatedInDb = userRepository.findByUsername(testUsername).orElseThrow();
        assertEquals(response.getAvatarUrl(), updatedInDb.getAvatarUrl());
    }

    @Test
    @DisplayName("KN-200: Chặn tệp vượt quá 2MB và ném AppException")
    void testKN200_RejectFileExceeding2MB() {
        // Tạo tệp giả lập > 2MB (2.5MB = 2.5 * 1024 * 1024 bytes)
        byte[] oversizedBytes = new byte[2621440];
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file", "large_avatar.png", "image/png", oversizedBytes
        );

        AppException exception = assertThrows(AppException.class, () -> {
            userService.updateAvatar(testUsername, oversizedFile);
        });

        assertTrue(exception.getMessage().contains("Dung lượng ảnh vượt quá 2MB"));
    }

    @Test
    @DisplayName("KN-200: Chặn tệp không phải JPG hoặc PNG (ví dụ file văn bản, PDF, GIF)")
    void testKN200_RejectInvalidFormat() {
        byte[] textContent = "Đây không phải là ảnh hợp lệ".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "avatar.txt", "text/plain", textContent
        );

        AppException exception = assertThrows(AppException.class, () -> {
            userService.updateAvatar(testUsername, invalidFile);
        });

        assertTrue(exception.getMessage().contains("Định dạng tệp không được hỗ trợ"));
    }

    @Test
    @DisplayName("KN-200: Chặn tệp rỗng hoặc không có nội dung")
    void testKN200_RejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]
        );

        AppException exception = assertThrows(AppException.class, () -> {
            userService.updateAvatar(testUsername, emptyFile);
        });

        assertTrue(exception.getMessage().contains("Vui lòng chọn tệp ảnh đại diện"));
    }

    // =========================================================================
    // KN-201: ẢNH ĐƯỢC CẮT VUÔNG VÀ TẠO BẢN THU NHỎ (THUMBNAIL)
    // =========================================================================

    @Test
    @DisplayName("KN-201: Cắt vuông từ tâm và tạo bản thu nhỏ (thumbnail) kích thước chuẩn")
    void testKN201_CenterSquareCropAndThumbnail() throws Exception {
        // Tạo ảnh chữ nhật không vuông: 800 x 500 px
        int originalWidth = 800;
        int originalHeight = 500;
        byte[] rectImgBytes = createTestImage(originalWidth, originalHeight, "png");

        MockMultipartFile file = new MockMultipartFile(
                "file", "rectangle.png", "image/png", rectImgBytes
        );

        UserResponse response = userService.updateAvatar(testUsername, file);
        assertNotNull(response.getAvatarUrl());

        // Kiểm tra file thực tế được lưu trên đĩa
        String savedRelPath = response.getAvatarUrl();
        Path candidate1 = Paths.get("FE", savedRelPath);
        Path candidate2 = Paths.get("..", "FE", savedRelPath);
        Path actualPath = Files.exists(candidate1) ? candidate1 : (Files.exists(candidate2) ? candidate2 : null);

        assertNotNull(actualPath, "File ảnh đã lưu phải tồn tại trên hệ thống tệp: " + savedRelPath);

        // Đọc ảnh đã xử lý lưu trên đĩa
        BufferedImage savedImage = ImageIO.read(actualPath.toFile());
        assertNotNull(savedImage, "Ảnh lưu phải đọc được bình thường");

        // Kiểm tra tỉ lệ: Ảnh PHẢI là hình vuông (Width == Height)
        assertEquals(savedImage.getWidth(), savedImage.getHeight(),
                "KN-201: Ảnh kết quả phải được cắt vuông (tỉ lệ 1:1)");

        // Kiểm tra bản thu nhỏ: Thumbnail tối đa 256x256 pixel
        assertTrue(savedImage.getWidth() <= 256,
                "KN-201: Bản thu nhỏ (thumbnail) có kích thước tối đa 256px");
        assertEquals(256, savedImage.getWidth(),
                "KN-201: Bản thu nhỏ từ ảnh 800x500 phải đạt 256x256 pixel");

        // Xóa file test sau khi hoàn tất nếu cần
        try {
            Files.deleteIfExists(actualPath);
        } catch (Exception ignored) {}
    }
}
