package com.example.auth.repository;

import com.example.auth.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByCandidateProfileId(Long candidateProfileId);
    List<Application> findByJobRequestId(Long jobRequestId);
    List<Application> findByStatus(String status);
    
    java.util.Optional<Application> findByTrackingCode(String trackingCode);
    java.util.Optional<Application> findByTrackingCodeAndCandidateEmailIgnoreCase(String trackingCode, String email);
    boolean existsByJobRequestIdAndCandidateEmailIgnoreCase(Long jobRequestId, String email);
    long countByIpAddressAndAppliedAtAfter(String ipAddress, java.time.LocalDateTime afterTime);
    List<Application> findByCandidateEmailIgnoreCase(String email);
}
