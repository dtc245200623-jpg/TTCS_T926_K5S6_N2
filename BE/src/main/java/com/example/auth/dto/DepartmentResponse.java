package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentResponse {

    private Long id;
    private String code;
    private String name;
    private String description;

    // Cấu trúc phân cấp nhiều cấp (KN-174)
    private Long parentId;
    private String parentName;
    private String parentCode;
    private Integer level;
    private Boolean hasChildren;

    @Builder.Default
    private List<DepartmentResponse> children = new ArrayList<>();

    // Người phụ trách phòng ban (KN-175)
    private Long managerId;
    private String managerName;
    private String managerEmail;
    private String managerUsername;
    private String managerTitle;

    // Trạng thái áp dụng & Thống kê tuyển dụng (KN-176)
    private Boolean active;
    private Long totalJobRequests;
    private Long openJobRequests;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
