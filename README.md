# SpaCare

Đồ án môn Requirements Analysis and Design: quản lý đặt lịch spa, thực hiện dịch vụ, thanh toán và khách hàng thân thiết bằng Java. Repo gồm báo cáo, slide, prototype HTML và Java console dùng H2..

| Tài khoản | Mật khẩu mẫu | Quyền |
|---|---|---|
| admin | 123456 | Quản trị, đặt lại dữ liệu mẫu |
| ql_hoa | 123456 | Quản lý dịch vụ/ca làm/báo cáo, nghiệp vụ lễ tân |
| letan01 | 123456 | Khách hàng, lịch hẹn, check-in, thanh toán, bán gói |
| ktv_lan | 123456 | Xem lịch được giao, bắt đầu và hoàn thành dịch vụ |

Mật khẩu console tổng hợp được băm PBKDF2 với salt ngẫu nhiên. Sau 5 lần sai, khóa 15 phút, trạng thái lưu vào DB. Khi có terminal tương tác hỗ trợ, mật khẩu không hiển thị lúc nhập; trong IDE không có console hệ thống sẽ dùng dòng nhập thông thường.

## Nội dung repo

| Thư mục | Nội dung |
|---|---|
| `01_Report` | Báo cáo Word/PDF từ repo gốc, chưa chỉnh theo code mới |
| `02_Slides` | Slide thuyết trình gốc |
| `03_Demo/01_Login` đến `05_Payment_Loyalty` | Năm bộ UI HTML và Java demo độc lập |
| `03_Demo/06_Packages` | Java console tổng hợp đã bổ sung đăng nhập, phân quyền, quản lý và kiểm thử |
| `04_Database` | Schema/dữ liệu minh họa của bộ demo cũ; console 06 dùng resources riêng |
| `05_Presentation_Script` | Kịch bản thuyết trình gốc |
| `06_QA_Preparation` | Câu hỏi chuẩn bị gốc |
| `07_Diagrams` | Use Case, ERD, Sequence, Activity |

Đọc [hướng dẫn console tổng hợp](03_Demo/06_Packages/GUIDE.md) để demo theo vai trò, xem lệnh Windows và giới hạn thực tế. Prototype cũ mở trực tiếp `03_Demo/0X_*/UI/index.html` bằng trình duyệt; dữ liệu HTML và Java độc lập.
