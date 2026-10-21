package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionRequest {
    @JsonProperty("approved")
    @JsonAlias({"isApproved", "approved"})
    private Boolean approved;

    // KN-236: Action name ("APPROVE", "REJECT", "REQUEST_CHANGE")
    private String action;

    private String comment;
}
