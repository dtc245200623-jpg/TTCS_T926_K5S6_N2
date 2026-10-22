# Hướng dẫn Quản lý & Đồng bộ Database cho Nhóm (Database Migrations)

Tài liệu này hướng dẫn cách nhóm làm việc với Database chung trên SQL Server và đồng bộ mã nguồn qua GitHub theo đúng quy trình chuẩn.

---

## 1. Nguyên tắc cơ bản: Tách biệt Code và Database

- **GitHub chỉ lưu Code:** Khi các thành viên thực hiện `git push` hoặc `git pull`, bạn chỉ đang gửi và nhận các file mã nguồn (Java, HTML, JS, CSS, SQL scripts).
- **Database lưu ở SQL Server riêng:** Mọi dữ liệu thực tế (danh sách người dùng, hồ sơ ứng viên, yêu cầu tuyển dụng, v.v.) được lưu trực tiếp trên SQL Server chung.
- **Tính toàn vẹn dữ liệu:** Khi thành viên pull code mới về, code được cập nhật nhưng cấu hình kết nối vẫn trỏ về Database Server chung, nên dữ liệu do thành viên khác tạo ra vẫn nguyên vẹn.

---

## 2. Cách đồng bộ cấu trúc Database khi có thay đổi

Trong dự án Spring Boot + Hibernate:
1. **Tạo bảng và cột mới tự động:**
   - Hệ thống đã bật thuộc tính `spring.jpa.hibernate.ddl-auto: update`.
   - Khi một thành viên thêm Entity mới hoặc thêm thuộc tính mới vào Entity trong code Java, sau khi người khác `git pull` về và khởi động lại Backend, Hibernate sẽ **tự động tạo bảng hoặc thêm cột mới** vào SQL Server mà không làm mất dữ liệu cũ.

2. **Chỉnh sửa kiểu dữ liệu hoặc cấu trúc nâng cao (Migrations):**
   - Hibernate `ddl-auto: update` không tự động đổi kiểu dữ liệu cột (ví dụ từ `VARCHAR` sang `NVARCHAR`) hoặc xóa cột để tránh rủi ro mất dữ liệu.
   - Khi cần đổi cấu trúc này, thành viên phụ trách sẽ:
     1. Viết lệnh SQL chỉnh sửa vào thư mục `database/migrations/` (ví dụ: `database/migrations/02_ten_thay_doi.sql`).
     2. Đẩy file script `.sql` này lên GitHub cùng với commit code.
     3. Khi các thành viên khác `git pull` về, chỉ cần mở file `.sql` đó trong SQL Server Management Studio (SSMS) và nhấn **Execute (F5)** để áp dụng lên Database.

---

## 3. Quy trình làm việc hàng ngày của Team

1. **Người A thêm tính năng mới:**
   - Tạo code mới + cập nhật Entity Java (hoặc tạo script `.sql` trong `database/migrations/` nếu có chỉnh sửa cột).
   - Test tính năng, dữ liệu được ghi trực tiếp vào SQL Server chung.
   - Thực hiện `git add`, `git commit -m "mô tả tính năng"` và `git push` lên GitHub.

2. **Người B nhận cập nhật:**
   - Thực hiện `git pull` từ GitHub về máy mình.
   - Mã nguồn được cập nhật phiên bản mới nhất.
   - Nếu có file `.sql` mới trong `database/migrations/`, chạy file script đó trong SSMS.
   - Khởi động lại Backend (`AuthApplication`), ứng dụng tự động truy xuất dữ liệu từ SQL Server chung (bao gồm cả các dữ liệu Người A vừa tạo).
