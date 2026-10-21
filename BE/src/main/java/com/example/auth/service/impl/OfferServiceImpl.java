package com.example.auth.service.impl;

import com.example.auth.dto.OfferDto;
import com.example.auth.entity.Application;
import com.example.auth.entity.JobPosition;
import com.example.auth.entity.JobRequest;
import com.example.auth.entity.Offer;
import com.example.auth.exception.AppException;
import com.example.auth.repository.ApplicationRepository;
import com.example.auth.repository.JobPositionRepository;
import com.example.auth.repository.OfferRepository;
import com.example.auth.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final ApplicationRepository applicationRepository;
    private final JobPositionRepository jobPositionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OfferDto> getPendingApprovalOffers() {
        return offerRepository.findByStatus("PENDING_APPROVAL").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OfferDto createOrSubmitOffer(Long applicationId, Double offeredSalary, String note) {
        if (offeredSalary == null || offeredSalary <= 0) {
            throw new AppException("Mức lương đề xuất (offeredSalary) phải lớn hơn 0.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ ứng tuyển với ID: " + applicationId));

        Offer offer = offerRepository.findByApplicationId(applicationId).orElse(null);
        if (offer == null) {
            offer = Offer.builder()
                    .application(application)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        offer.setOfferedSalary(offeredSalary);
        offer.setNote(note);
        offer.setStatus("PENDING_APPROVAL");

        // KN-179: Kiểm tra giới hạn duyệt offer theo dải lương của chức danh
        Double maxAllowed = resolveMaxSalaryAllowed(application);
        offer.setMaxSalaryAllowed(maxAllowed);

        if (maxAllowed != null && offeredSalary > maxAllowed) {
            offer.setExceedsLimit(true);
            log.warn("Offer ID cho ứng viên ID {} vượt hạn mức dải lương tối đa: đề xuất {}, tối đa {}",
                    applicationId, offeredSalary, maxAllowed);
        } else {
            offer.setExceedsLimit(false);
        }

        Offer saved = offerRepository.save(offer);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void approveOffer(Long id, String approverUsername, String note) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy Offer với ID: " + id));

        // KN-179: Áp dụng giới hạn duyệt offer theo dải lương của chức danh
        if (Boolean.TRUE.equals(offer.getExceedsLimit())) {
            // Kiểm tra xem người dùng hiện tại có quyền APPROVER (Ban Giám Đốc) hoặc ADMIN không
            boolean isApprover = hasAuthority("ROLE_APPROVER") || hasAuthority("ROLE_ADMIN");
            if (!isApprover) {
                Double offered = offer.getOfferedSalary() != null ? offer.getOfferedSalary() : 0.0;
                Double max = offer.getMaxSalaryAllowed() != null ? offer.getMaxSalaryAllowed() : 0.0;
                throw new AppException(String.format(
                        "Offer này có mức lương %,.0f đ vượt quá dải lương tối đa (%,.0f đ) của chức danh. " +
                        "Theo quy tắc hạn mức (KN-179), chỉ Người duyệt (Ban Giám Đốc - ROLE_APPROVER) mới có thẩm quyền phê duyệt offer này.",
                        offered, max
                ));
            }
        }

        offer.setStatus("APPROVED");
        if (note != null && !note.trim().isEmpty()) {
            offer.setApprovalNote(note.trim());
        }
        offerRepository.save(offer);
        log.info("Offer ID {} đã được phê duyệt thành công bởi {}", id, approverUsername);
    }

    @Override
    @Transactional
    public void approveOffer(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "system";
        approveOffer(id, username, null);
    }

    @Override
    @Transactional
    public void rejectOffer(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy Offer với ID: " + id));
        offer.setStatus("DECLINED");
        offerRepository.save(offer);
        log.info("Offer ID {} đã bị từ chối phê duyệt.", id);
    }

    private Double resolveMaxSalaryAllowed(Application application) {
        if (application == null || application.getJobRequest() == null) {
            return null;
        }
        JobRequest jobRequest = application.getJobRequest();

        // 1. Kiểm tra trực tiếp qua JobPosition liên kết
        if (jobRequest.getPosition() != null && jobRequest.getPosition().getMaxSalary() != null) {
            return jobRequest.getPosition().getMaxSalary();
        }

        // 2. Tìm JobPosition theo Title của JobRequest
        if (jobRequest.getTitle() != null) {
            JobPosition position = jobPositionRepository.findByName(jobRequest.getTitle())
                    .or(() -> jobPositionRepository.findByCode(jobRequest.getTitle()))
                    .orElse(null);
            if (position != null && position.getMaxSalary() != null) {
                return position.getMaxSalary();
            }
        }

        // 3. Fallback sang maxSalary của JobRequest
        return jobRequest.getMaxSalary();
    }

    private boolean hasAuthority(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (role.equals(ga.getAuthority())) return true;
        }
        return false;
    }

    private OfferDto mapToDto(Offer offer) {
        String candidateName = null;
        String jobTitle = null;
        String posCode = null;
        String posLevel = null;
        Double maxSalary = offer.getMaxSalaryAllowed();

        if (offer.getApplication() != null) {
            if (offer.getApplication().getCandidateProfile() != null) {
                candidateName = offer.getApplication().getCandidateProfile().getFullName();
            }
            if (offer.getApplication().getJobRequest() != null) {
                JobRequest jr = offer.getApplication().getJobRequest();
                jobTitle = jr.getTitle();
                if (maxSalary == null) maxSalary = jr.getMaxSalary();
                if (jr.getPosition() != null) {
                    posCode = jr.getPosition().getCode();
                    posLevel = jr.getPosition().getLevel();
                    if (maxSalary == null) maxSalary = jr.getPosition().getMaxSalary();
                }
            }
        }

        return OfferDto.builder()
                .id(offer.getId())
                .candidateName(candidateName)
                .jobTitle(jobTitle)
                .positionCode(posCode)
                .positionLevel(posLevel)
                .offeredSalary(offer.getOfferedSalary())
                .maxSalary(maxSalary)
                .maxSalaryAllowed(offer.getMaxSalaryAllowed())
                .exceedsLimit(offer.getExceedsLimit())
                .status(offer.getStatus())
                .note(offer.getNote())
                .approvalNote(offer.getApprovalNote())
                .build();
    }
}
