package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecruitmentCategoryResponse {

    private Long id;
    private String type;
    private String typeName;
    private String code;
    private String name;
    private String description;

    // KN-193: Thứ tự hiển thị
    private Integer displayOrder;

    private Boolean active;
    private Boolean isSystem;

    // KN-192: Kiểm tra ràng buộc tham chiếu
    private Long usageCount;
    private Boolean canDelete;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
