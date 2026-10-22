-- ===================================================================
-- MIGRATION SCRIPT 01: Cập nhật NVARCHAR (Tiếng Việt) & Bổ sung cột
-- ===================================================================
-- Mô tả: Chuyển đổi các cột văn bản sang NVARCHAR để hỗ trợ tiếng Việt có dấu
-- và bổ sung các cột mới cần thiết cho hệ thống tuyển dụng.
-- Thực thi trên SQL Server (SSMS hoặc sqlcmd).
-- ===================================================================

SET QUOTED_IDENTIFIER ON;

-- 1. Phòng ban
ALTER TABLE departments ALTER COLUMN name NVARCHAR(255);
ALTER TABLE departments ALTER COLUMN description NVARCHAR(MAX);

-- 2. Yêu cầu tuyển dụng
ALTER TABLE job_requests ALTER COLUMN title NVARCHAR(255);
ALTER TABLE job_requests ALTER COLUMN description NVARCHAR(MAX);
ALTER TABLE job_requests ALTER COLUMN requirements NVARCHAR(MAX);

-- 3. Hồ sơ ứng viên
ALTER TABLE candidate_profiles ALTER COLUMN full_name NVARCHAR(255);
ALTER TABLE candidate_profiles ALTER COLUMN address NVARCHAR(MAX);
ALTER TABLE candidate_profiles ALTER COLUMN current_title NVARCHAR(255);

-- 4. Đề xuất tuyển dụng (Offer) & Phỏng vấn
ALTER TABLE offers ALTER COLUMN note NVARCHAR(MAX);
ALTER TABLE interviews ALTER COLUMN location NVARCHAR(255);
ALTER TABLE interviews ALTER COLUMN feedback NVARCHAR(MAX);

-- 5. Người dùng (Users)
ALTER TABLE users ALTER COLUMN full_name NVARCHAR(255);
ALTER TABLE users ALTER COLUMN department NVARCHAR(255);
ALTER TABLE users ALTER COLUMN job_title NVARCHAR(100);
ALTER TABLE users ALTER COLUMN lock_reason NVARCHAR(500);

-- Bổ sung cột avatar_url nếu chưa có
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'avatar_url')
BEGIN
    ALTER TABLE users ADD avatar_url NVARCHAR(500);
    PRINT 'Đã thêm cột avatar_url vào bảng users thành công.';
END
ELSE
BEGIN
    PRINT 'Cột avatar_url đã tồn tại trong bảng users.';
END
