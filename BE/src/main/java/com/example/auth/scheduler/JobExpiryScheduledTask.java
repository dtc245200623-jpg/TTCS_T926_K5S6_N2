package com.example.auth.scheduler;

import com.example.auth.service.JobRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * KN-302, KN-303: Tác vụ định kỳ hằng ngày để tự động ẩn các tin tuyển dụng quá hạn nhận hồ sơ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobExpiryScheduledTask {

    private final JobRequestService jobRequestService;

    /**
     * Tự động quét lúc 00:00 mỗi ngày
     * Cron: giây phút giờ ngày_trong_tháng tháng ngày_trong_tuần
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleDailyJobExpiryCheck() {
        log.info(">>> [CRON] Bắt đầu tác vụ định kỳ hằng ngày kiểm tra tin tuyển dụng quá hạn nhận hồ sơ...");
        try {
            int expiredCount = jobRequestService.autoExpireJobs();
            log.info(">>> [CRON] Tác vụ định kỳ hoàn tất: Đã ẩn {} tin quá hạn", expiredCount);
        } catch (Exception e) {
            log.error(">>> [CRON] Lỗi trong quá trình quét tin quá hạn: {}", e.getMessage(), e);
        }
    }

    /**
     * Tự động quét một lần khi ứng dụng vừa khởi động xong
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupJobExpiryCheck() {
        log.info(">>> [STARTUP] Kiểm tra tin tuyển dụng quá hạn khi hệ thống khởi động...");
        try {
            int expiredCount = jobRequestService.autoExpireJobs();
            if (expiredCount > 0) {
                log.info(">>> [STARTUP] Đã tự động ẩn {} tin tuyển dụng quá hạn", expiredCount);
            }
        } catch (Exception e) {
            log.warn(">>> [STARTUP] Cảnh báo kiểm tra tin quá hạn lúc khởi động: {}", e.getMessage());
        }
    }
}
