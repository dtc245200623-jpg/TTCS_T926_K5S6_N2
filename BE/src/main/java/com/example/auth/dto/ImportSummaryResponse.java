package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportSummaryResponse {
    private int totalRows;
    private int successCount;
    private int failedCount;

    @Builder.Default
    private List<UserImportRowDto> failedRows = new ArrayList<>();

    @Builder.Default
    private List<UserResponse> importedUsers = new ArrayList<>();
}
