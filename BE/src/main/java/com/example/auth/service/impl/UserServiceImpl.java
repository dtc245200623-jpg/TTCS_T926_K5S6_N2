package com.example.auth.service.impl;

import com.example.auth.dto.UserRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.entity.User;
import com.example.auth.exception.AppException;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.auth.service.EmailService emailService;

    @Override
    public org.springframework.data.domain.Page<UserResponse> getAllUsers(String search, String role, Boolean isLocked, org.springframework.data.domain.Pageable pageable) {
        return userRepository.searchUsers(search, role, isLocked, pageable).map(this::mapToResponse);
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException("Email đã tồn tại");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException("Username đã tồn tại");
        }
        
        String cleanPhone = null;
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            cleanPhone = request.getPhoneNumber().trim().replaceAll("[\\s\\-\\.]", "");
            if (!VN_PHONE_PATTERN.matcher(cleanPhone).matches()) {
                throw new AppException("Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam gồm 10 chữ số.");
            }
            if (userRepository.findByPhoneNumber(cleanPhone).isPresent()) {
                throw new AppException("Số điện thoại '" + cleanPhone + "' đã tồn tại trong hệ thống");
            }
        }

        // Tạo mật khẩu ngẫu nhiên
        String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
        
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .fullName(request.getFullName())
                .department(request.getDepartment())
                .phoneNumber(cleanPhone)
                .title(request.getTitle())
                .password(passwordEncoder.encode(tempPassword))
                .roles(request.getRoles() != null && !request.getRoles().isEmpty() ? request.getRoles() : java.util.Set.of("ROLE_USER"))
                .tokenVersion(1L)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Gửi email kích hoạt với mật khẩu tạm
        emailService.sendActivationEmail(savedUser.getEmail(), tempPassword);
        
        return mapToResponse(savedUser);
    }

    @Override
    public UserResponse updateUserRoles(Long id, Set<String> roles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));
        
        // Prevent admin from revoking their own admin privileges
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && user.getUsername().equals(authentication.getName())) {
            if (!roles.contains("ROLE_ADMIN") && !roles.contains("ADMIN")) {
                throw new AppException("Quản trị viên không thể tự thu hồi quyền quản trị của chính mình");
            }
        }
        
        user.setRoles(roles);
        // Force token invalidation by incrementing token version
        user.setTokenVersion(user.getTokenVersion() + 1); 
        
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public UserResponse toggleLockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));
                
        // Không cho phép khóa tài khoản root admin (bảo vệ an toàn)
        if (user.getRoles().contains("ROLE_ADMIN") && user.getUsername().equals("quantrihethong")) {
            throw new AppException("Không thể khóa tài khoản quản trị viên gốc");
        }
        
        // Ngăn quản trị viên tự khóa tài khoản của chính mình
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && user.getUsername().equals(authentication.getName())) {
            throw new AppException("Bạn không thể tự khóa tài khoản của chính mình");
        }
        
        user.setIsLocked(!Boolean.TRUE.equals(user.getIsLocked()));
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            user.setLockReason("Khóa bởi Quản trị viên");
            user.setLockTime(LocalDateTime.now());
            user.setTokenVersion(user.getTokenVersion() + 1); // Đăng xuất người dùng bị khóa ngay lập tức
        } else {
            user.setLockReason(null);
            user.setLockTime(null);
        }
        
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public UserResponse adminChangePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));
                
        // Không cho phép xóa tài khoản root admin
        if (user.getRoles().contains("ROLE_ADMIN") && user.getUsername().equals("quantrihethong")) {
            throw new AppException("Không thể xóa tài khoản quản trị viên gốc");
        }
        
        // Ngăn quản trị viên tự xóa chính mình
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && user.getUsername().equals(authentication.getName())) {
            throw new AppException("Bạn không thể tự xóa tài khoản của chính mình");
        }
        
        try {
            userRepository.delete(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new AppException("Không thể xóa tài khoản này vì đang có dữ liệu liên quan (công tác, hồ sơ...)");
        }
    }

    @Override
    public byte[] generateImportTemplate(String format) {
        if ("csv".equalsIgnoreCase(format)) {
            String csvHeader = "\uFEFFSTT,Tên đăng nhập (*),Email (*),Họ và tên,Phòng ban,Vai trò\n"
                    + "1,nguyenvana,nguyenvana@company.com,Nguyễn Văn A,Phòng Kỹ Thuật,ROLE_USER\n"
                    + "2,tranthib,tranthib@company.com,Trần Thị B,Phòng Tuyển Dụng,ROLE_RECRUITER\n"
                    + "3,lequangc,lequangc@company.com,Lê Quang C,Ban Giám Đốc,\"ROLE_HR_MANAGER, ROLE_ADMIN\"\n";
            return csvHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        // Default to Excel .xlsx using Apache POI
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("DanhSachTaiKhoan");

            // Header Style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            // Data Style
            org.apache.poi.ss.usermodel.CellStyle dataStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font dataFont = workbook.createFont();
            dataFont.setFontName("Arial");
            dataFont.setFontHeightInPoints((short) 10);
            dataStyle.setFont(dataFont);

            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            String[] headers = {"STT", "Tên đăng nhập (*)", "Email (*)", "Họ và tên", "Phòng ban", "Vai trò (cách nhau bởi dấu phẩy)"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample rows
            Object[][] sampleData = {
                {1, "nguyenvana", "nguyenvana@company.com", "Nguyễn Văn A", "Phòng Kỹ Thuật", "ROLE_USER"},
                {2, "tranthib", "tranthib@company.com", "Trần Thị B", "Phòng Tuyển Dụng", "ROLE_RECRUITER"},
                {3, "lequangc", "lequangc@company.com", "Lê Quang C", "Ban Giám Đốc", "ROLE_HR_MANAGER, ROLE_ADMIN"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(20);
                for (int c = 0; c < sampleData[r].length; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    if (c == 0) {
                        cell.setCellValue((Integer) sampleData[r][c]);
                    } else {
                        cell.setCellValue((String) sampleData[r][c]);
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            // Note sheet / Instructions
            org.apache.poi.ss.usermodel.Sheet noteSheet = workbook.createSheet("HuongDan");
            org.apache.poi.ss.usermodel.Row noteTitle = noteSheet.createRow(0);
            noteTitle.createCell(0).setCellValue("HƯỚNG DẪN NHẬP DỮ LIỆU TÀI KHOẢN:");
            String[] instructions = {
                "1. Các cột có dấu (*) là bắt buộc (Tên đăng nhập, Email).",
                "2. Tên đăng nhập phải từ 3 đến 30 ký tự, không chứa khoảng trắng hoặc ký tự đặc biệt.",
                "3. Email phải đúng định dạng email tiêu chuẩn và không được trùng với tài khoản đã có.",
                "4. Cột Vai trò có thể để trống (mặc định ROLE_USER) hoặc nhập một hoặc nhiều vai trò cách nhau bởi dấu phẩy.",
                "5. Các vai trò hợp lệ trong hệ thống: ROLE_USER, ROLE_ADMIN, ROLE_RECRUITER, ROLE_INTERVIEWER, ROLE_HR_MANAGER, ROLE_HIRING_MANAGER, ROLE_APPROVER, ROLE_CANDIDATE."
            };
            for (int i = 0; i < instructions.length; i++) {
                org.apache.poi.ss.usermodel.Row row = noteSheet.createRow(i + 1);
                row.createCell(0).setCellValue(instructions[i]);
            }
            noteSheet.autoSizeColumn(0);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 4000));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException("Lỗi khi tạo file mẫu: " + e.getMessage());
        }
    }

    private static final Set<String> VALID_ROLES = Set.of(
            "ROLE_USER", "ROLE_ADMIN", "ROLE_RECRUITER", "ROLE_INTERVIEWER",
            "ROLE_HR_MANAGER", "ROLE_HIRING_MANAGER", "ROLE_APPROVER", "ROLE_CANDIDATE"
    );

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private static final java.util.regex.Pattern USERNAME_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_.-]{3,30}$");

    private static class RawUserData {
        int rowNumber;
        String username;
        String email;
        String fullName;
        String department;
        String rolesStr;
    }

    private List<RawUserData> extractRawData(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("Vui lòng tải lên tệp dữ liệu");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<RawUserData> rawList = new java.util.ArrayList<>();

        try {
            if (filename.endsWith(".csv")) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    int rowNum = 0;
                    while ((line = reader.readLine()) != null) {
                        rowNum++;
                        if (rowNum == 1) continue; // skip header
                        if (line.trim().isEmpty()) continue;

                        // Basic CSV split supporting quoted values
                        String[] parts = parseCsvLine(line);
                        if (parts.length >= 2) {
                            RawUserData raw = new RawUserData();
                            raw.rowNumber = rowNum;
                            // Format: STT, Username, Email, FullName, Department, Roles
                            if (parts.length == 2) {
                                raw.username = parts[0];
                                raw.email = parts[1];
                            } else if (parts.length == 3) {
                                raw.username = parts[1];
                                raw.email = parts[2];
                            } else {
                                // Assuming col 0 is STT if numeric, otherwise col 0 is username
                                int offset = (parts[0].matches("^\\d+$")) ? 1 : 0;
                                raw.username = parts.length > offset ? parts[offset] : "";
                                raw.email = parts.length > offset + 1 ? parts[offset + 1] : "";
                                raw.fullName = parts.length > offset + 2 ? parts[offset + 2] : "";
                                raw.department = parts.length > offset + 3 ? parts[offset + 3] : "";
                                raw.rolesStr = parts.length > offset + 4 ? parts[offset + 4] : "";
                            }
                            rawList.add(raw);
                        }
                    }
                }
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                    org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();

                    for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                        org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                        if (row == null) continue;

                        String col0 = formatter.formatCellValue(row.getCell(0)).trim();
                        String col1 = formatter.formatCellValue(row.getCell(1)).trim();
                        String col2 = formatter.formatCellValue(row.getCell(2)).trim();
                        String col3 = formatter.formatCellValue(row.getCell(3)).trim();
                        String col4 = formatter.formatCellValue(row.getCell(4)).trim();
                        String col5 = formatter.formatCellValue(row.getCell(5)).trim();

                        if (col0.isEmpty() && col1.isEmpty() && col2.isEmpty() && col3.isEmpty() && col4.isEmpty() && col5.isEmpty()) {
                            continue;
                        }

                        RawUserData raw = new RawUserData();
                        raw.rowNumber = r + 1; // 1-based row number matching spreadsheet UI

                        // If col0 is numeric STT, data starts at col1
                        if (col0.matches("^\\d+$") && !col1.isEmpty()) {
                            raw.username = col1;
                            raw.email = col2;
                            raw.fullName = col3;
                            raw.department = col4;
                            raw.rolesStr = col5;
                        } else {
                            raw.username = col0;
                            raw.email = col1;
                            raw.fullName = col2;
                            raw.department = col3;
                            raw.rolesStr = col4;
                        }
                        rawList.add(raw);
                    }
                }
            } else {
                throw new AppException("Định dạng tệp không được hỗ trợ. Vui lòng tải lên tệp Excel (.xlsx, .xls) hoặc CSV (.csv)");
            }
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AppException("Không thể đọc tệp dữ liệu: " + e.getMessage());
        }

        return rawList;
    }

    private String[] parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                result.add(curVal.toString().trim());
                curVal.setLength(0);
            } else {
                curVal.append(ch);
            }
        }
        result.add(curVal.toString().trim());
        return result.toArray(new String[0]);
    }

    private List<com.example.auth.dto.UserImportRowDto> validateRows(List<RawUserData> rawList) {
        List<com.example.auth.dto.UserImportRowDto> dtoList = new java.util.ArrayList<>();
        Set<String> seenUsernames = new java.util.HashSet<>();
        Set<String> seenEmails = new java.util.HashSet<>();

        for (RawUserData raw : rawList) {
            List<String> errors = new java.util.ArrayList<>();
            String u = raw.username != null ? raw.username.trim() : "";
            String e = raw.email != null ? raw.email.trim() : "";
            String fn = raw.fullName != null ? raw.fullName.trim() : "";
            String dept = raw.department != null ? raw.department.trim() : "";

            // Validate Username
            if (u.isEmpty()) {
                errors.add("Tên đăng nhập không được để trống");
            } else if (!USERNAME_PATTERN.matcher(u).matches()) {
                errors.add("Tên đăng nhập phải từ 3 đến 30 ký tự, không chứa khoảng trắng hoặc ký tự đặc biệt");
            } else {
                String uLower = u.toLowerCase();
                if (seenUsernames.contains(uLower)) {
                    errors.add("Tên đăng nhập bị trùng lặp trong tệp");
                } else {
                    seenUsernames.add(uLower);
                    if (userRepository.existsByUsername(u)) {
                        errors.add("Tên đăng nhập đã tồn tại trong hệ thống");
                    }
                }
            }

            // Validate Email
            if (e.isEmpty()) {
                errors.add("Email không được để trống");
            } else if (!EMAIL_PATTERN.matcher(e).matches()) {
                errors.add("Định dạng email không hợp lệ");
            } else {
                String eLower = e.toLowerCase();
                if (seenEmails.contains(eLower)) {
                    errors.add("Email bị trùng lặp trong tệp");
                } else {
                    seenEmails.add(eLower);
                    if (userRepository.existsByEmail(e)) {
                        errors.add("Email đã tồn tại trong hệ thống");
                    }
                }
            }

            // Validate Roles
            Set<String> roles = new java.util.HashSet<>();
            if (raw.rolesStr == null || raw.rolesStr.trim().isEmpty()) {
                roles.add("ROLE_USER");
            } else {
                String[] rParts = raw.rolesStr.split("[,;]");
                for (String rp : rParts) {
                    String roleClean = rp.trim().toUpperCase();
                    if (!roleClean.isEmpty()) {
                        if (!roleClean.startsWith("ROLE_")) {
                            roleClean = "ROLE_" + roleClean;
                        }
                        if (VALID_ROLES.contains(roleClean)) {
                            roles.add(roleClean);
                        } else {
                            errors.add("Vai trò '" + rp.trim() + "' không hợp lệ trong hệ thống");
                        }
                    }
                }
                if (roles.isEmpty()) {
                    roles.add("ROLE_USER");
                }
            }

            com.example.auth.dto.UserImportRowDto rowDto = com.example.auth.dto.UserImportRowDto.builder()
                    .rowNumber(raw.rowNumber)
                    .username(u)
                    .email(e)
                    .fullName(fn)
                    .department(dept)
                    .roles(roles)
                    .valid(errors.isEmpty())
                    .errors(errors)
                    .build();

            dtoList.add(rowDto);
        }

        return dtoList;
    }

    @Override
    public List<com.example.auth.dto.UserImportRowDto> previewImportUsers(org.springframework.web.multipart.MultipartFile file) {
        List<RawUserData> rawList = extractRawData(file);
        if (rawList.isEmpty()) {
            throw new AppException("Tệp không chứa dòng dữ liệu nào để xem trước");
        }
        return validateRows(rawList);
    }

    @Override
    public com.example.auth.dto.ImportSummaryResponse importUsers(org.springframework.web.multipart.MultipartFile file) {
        List<RawUserData> rawList = extractRawData(file);
        if (rawList.isEmpty()) {
            throw new AppException("Tệp không chứa dòng dữ liệu nào để nhập");
        }

        List<com.example.auth.dto.UserImportRowDto> validatedRows = validateRows(rawList);
        List<com.example.auth.dto.UserImportRowDto> failedRows = new java.util.ArrayList<>();
        List<UserResponse> importedUsers = new java.util.ArrayList<>();

        for (com.example.auth.dto.UserImportRowDto row : validatedRows) {
            if (!row.isValid()) {
                // Dòng lỗi bị bỏ qua
                failedRows.add(row);
                continue;
            }

            try {
                // Tạo mật khẩu tạm thời ngẫu nhiên 8 ký tự
                String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);

                User user = User.builder()
                        .username(row.getUsername())
                        .email(row.getEmail())
                        .fullName(row.getFullName())
                        .department(row.getDepartment())
                        .password(passwordEncoder.encode(tempPassword))
                        .roles(row.getRoles())
                        .tokenVersion(1L)
                        .isLocked(false)
                        .failedAttempts(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                User savedUser = userRepository.save(user);

                // Gửi email kích hoạt (không để lỗi SMTP làm hỏng toàn bộ quá trình nhập)
                try {
                    emailService.sendActivationEmail(savedUser.getEmail(), tempPassword);
                } catch (Exception ex) {
                    // Log warning và tiếp tục
                }

                importedUsers.add(mapToResponse(savedUser));
            } catch (Exception ex) {
                row.setValid(false);
                row.getErrors().add("Lỗi khi lưu vào CSDL: " + ex.getMessage());
                failedRows.add(row);
            }
        }

        return com.example.auth.dto.ImportSummaryResponse.builder()
                .totalRows(validatedRows.size())
                .successCount(importedUsers.size())
                .failedCount(failedRows.size())
                .failedRows(failedRows)
                .importedUsers(importedUsers)
                .build();
    }

    @Override
    public UserResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy thông tin người dùng: " + username));
        return mapToResponse(user);
    }

    private static final java.util.regex.Pattern VN_PHONE_PATTERN = 
            java.util.regex.Pattern.compile("^(\\+?84|0)(3|5|7|8|9)[0-9]{8}$");

    @Override
    public UserResponse updateProfile(String username, com.example.auth.dto.UserProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy thông tin người dùng: " + username));

        if (request == null) {
            throw new AppException("Dữ liệu cập nhật không hợp lệ");
        }

        // KN-166: Cho phép sửa họ tên
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        // KN-166: Cho phép sửa chức danh hiển thị
        if (request.getTitle() != null) {
            user.setTitle(request.getTitle().trim());
        }

        // KN-166 & KN-209: Cho phép sửa số điện thoại và kiểm tra định dạng số điện thoại Việt Nam
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            String cleanPhone = request.getPhoneNumber().trim().replaceAll("[\\s\\-\\.]", "");
            if (!VN_PHONE_PATTERN.matcher(cleanPhone).matches()) {
                throw new AppException("Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam gồm 10 chữ số (Ví dụ: 0912345678, 0387654321 hoặc +84912345678).");
            }

            // Kiểm tra số điện thoại không được nhập trùng với tài khoản khác
            userRepository.findByPhoneNumber(cleanPhone).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new AppException("Số điện thoại '" + cleanPhone + "' đã được sử dụng bởi một tài khoản khác trong hệ thống. Vui lòng nhập số điện thoại khác.");
                }
            });

            user.setPhoneNumber(cleanPhone);
        } else if (request.getPhoneNumber() != null && request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(null);
        }

        // KN-167: Không tự đổi được email, phòng ban và vai trò
        // Không nhận và không cập nhật email, department, roles, username!

        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Override
    public UserResponse updateAvatar(String username, org.springframework.web.multipart.MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Không tìm thấy thông tin người dùng: " + username));

        if (file == null || file.isEmpty()) {
            throw new AppException("Vui lòng chọn tệp ảnh đại diện.");
        }

        // KN-200: Kiểm tra dung lượng tối đa 2MB (2 * 1024 * 1024 bytes)
        long maxSizeBytes = 2 * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new AppException("Dung lượng ảnh vượt quá 2MB. Vui lòng chọn tệp ảnh có dung lượng tối đa 2MB.");
        }

        // KN-200: Kiểm tra định dạng tệp (chỉ chấp nhận JPG/PNG)
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        String contentType = file.getContentType();
        boolean isValidFormat = "jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension)
                || (contentType != null && (contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/jpg")));
        if (!isValidFormat) {
            throw new AppException("Định dạng tệp không được hỗ trợ. Hệ thống chỉ chấp nhận ảnh định dạng JPG hoặc PNG.");
        }

        try {
            // KN-201: Cắt vuông tâm ảnh (Center square crop) và tạo bản thu nhỏ (thumbnail)
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new AppException("Tệp ảnh không hợp lệ hoặc bị lỗi nội dung.");
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            int squareSize = Math.min(width, height);
            int x = (width - squareSize) / 2;
            int y = (height - squareSize) / 2;

            // Cắt vuông tâm ảnh
            BufferedImage squareImage = originalImage.getSubimage(x, y, squareSize, squareSize);

            // Tạo bản thu nhỏ (Thumbnail 256x256 pixel chất lượng cao)
            int targetThumbnailSize = Math.min(squareSize, 256);
            boolean isPng = "png".equals(extension) || (contentType != null && contentType.equalsIgnoreCase("image/png"));
            BufferedImage thumbnail = new BufferedImage(
                    targetThumbnailSize,
                    targetThumbnailSize,
                    isPng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(squareImage, 0, 0, targetThumbnailSize, targetThumbnailSize, null);
            g2d.dispose();

            // Xác định thư mục lưu trữ ảnh (tại FE/uploads/avatars)
            Path uploadDir = Paths.get("FE/uploads/avatars").toAbsolutePath();
            if (!Files.exists(uploadDir.getParent().getParent())) {
                uploadDir = Paths.get("../FE/uploads/avatars").toAbsolutePath();
            }
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String outputFormat = isPng ? "png" : "jpg";
            String newFilename = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + "." + outputFormat;
            Path targetPath = uploadDir.resolve(newFilename);
            ImageIO.write(thumbnail, outputFormat, targetPath.toFile());

            // Lưu đường dẫn ảnh vào cơ sở dữ liệu
            String avatarUrl = "uploads/avatars/" + newFilename;
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(LocalDateTime.now());

            User updatedUser = userRepository.save(user);
            return mapToResponse(updatedUser);

        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException("Lỗi khi xử lý và lưu ảnh đại diện: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
        }
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .department(user.getDepartment())
                .phoneNumber(user.getPhoneNumber())
                .title(user.getTitle())
                .avatarUrl(user.getAvatarUrl())
                .roles(user.getRoles())
                .isLocked(Boolean.TRUE.equals(user.getIsLocked()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}

