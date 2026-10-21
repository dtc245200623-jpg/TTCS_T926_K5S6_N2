package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * KN-226: Request duyệt / từ chối ghi đè chỉ tiêu headcount
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadcountOverrideActionRequest {
    private Long jobRequestId;

    @JsonProperty("approved")
    @JsonAlias({"isApproved", "approved"})
    private Boolean approved;

    private String reason;
}
