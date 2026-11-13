# SpaCare Java console

Yêu cầu JDK 17+. Dùng `sh run.sh saved` trên Linux/macOS để mở ứng dụng có đăng nhập và lưu dữ liệu. Có Maven thì dùng Maven; không có Maven thì biên dịch trực tiếp bằng JDK, dùng H2 đã có ở demo 02.

| Lệnh Linux/macOS | Tác dụng |
|---|---|
| `sh run.sh saved` | Menu tương tác, lưu vào `data/spacare.mv.db` |
| `sh run.sh menu` | Menu tương tác, dữ liệu tạm trong RAM |
| `sh run.sh demo` | Kịch bản tự động combo, liệu trình, điểm |
| `sh run.sh check` | 36 kiểm tra tính giá độc lập |
| `sh run.sh test` | Kiểm tra tính giá và 59 kiểm tra nghiệp vụ trên H2 trong RAM |

Windows: `run_saved.bat` (Maven) hoặc `run_nomaven.bat saved` (JDK, tải H2 lần đầu). Chạy từ thư mục Java. Các script Windows được giữ từ repo gốc, chưa kiểm thử trên Windows trong lần cập nhật này.

Tài khoản: `admin`, `ql_hoa`, `letan01`, `ktv_lan`; mật khẩu mẫu `123456`. Quản lý xem doanh thu, sửa dịch vụ, phân ca. Lễ tân quản lý khách/lịch/thanh toán. Kỹ thuật viên chỉ xem lịch được giao và bắt đầu/hoàn thành dịch vụ. Chỉ admin đặt lại dữ liệu mẫu, sau khi gõ `DONGY`.

Dữ liệu H2 của console phiên bản trước được thêm cột tài khoản tự động. Mật khẩu SHA-256 cũ được chuyển PBKDF2 sau lần đăng nhập đúng. Không dùng file DB của demo 01–05 làm file DB của demo 06 vì schema khác nhau.

Xem kịch bản và giới hạn ở `../GUIDE.md`. Bộ kiểm tra nghiệp vụ ở `src/test/java/spacare/WorkflowCheck.java`, chạy bằng `sh run.sh test` (không phải JUnit). Kịch bản tự động dùng tài khoản admin mẫu trên DB RAM mới.
