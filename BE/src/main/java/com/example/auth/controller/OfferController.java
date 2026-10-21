package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.dto.OfferDto;
import com.example.auth.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('APPROVER', 'HR_MANAGER', 'ADMIN', 'RECRUITER')")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getPendingOffers() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Offer chờ duyệt thành công", offerService.getPendingApprovalOffers()));
    }

    /**
     * Tạo hoặc trình duyệt Offer mới (KN-179: Tự động kiểm tra dải lương và gắn cờ vượt hạn mức)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OfferDto>> createOffer(@RequestBody Map<String, Object> body) {
        Long applicationId = Long.valueOf(body.get("applicationId").toString());
        Double offeredSalary = Double.valueOf(body.get("offeredSalary").toString());
        String note = body.get("note") != null ? body.get("note").toString() : null;

        OfferDto offer = offerService.createOrSubmitOffer(applicationId, offeredSalary, note);
        String msg = Boolean.TRUE.equals(offer.getExceedsLimit())
                ? "Tạo Offer thành công. CẢNH BÁO: Mức lương vượt hạn mức chức danh, cần Ban Giám Đốc phê duyệt!"
                : "Tạo Offer trong hạn mức thành công.";
        return ResponseEntity.ok(ApiResponse.success(msg, offer));
    }

    /**
     * Duyệt hoặc từ chối Offer (KN-179: Áp dụng giới hạn duyệt offer theo dải lương của chức danh)
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'HR_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveOffer(
            @PathVariable Long id,
            @RequestParam boolean isApproved,
            @RequestParam(required = false) String note,
            Authentication authentication) {
        if (isApproved) {
            offerService.approveOffer(id, authentication != null ? authentication.getName() : "system", note);
            return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt Offer thành công."));
        } else {
            offerService.rejectOffer(id);
            return ResponseEntity.ok(ApiResponse.success("Đã từ chối Offer."));
        }
    }
}
