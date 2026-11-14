# Demo 01 Đăng nhập

Kiểm tra đăng nhập, vai trò và khóa sau 5 lần sai. UC-01 · khoảng 1–2 phút.

## Chạy

Mở [UI/index.html](UI/index.html) để thử giao diện. Java cần JDK 17+ và Maven; vào thư mục `Java`:

**Windows:** `run.bat`.

**macOS / Linux:** `sh run.sh`.

UI và Java là hai demo độc lập, dữ liệu trong RAM.

## Kịch bản

Mật khẩu mẫu: `123456`.

| Thao tác | Kết quả |
|---|---|
| Đăng nhập `letan01` | Vai trò lễ tân |
| Đăng nhập `ktv_lan` | Vai trò kỹ thuật viên |
| Đăng nhập `ql_hoa` | Vai trò quản lý |
| Nhập sai `admin` 5 lần | Tài khoản bị khóa trong lần chạy demo |
| Nhập đúng sau khi khóa | Vẫn bị từ chối |

Java chạy class `LoginDemo`; tài khoản không tồn tại trả về `null`. Demo này dùng SHA-256 với tiền tố cố định và bộ đếm khóa trong RAM. Bản web có PBKDF2 và khóa 15 phút lưu DB; không nhầm hai cách triển khai.
