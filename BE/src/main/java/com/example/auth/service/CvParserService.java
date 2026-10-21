package com.example.auth.service;

import com.example.auth.dto.CvParsingResult;
import org.springframework.web.multipart.MultipartFile;

public interface CvParserService {
    boolean isValidPdf(MultipartFile file);
    CvParsingResult parsePdfCv(MultipartFile file);
}
