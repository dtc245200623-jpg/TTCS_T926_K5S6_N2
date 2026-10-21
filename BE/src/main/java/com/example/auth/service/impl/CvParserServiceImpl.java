package com.example.auth.service.impl;

import com.example.auth.dto.CvParsingResult;
import com.example.auth.service.CvParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dịch vụ kiểm tra và trích xuất thông tin ứng viên (Họ tên, Email, Số điện thoại) từ CV PDF
 * Thực hiện theo yêu cầu các Subtasks: KN-286, KN-289, KN-290.
 */
@Service
@Slf4j
public class CvParserServiceImpl implements CvParserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:(?:\\+?84|0)[\\s.-]?)((?:3|5|7|8|9|2)[0-9]{8})"
    );

    private static final Pattern NAME_PREFIX_PATTERN = Pattern.compile(
            "^(?:họ\\s*(?:và)?\\s*tên|full\\s*name|họ\\s*tên|candidate\\s*name)\\s*[:\\-]?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /**
     * KN-290: Kiểm tra hợp lệ tệp CV PDF trước khi bóc tách
     */
    @Override
    public boolean isValidPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            return false;
        }

        // Kiểm tra magic bytes %PDF-
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[5];
            int read = is.read(header);
            if (read < 4) {
                return false;
            }
            String magic = new String(header, 0, 4);
            return "%PDF".equalsIgnoreCase(magic);
        } catch (Exception e) {
            log.warn("Không thể đọc header file PDF: {}", e.getMessage());
            return false;
        }
    }

    /**
     * KN-286: Trích xuất họ tên, email và số điện thoại từ CV PDF
     * KN-289: Xử lý trường hợp bóc tách CV thất bại mà vẫn không làm hỏng việc nộp hồ sơ
     */
    @Override
    public CvParsingResult parsePdfCv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return CvParsingResult.skipped("Không có tệp đính kèm để bóc tách");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.toLowerCase().endsWith(".pdf")) {
            return CvParsingResult.skipped("Tệp không phải định dạng PDF, bỏ qua bóc tách tự động");
        }

        if (!isValidPdf(file)) {
            log.warn("Tệp PDF không hợp lệ hoặc bị lỗi định dạng: {}", originalFilename);
            return CvParsingResult.failed("Tệp PDF không hợp lệ hoặc bị hỏng dữ liệu");
        }

        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            if (document.isEncrypted()) {
                log.warn("Tệp PDF có mật khẩu bảo vệ, không thể bóc tách: {}", originalFilename);
                return CvParsingResult.failed("Tệp PDF được mã hóa hoặc có mật khẩu bảo vệ");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String fullText = stripper.getText(document);

            if (fullText == null || fullText.trim().isEmpty()) {
                log.info("Tệp PDF không có văn bản dạng text (có thể là file scan/ảnh)");
                return CvParsingResult.failed("Tệp PDF dạng hình ảnh scan, không tìm thấy văn bản text");
            }

            String email = extractEmail(fullText);
            String phone = extractPhone(fullText);
            String fullName = extractFullName(fullText);

            String snippet = fullText.length() > 500 ? fullText.substring(0, 500) : fullText;
            log.info("Bóc tách CV PDF thành công - Name: {}, Email: {}, Phone: {}", fullName, email, phone);

            return CvParsingResult.success(fullName, email, phone, snippet);

        } catch (Throwable t) {
            // KN-289: Bắt toàn bộ lỗi, ghi log nhưng không throw ngoại lệ làm gián đoạn nộp hồ sơ
            log.error("Lỗi ngoại lệ khi bóc tách tệp PDF CV [{}]: {}", originalFilename, t.getMessage());
            return CvParsingResult.failed("Lỗi xử lý bóc tách PDF: " + t.getMessage());
        }
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            String digits = matcher.group(1);
            return "0" + digits;
        }
        return null;
    }

    private String extractFullName(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Kiểm tra có tiền tố Họ tên không
            Matcher prefixMatcher = NAME_PREFIX_PATTERN.matcher(trimmed);
            if (prefixMatcher.find()) {
                String candidateName = prefixMatcher.group(1).trim();
                if (!candidateName.isEmpty() && candidateName.length() < 100) {
                    return cleanName(candidateName);
                }
            }
        }

        // Nếu không có tiền tố, duyệt 5 dòng đầu tiên bỏ qua tiêu đề thông dụng
        int checked = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            checked++;
            if (checked > 8) break;

            String lower = trimmed.toLowerCase();
            if (lower.contains("curriculum vitae") || lower.contains("resume") || lower.contains("sơ yếu lý lịch")
                    || lower.contains("hồ sơ ứng tuyển") || lower.contains("thông tin cá nhân") || lower.startsWith("cv")) {
                continue;
            }

            // Nếu dòng chứa email hoặc phone thì bỏ qua
            if (lower.contains("@") || lower.matches(".*\\d{6,}.*")) {
                continue;
            }

            // Dòng có từ 2 đến 5 từ, độ dài hợp lý
            String[] words = trimmed.split("\\s+");
            if (words.length >= 2 && words.length <= 6 && trimmed.length() <= 60) {
                return cleanName(trimmed);
            }
        }

        return null;
    }

    private String cleanName(String name) {
        if (name == null) return null;
        return name.replaceAll("^[\\s:;\\-_]+", "")
                   .replaceAll("[\\s:;\\-_]+$", "")
                   .trim();
    }
}
