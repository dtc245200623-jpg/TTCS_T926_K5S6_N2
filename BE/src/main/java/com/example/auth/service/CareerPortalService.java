package com.example.auth.service;

import com.example.auth.dto.CareerPortalConfigRequest;
import com.example.auth.dto.CareerPortalConfigResponse;
import com.example.auth.dto.JobRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CareerPortalService {
    CareerPortalConfigResponse getPortalConfig();
    CareerPortalConfigResponse updatePortalConfig(CareerPortalConfigRequest request, String updatedBy);
    String uploadLogo(MultipartFile file);
    String uploadBanner(MultipartFile file);
    List<JobRequestDto> getPublicJobs();
}
