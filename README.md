# TTCS_T926_K5S6_N2
# HỆ THỐNG TUYỂN DỤNG NỘI BỘ
## Internal Recruitment Management System

> **Mã dự án:** `TTCS_T926_K5S6_N2`  
> **Nhóm thực hiện:** Nhóm 2  
> **Thời gian dự kiến:** 8 tuần (8 Sprint, mỗi Sprint 1 tuần)  
> **Phương pháp phát triển:** Agile/Scrum

---

## 1. Tổng quan dự án

Hệ thống tuyển dụng nội bộ (Internal Recruitment Management System – ATS) là ứng dụng quản lý tuyển dụng trên nền tảng web, hỗ trợ doanh nghiệp theo dõi toàn bộ vòng đời tuyển dụng trên một nguồn dữ liệu tập trung, từ khi phát sinh nhu cầu nhân sự đến khi ứng viên nhận việc.

Dự án hướng đến việc thay thế cách quản lý rời rạc bằng Excel, Gmail và Google Drive bằng quy trình có thể theo dõi, kiểm soát và truy xuất lịch sử. Các quyết định quan trọng như phê duyệt nhu cầu tuyển dụng, chuyển giai đoạn ứng viên và chốt đề xuất nhận việc cần có tiêu chí rõ ràng và lưu lại dấu vết xử lý.

Dự án được tổ chức theo backlog gồm **9 Epic, 76 User Story, 350 Story Point và 8 Sprint**. Jira được sử dụng để quản lý công việc; GitHub dùng để quản lý mã nguồn, nhánh phát triển và Pull Request.

---

## 2. Bài toán nghiệp vụ

Hiện trạng nghiệp vụ được mô tả trong backlog cho thấy bộ phận nhân sự đang sử dụng nhiều công cụ rời rạc để thực hiện tuyển dụng:

- Yêu cầu tuyển dụng được phê duyệt qua email hoặc trao đổi chat.
- CV ứng viên nằm rải rác trong hộp thư của từng nhân viên tuyển dụng.
- Lịch phỏng vấn được hẹn thủ công qua điện thoại.
- Nhận xét phỏng vấn được ghi theo nhiều định dạng khác nhau.
- Tiến độ tuyển dụng khó theo dõi tập trung và khó xác định khâu đang bị tắc.
- Lịch sử phê duyệt headcount và mức lương thiếu dấu vết thống nhất.
- Ứng viên phù hợp từng bị loại có thể bị bỏ quên khi phát sinh vị trí mới.
- Ứng viên không nhận được phản hồi kịp thời sau khi bị loại.

Hệ thống được xây dựng để tập trung dữ liệu, chuẩn hóa quy trình và hỗ trợ theo dõi tiến độ, lịch sử xử lý cũng như kết quả tuyển dụng.

---

## 3. Tầm nhìn và mục tiêu sản phẩm

### Tầm nhìn

Dành cho bộ phận Nhân sự và các trưởng bộ phận đang quản lý tuyển dụng bằng các công cụ rời rạc, hệ thống là nền tảng ATS nội bộ trên web giúp theo dõi toàn bộ vòng đời tuyển dụng từ yêu cầu headcount đến ngày nhận việc. Khác với cách làm thủ công, hệ thống lưu lại dấu vết, tiêu chí và lịch sử của các quyết định tuyển dụng để có thể tra cứu, đối chiếu.

### Mục tiêu sau 8 tuần

| Mục tiêu | Chỉ số/tiêu chí đánh giá |
|---|---|
| Chạy trọn một vị trí tuyển dụng thật trên hệ thống | Một vị trí pilot đi hết chu trình: yêu cầu → duyệt → đăng tin → ứng tuyển → phỏng vấn → offer → nhận việc |
| Rút ngắn thời gian sàng lọc | Recruiter xử lý 20 CV mới trong tối đa 15 phút nhờ pipeline và nhãn sàng lọc |
| Lưu dấu vết phê duyệt headcount | 100% yêu cầu tuyển dụng trong dữ liệu pilot có lịch sử phê duyệt đầy đủ |
| Hỗ trợ quyết định tuyển theo tiêu chí | Mỗi ứng viên vào vòng cuối có tối thiểu 2 phiếu đánh giá theo cùng khung năng lực |
| Bàn giao sản phẩm chạy được | Deploy staging tự động, coverage tầng service từ 60% trở lên và không còn lỗi Critical tồn đọng |

---

## 4. Phạm vi dự án

### 4.1. Trong phạm vi (In-scope)

- Xác thực, phân quyền theo vai trò và quản trị người dùng nội bộ.
- Danh mục phòng ban, chức danh, dải lương và khung năng lực.
- Yêu cầu tuyển dụng, luồng phê duyệt nhiều cấp và ngân sách headcount.
- Soạn thảo, duyệt và xuất bản tin tuyển dụng.
- Cổng ứng tuyển công khai, nộp CV, tra cứu trạng thái và rút hồ sơ.
- Giới thiệu ứng viên nội bộ (referral).
- Hồ sơ ứng viên hợp nhất, phát hiện hồ sơ trùng và kho ứng viên tiềm năng.
- Pipeline tuyển dụng dạng Kanban theo từng giai đoạn.
- Đặt lịch phỏng vấn, phát hiện trùng lịch và gửi thư mời.
- Phiếu đánh giá theo khung năng lực và bảng so sánh ứng viên.
- Đề xuất offer, duyệt offer theo hạn mức và checklist onboarding.
- Email tự động theo giai đoạn và thông báo trong ứng dụng.
- Dashboard và báo cáo tuyển dụng.

### 4.2. Ngoài phạm vi (Out-of-scope)

- Bóc tách CV bằng AI hoặc so khớp ngữ nghĩa giữa JD và hồ sơ.
- Đăng tin tự động lên VietnamWorks, TopCV, LinkedIn và các nền tảng bên ngoài.
- Đồng bộ hai chiều với Google Calendar hoặc Outlook.
- Phỏng vấn video trực tiếp trong ứng dụng.
- Bài kiểm tra năng lực trực tuyến và chấm điểm tự động.
- Ký số hợp đồng lao động.
- Quản lý nhân sự sau khi nhận việc như chấm công, tiền lương và đánh giá định kỳ.
- Ứng dụng di động native.

---

## 5. Đối tượng sử dụng và vai trò

Hệ thống có 7 vai trò chính:

| Vai trò | Mã | Mô tả và mục tiêu sử dụng |
|---|---|---|
| Ứng viên | `Candidate` | Người nộp hồ sơ từ bên ngoài, không có tài khoản nội bộ; nộp CV, tra cứu trạng thái, xác nhận lịch phỏng vấn và phản hồi offer. |
| Nhân viên tuyển dụng | `Recruiter` | Vận hành tuyển dụng hằng ngày; sàng lọc CV, điều phối pipeline, đặt lịch phỏng vấn và soạn offer. |
| Trưởng bộ phận | `Hiring Manager` | Người sở hữu nhu cầu tuyển dụng; tạo yêu cầu, xem ứng viên thuộc vị trí và tham gia quyết định tuyển. |
| Người phỏng vấn | `Interviewer` | Nhân sự được mời tham gia vòng phỏng vấn; xem lịch, đọc CV và nộp phiếu đánh giá theo khung năng lực. |
| Trưởng phòng Nhân sự | `HR Manager` | Giám sát hoạt động tuyển dụng, phân công recruiter, theo dõi ngân sách headcount và báo cáo. |
| Người duyệt | `Approver` | Ban giám đốc hoặc cấp duyệt theo hạn mức; phê duyệt yêu cầu tuyển dụng và offer vượt hạn mức. |
| Quản trị hệ thống | `Admin` | Vận hành ứng dụng; quản lý tài khoản, vai trò, danh mục dùng chung và nhật ký hệ thống. |

### Nguyên tắc phân quyền

- Quyền được cấp theo vai trò và phạm vi công việc.
- Các quyền trên dữ liệu ứng viên phải được kiểm tra ở tầng máy chủ.
- Recruiter, Hiring Manager và Interviewer chỉ được truy cập dữ liệu trong phạm vi được giao hoặc tham gia.
- Admin có quyền quản trị theo ma trận phân quyền của hệ thống.
- Các quyền xem, ghi và toàn quyền được xác định theo từng module.

---

## 6. Các phân hệ chức năng (Epic)

Dự án có 9 Epic với tổng cộng 76 User Story và 350 Story Point.

| Mã Epic | Tên Epic | Phạm vi chính | Story Point | Số Story | Sprint |
|---|---|---|---:|---:|---|
| EP-01 | Tài khoản, Phân quyền & Hồ sơ | Đăng nhập, khôi phục mật khẩu, phân quyền theo vai trò, quản trị tài khoản nội bộ và hồ sơ cá nhân. | 52 | 13 | 1–2 |
| EP-02 | Danh mục Tổ chức & Vị trí | Phòng ban, chức danh, dải lương, khung năng lực và ngân hàng câu hỏi phỏng vấn. | 25 | 5 | 2 |
| EP-03 | Yêu cầu tuyển dụng & Phê duyệt | Requisition, luồng duyệt nhiều cấp, ngân sách headcount và phân công recruiter. | 44 | 9 | 2–3 |
| EP-04 | Đăng tin & Cổng ứng tuyển | Soạn và xuất bản tin, trang việc làm công khai, nộp CV, tra cứu trạng thái, rút hồ sơ và giới thiệu nội bộ. | 47 | 11 | 2–4 |
| EP-05 | Hồ sơ ứng viên & Pipeline | Hồ sơ hợp nhất, gộp trùng, Kanban theo giai đoạn, sàng lọc và kho ứng viên tiềm năng. | 53 | 11 | 4–6 |
| EP-06 | Phỏng vấn & Đánh giá | Đặt lịch đơn lẻ/hàng loạt, chống trùng lịch, thư mời, câu hỏi gợi ý, phiếu đánh giá và quyết định tuyển. | 51 | 11 | 6–7 |
| EP-07 | Offer & Onboarding | Đề xuất offer, duyệt theo hạn mức lương, thư mời nhận việc, xử lý từ chối và checklist ngày đầu. | 28 | 5 | 7 |
| EP-08 | Thông báo & Email tự động | Mẫu email theo giai đoạn, thư từ chối hàng loạt, nhật ký email, thông báo trong ứng dụng và nhắc SLA. | 22 | 6 | 5, 7, 8 |
| EP-09 | Báo cáo & Dashboard tuyển dụng | Phễu tuyển dụng, tỷ lệ chuyển đổi, time-to-hire, cost-per-hire, hiệu quả nguồn ứng viên và tiến độ vị trí. | 28 | 5 | 8 |
| **Tổng** | **9 Epic** | | **350** | **76** | **1–8** |

---

## 7. Kế hoạch triển khai Sprint

Dự án dự kiến triển khai trong 8 tuần, mỗi Sprint kéo dài 1 tuần. Velocity mục tiêu là 42–45 Story Point/Sprint; đây là mục tiêu và sẽ được hiệu chỉnh sau Sprint 2 theo velocity thực tế.

| Sprint | Chủ đề | Kết quả demo cuối Sprint | Story | Point |
|---|---|---|---:|---:|
| Sprint 1 | Tài khoản & phân quyền | Bảy vai trò đăng nhập được và chỉ thấy đúng phần việc của mình. | 10 | 42 |
| Sprint 2 | Danh mục tổ chức & vị trí | Sơ đồ tổ chức, khung năng lực và một yêu cầu tuyển dụng đầu tiên. | 10 | 45 |
| Sprint 3 | Phê duyệt & đăng tin | Một headcount đi trọn luồng duyệt rồi thành tin tuyển dụng công khai. | 10 | 44 |
| Sprint 4 | Cổng ứng tuyển | Ứng viên ngoài nộp CV trên điện thoại và tra cứu được trạng thái. | 9 | 45 |
| Sprint 5 | Hồ sơ ứng viên & pipeline | Recruiter điều phối 20 ứng viên trên một bảng Kanban. | 10 | 45 |
| Sprint 6 | Phỏng vấn & đánh giá | Một buổi phỏng vấn được đặt lịch, gửi thư mời và có phiếu đánh giá. | 9 | 43 |
| Sprint 7 | Offer & onboarding | Một offer được duyệt, gửi đi và ứng viên chấp nhận. | 9 | 42 |
| Sprint 8 | Thông báo & báo cáo | Dashboard tuyển dụng, báo cáo phễu và time-to-hire. | 9 | 44 |
| **Tổng** | **8 Sprint** | | **76** | **350** |

---

## 8. Nền tảng kỹ thuật dự kiến

| Thành phần | Công nghệ/lựa chọn | Ghi chú |
|---|---|---|
| Frontend | React + TypeScript | Giao diện web responsive. |
| Backend | Spring Boot (Java) hoặc NestJS | Chốt theo thế mạnh mentor trước Sprint 1 và không đổi giữa chừng. |
| Cơ sở dữ liệu | PostgreSQL | Hỗ trợ ràng buộc dữ liệu và lịch sử phê duyệt. |
| Xác thực | JWT access token + refresh token | Phục vụ xác thực và quản lý phiên. |
| Lưu trữ tệp | Dịch vụ lưu trữ đối tượng | Lưu CV và tài liệu ứng viên thông qua lớp trừu tượng. |
| Gửi email | SMTP nội bộ + hàng đợi | Gửi thư mời và phản hồi bất đồng bộ, hỗ trợ gửi lại. |

> **Lưu ý:** Backend vẫn là lựa chọn cần nhóm chốt giữa Spring Boot và NestJS theo backlog. Không nên ghi một lựa chọn là công nghệ đã triển khai nếu nhóm chưa quyết định.

---

## 9. Yêu cầu phi chức năng

### Hiệu năng và quy mô

- Danh sách ứng viên trả kết quả dưới 1,5 giây với 20.000 hồ sơ và 200 vị trí.
- Hỗ trợ khoảng 400 người dùng nội bộ.
- Cổng ứng tuyển công khai chịu được 100 lượt nộp hồ sơ mỗi giờ.
- Dashboard tổng quan tải dữ liệu trong dưới 2 giây theo tiêu chí backlog.

### Bảo mật và dữ liệu cá nhân

- Mật khẩu được băm bằng bcrypt.
- Cổng ứng tuyển công khai có cơ chế chống spam và giới hạn tần suất.
- Mọi endpoint phải kiểm tra quyền ở tầng server.
- Hồ sơ ứng viên chỉ được xem bởi recruiter phụ trách và hiring manager của vị trí tương ứng.
- Truy cập dữ liệu ứng viên phải được ghi nhật ký.
- Có chức năng xóa hồ sơ theo yêu cầu của ứng viên.

### Giao diện và vận hành

- Giao diện responsive từ chiều rộng 360px.
- Ngôn ngữ sử dụng: tiếng Việt.
- Múi giờ: `Asia/Ho_Chi_Minh`.
- Đơn vị tiền lương: VND.
- Sao lưu cơ sở dữ liệu và tệp CV hằng ngày, giữ 7 bản gần nhất.

---

## 10. Definition of Ready (DoR)

User Story được đưa vào Sprint khi đáp ứng các điều kiện:

1. Được viết theo mẫu vai trò – hành động – giá trị.
2. Có Acceptance Criteria rõ ràng, kiểm chứng được và không mơ hồ.
3. Đã được ước lượng Story Point và cả nhóm cùng hiểu.
4. Không còn phụ thuộc chặn chưa được xử lý.
5. Có thiết kế giao diện hoặc phác thảo nếu Story có UI.
6. Có kích thước không quá 8 Story Point; Story lớn hơn cần được chia nhỏ.

## 11. Definition of Done (DoD)

User Story chỉ được xem là hoàn thành khi:

1. Tất cả Acceptance Criteria được kiểm chứng và đạt.
2. Mã nguồn được ít nhất một thành viên review và merge vào nhánh tích hợp theo quy định nhóm.
3. Có unit test cho tầng service; độ phủ nhánh mới đạt từ 60% trở lên.
4. CI xanh: build, lint và test đều pass.
5. Đã deploy lên staging và chạy được.
6. Quyền truy cập được kiểm tra ở tầng server, không chỉ ẩn trên giao diện.
7. Giao diện hoạt động đúng ở chiều rộng 360px.
8. Không còn lỗi mức Major trở lên.
9. Product Owner nghiệm thu trên môi trường staging.

---

## 12. Quy trình phát triển và quản lý mã nguồn

### Công cụ quản lý

- **Jira:** Quản lý Epic, User Story, Sub-task, Sprint, Story Point và trạng thái công việc.
- **GitHub:** Quản lý mã nguồn, nhánh, commit, Pull Request và review.
- **CI/CD:** Tự động hóa build, lint, test và triển khai staging theo cấu hình của nhóm.

### Quy ước nhánh

- `main`: Phiên bản ổn định.
- `develop`: Nhánh tích hợp chức năng đang phát triển.
- `feature/<story-id>-<description>`: Phát triển chức năng.
- `fix/<story-id>-<description>`: Sửa lỗi.
- `test/<story-id>-<description>`: Công việc kiểm thử.

### Quy trình làm việc

1. Nhận User Story/Sub-task trên Jira và đọc Description cùng Acceptance Criteria.
2. Thống nhất hợp đồng API giữa FE và BE: URL, phương thức, request/response, mã lỗi và quyền.
3. Tạo nhánh riêng từ `develop`.
4. Phát triển, kiểm thử và commit theo phạm vi nhiệm vụ.
5. Push nhánh lên GitHub và tạo Pull Request vào `develop`.
6. Review mã nguồn, chạy build và test.
7. Merge khi đáp ứng yêu cầu.
8. Cập nhật trạng thái Jira; chỉ chuyển Story sang Done khi đạt DoD.

---

## 13. Rủi ro chính và hướng ứng phó

| Rủi ro | Hướng ứng phó |
|---|---|
| Công việc kỹ thuật ngoài backlog bị bỏ quên | Tạo danh sách công việc kỹ thuật từ Sprint 1 và dành khoảng 15% thời lượng mỗi Sprint cho công việc này. |
| Velocity thực tế thấp hơn 42 Story Point trong hai Sprint đầu | Hiệu chỉnh kế hoạch sau Sprint 2; ưu tiên cắt các Story mức Should và Could khi cần. |
| Luồng phê duyệt nhiều cấp phức tạp hơn ước lượng | Thực hiện spike kỹ thuật trong Sprint 2; cân nhắc luồng duyệt hai cấp cố định nếu cần. |
| Bóc tách CV tiếng Việt không chính xác | Chỉ dùng kết quả bóc tách làm gợi ý để recruiter xác nhận/sửa; không tự ghi đè dữ liệu. |
| Story 8 point không hoàn thành trong Sprint 1 tuần | Chẻ Story lớn thành phần API và UI ngay trong Sprint Planning. |
| Dữ liệu ứng viên bị lộ do thiếu kiểm tra quyền | Kiểm tra ma trận quyền ở tầng server từ Sprint 1 và rà soát bảo mật trước bàn giao. |
| Không có vị trí tuyển dụng thật để chạy pilot | Chuẩn bị bộ dữ liệu mô phỏng cho một vị trí đầy đủ từ Sprint 3. |

---

## 14. Giả định và giới hạn cần xác nhận

Các giả định nghiệp vụ trong backlog cần được Product Owner xác nhận:

- Không đăng tin tự động sang các nền tảng tuyển dụng bên ngoài.
- Không đồng bộ lịch hai chiều với Google Calendar hoặc Outlook.
- Luồng phê duyệt tối đa ba cấp.
- Hạn mức duyệt offer dựa trên dải lương của chức danh.
- Không lưu hợp đồng lao động và không ký số trong hệ thống.
- Ứng viên không cần tài khoản đăng nhập; tra cứu bằng mã và thông tin xác minh.

---

## 15. Cấu trúc repository

```text
TTCS_T926_K5S6_N2/
├── BE/          # Mã nguồn Backend
├── FE/          # Mã nguồn Frontend
└── README.md    # Tài liệu giới thiệu dự án
```

Cấu trúc chi tiết bên trong `BE/` và `FE/` sẽ được bổ sung theo công nghệ và quy ước triển khai thực tế của nhóm.

---

## 16. Trạng thái dự án

- [ ] Khởi tạo repository và cấu trúc thư mục.
- [ ] Thống nhất công nghệ Backend.
- [ ] Hoàn thành các User Story Sprint 1.
- [ ] Kiểm thử và nghiệm thu Sprint 1.
- [ ] Triển khai các Sprint tiếp theo theo kế hoạch.
- [ ] Hoàn thành demo pilot toàn bộ vòng đời tuyển dụng.
- [ ] Hoàn thiện tài liệu và bàn giao.

---

## 17. Tài liệu liên quan

- Product Backlog và Sprint Plan trên Jira.
- Tài liệu phân tích, thiết kế hệ thống.
- Tài liệu đặc tả API Frontend – Backend.
- Tài liệu kiểm thử và nghiệm thu.
- Nhật ký thay đổi và Pull Request trên GitHub.

---

**Hệ thống tuyển dụng nội bộ – `TTCS_T926_K5S6_N2`**  
*Dự án phát triển phần mềm theo Agile/Scrum, quản lý công việc bằng Jira và mã nguồn bằng GitHub.*
