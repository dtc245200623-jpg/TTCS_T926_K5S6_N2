package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trạng thái ứng tuyển công khai dành cho ứng viên tra cứu (KN-291, KN-292).
 * Chỉ hiển thị 4 trạng thái tổng quát: Đã nhận, Đang xem xét, Mời phỏng vấn, Kết thúc.
 * Tuyệt đối ẩn toàn bộ nhận xét nội bộ, lý do loại, điểm số phỏng vấn, và tên người phỏng vấn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicApplicationStatusDto {
    private String trackingCode;
    private String candidateName;
    private String jobTitle;
    private String departmentName;
    private String locationName;
    private LocalDateTime appliedAt;
    
    // Trạng thái tổng quát: "Đã nhận", "Đang xem xét", "Mời phỏng vấn", "Kết thúc"
    private String generalStatus;
    
    // Mô tả thông điệp thân thiện theo trạng thái
    private String statusDescription;
    
    // Mã trạng thái để UI hiển thị màu sắc phù hợp (RECEIVED, REVIEWING, INTERVIEWING, COMPLETED)
    private String statusCode;
    
    private LocalDateTime updatedAt;
}
