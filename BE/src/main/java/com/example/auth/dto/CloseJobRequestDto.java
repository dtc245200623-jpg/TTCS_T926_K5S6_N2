package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloseJobRequestDto {

    /**
     * KN-246: Ba trạng thái kết thúc có lý do riêng:
     * - FILLED: Đã tuyển đủ
     * - PAUSED: Tạm dừng
     * - CANCELLED: Huỷ
     */
    @NotBlank(message = "Vui lòng chọn trạng thái kết thúc (FILLED, PAUSED, CANCELLED)")
    private String closingReason;

    /**
     * Ghi chú / lý do chi tiết khi đóng hoặc huỷ yêu cầu
     */
    private String closingNote;

    /**
     * KN-249: Xác nhận đồng ý đóng yêu cầu dù còn ứng viên trong pipeline
     */
    private Boolean confirmCloseWithPendingCandidates;
}
