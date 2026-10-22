-- ===================================================================
-- MIGRATION SCRIPT 02: Bổ sung tính năng Quản lý hồ sơ, Rút hồ sơ & Ghi chú
-- (Hỗ trợ Story KN-293 -> KN-296 và KN-304 -> KN-308)
-- ===================================================================

SET QUOTED_IDENTIFIER ON;

-- 1. Bổ sung các cột rút hồ sơ và nhãn vào bảng applications nếu chưa có
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'applications' AND COLUMN_NAME = 'tags')
BEGIN
    ALTER TABLE applications ADD tags NVARCHAR(1000);
    PRINT 'Đã thêm cột tags vào bảng applications.';
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'applications' AND COLUMN_NAME = 'withdrawal_reason')
BEGIN
    ALTER TABLE applications ADD withdrawal_reason NVARCHAR(MAX);
    PRINT 'Đã thêm cột withdrawal_reason vào bảng applications.';
END

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'applications' AND COLUMN_NAME = 'withdrawn_at')
BEGIN
    ALTER TABLE applications ADD withdrawn_at DATETIME2;
    PRINT 'Đã thêm cột withdrawn_at vào bảng applications.';
END

-- 2. Tạo bảng application_notes cho ghi chú nội bộ giữa Recruiter và Hiring Manager (KN-307)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'application_notes')
BEGIN
    CREATE TABLE application_notes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        application_id BIGINT NOT NULL,
        content NVARCHAR(MAX) NOT NULL,
        author_username VARCHAR(100),
        author_full_name NVARCHAR(255),
        created_at DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_app_notes_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
    );
    PRINT 'Đã tạo bảng application_notes thành công.';
END
ELSE
BEGIN
    PRINT 'Bảng application_notes đã tồn tại.';
END
