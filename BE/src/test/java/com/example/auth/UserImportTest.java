package com.example.auth;

import com.example.auth.dto.ImportSummaryResponse;
import com.example.auth.dto.UserImportRowDto;
import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserImportTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("existing_user")) {
            userRepository.save(User.builder()
                    .username("existing_user")
                    .email("existing@company.com")
                    .password("secret123")
                    .roles(java.util.Set.of("ROLE_USER"))
                    .tokenVersion(1L)
                    .build());
        }
    }

    @Test
    void testGenerateTemplate() {
        byte[] excelBytes = userService.generateImportTemplate("xlsx");
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 100);

        byte[] csvBytes = userService.generateImportTemplate("csv");
        assertNotNull(csvBytes);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("Tên đăng nhập (*)"));
        assertTrue(csvContent.contains("nguyenvana"));
    }

    @Test
    void testPreviewImportCsvWithErrorsAndValidRows() {
        // Line 1: Header
        // Line 2: Valid user (row 2)
        // Line 3: Missing username (row 3)
        // Line 4: Invalid email (row 4)
        // Line 5: Duplicate username with DB (row 5)
        // Line 6: Duplicate username in file (row 6)
        String csv = "\uFEFFSTT,Tên đăng nhập (*),Email (*),Họ và tên,Phòng ban,Vai trò\n"
                + "1,newuser1,newuser1@company.com,Nguyễn Một,IT,ROLE_USER\n"
                + "2,,missinguser@company.com,Người Vô Danh,Sales,ROLE_USER\n"
                + "3,newuser3,invalid-email-format,Người Ba,HR,ROLE_RECRUITER\n"
                + "4,existing_user,existing_user_new@company.com,Người Trùng,Admin,ROLE_ADMIN\n"
                + "5,newuser1,newuser1_dupe@company.com,Trùng Trong File,IT,ROLE_USER\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        List<UserImportRowDto> preview = userService.previewImportUsers(file);
        assertEquals(5, preview.size());

        // Row 2: valid
        assertTrue(preview.get(0).isValid());
        assertEquals("newuser1", preview.get(0).getUsername());
        assertTrue(preview.get(0).getErrors().isEmpty());

        // Row 3: missing username
        assertFalse(preview.get(1).isValid());
        assertTrue(preview.get(1).getErrors().stream().anyMatch(e -> e.contains("Tên đăng nhập không được để trống")));

        // Row 4: invalid email
        assertFalse(preview.get(2).isValid());
        assertTrue(preview.get(2).getErrors().stream().anyMatch(e -> e.contains("Định dạng email không hợp lệ")));

        // Row 5: username exists in DB
        assertFalse(preview.get(3).isValid());
        assertTrue(preview.get(3).getErrors().stream().anyMatch(e -> e.contains("đã tồn tại trong hệ thống")));

        // Row 6: username duplicate in file
        assertFalse(preview.get(4).isValid());
        assertTrue(preview.get(4).getErrors().stream().anyMatch(e -> e.contains("trùng lặp trong tệp")));
    }

    @Test
    void testImportSkipsErrorsAndImportsValidRows() {
        String csv = "\uFEFFSTT,Tên đăng nhập (*),Email (*),Họ và tên,Phòng ban,Vai trò\n"
                + "1,valid_import_1,valid1@test.com,Người Số Một,Kỹ Thuật,ROLE_RECRUITER\n"
                + "2,,error_email@test.com,Lỗi Tên,,ROLE_USER\n"
                + "3,valid_import_2,valid2@test.com,Người Số Hai,Nhân Sự,\"ROLE_USER, ROLE_ADMIN\"\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportSummaryResponse summary = userService.importUsers(file);

        assertEquals(3, summary.getTotalRows());
        assertEquals(2, summary.getSuccessCount());
        assertEquals(1, summary.getFailedCount());
        assertEquals(1, summary.getFailedRows().size());
        assertEquals(2, summary.getImportedUsers().size());

        // Verify valid users actually exist in DB
        assertTrue(userRepository.existsByUsername("valid_import_1"));
        assertTrue(userRepository.existsByUsername("valid_import_2"));

        User u2 = userRepository.findByUsername("valid_import_2").orElseThrow();
        assertTrue(u2.getRoles().contains("ROLE_ADMIN"));
        assertTrue(u2.getRoles().contains("ROLE_USER"));
    }

    @Test
    void testImportExcelEndToEnd() {
        // Generate template
        byte[] excelBytes = userService.generateImportTemplate("xlsx");
        assertNotNull(excelBytes);

        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                excelBytes);

        // Preview template (it has 3 valid sample users)
        List<UserImportRowDto> preview = userService.previewImportUsers(file);
        assertEquals(3, preview.size());
        assertTrue(preview.stream().allMatch(UserImportRowDto::isValid));

        // Import the users
        ImportSummaryResponse summary = userService.importUsers(file);
        assertEquals(3, summary.getTotalRows());
        assertEquals(3, summary.getSuccessCount());
        assertEquals(0, summary.getFailedCount());

        assertTrue(userRepository.existsByUsername("nguyenvana"));
        assertTrue(userRepository.existsByUsername("tranthib"));
        assertTrue(userRepository.existsByUsername("lequangc"));
    }
}
