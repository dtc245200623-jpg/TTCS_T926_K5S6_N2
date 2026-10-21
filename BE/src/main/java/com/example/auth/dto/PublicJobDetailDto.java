package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO dành riêng cho ứng viên xem chi tiết tin tuyển dụng (KN-272, KN-273, KN-275, KN-277).
 * Tuyệt đối không để lộ thông tin nội bộ (bước duyệt, người tạo, ngân sách nội bộ, ghi chú duyệt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicJobDetailDto {
    private Long id;
    private String title;
    
    private Long departmentId;
    private String departmentName;
    
    private Long positionId;
    private String positionName;
    
    private Long locationId;
    private String locationName;
    
    private Long employmentTypeId;
    private String employmentTypeName;
    
    private Integer headcount;
    
    // Dải lương công khai
    private Boolean isSalaryVisible;
    private Double minSalary;
    private Double maxSalary;
    private String salaryDisplay;
    
    // Mô tả, yêu cầu, quyền lợi
    private String description;
    private String requirements;
    private String benefits;
    
    // Hạn nộp & Trạng thái hết hạn
    private LocalDate applicationDeadline;
    private Boolean isExpired;
    private LocalDateTime publishedAt;
    private String status; // OPEN, APPROVED
}
