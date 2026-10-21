package com.example.auth.entity;

public enum CategoryType {
    CANDIDATE_SOURCE("Nguồn ứng viên"),
    REJECTION_REASON("Lý do loại hồ sơ"),
    WORK_LOCATION("Địa điểm làm việc"),
    EMPLOYMENT_TYPE("Hình thức làm việc");

    private final String displayName;

    CategoryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
