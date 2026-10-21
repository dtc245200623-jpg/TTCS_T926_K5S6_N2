package com.example.auth.service;

import com.example.auth.dto.OfferDto;
import java.util.List;

public interface OfferService {
    List<OfferDto> getPendingApprovalOffers();
    OfferDto createOrSubmitOffer(Long applicationId, Double offeredSalary, String note);
    void approveOffer(Long id, String approverUsername, String note);
    void approveOffer(Long id);
    void rejectOffer(Long id);
}
