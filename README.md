<h1 align="center">SpaCare</h1>

<p align="center">Quản lý lịch hẹn, dịch vụ và khách hàng spa.</p>

<p align="center">Java 17+ · H2 · Giao diện tiếng Việt</p>

<p align="center">
  <a href="#cài-đặt">Cài đặt</a> ·
  <a href="#tài-khoản-demo">Tài khoản</a> ·
  <a href="SpaCare_Test_Checklist.md">Kiểm thử</a>
</p>

---

## Giới thiệu

SpaCare kết nối đặt lịch, thực hiện dịch vụ, thanh toán, điểm thành viên và kho trong một ứng dụng web. Dữ liệu lưu trên máy; giao diện dùng trực tiếp API Java.

## Chức năng

- Đặt, dời, hủy lịch; kiểm tra nhân viên, phòng và ca làm.
- Quản lý khách hàng, nhân viên, chi nhánh và dịch vụ.
- Thanh toán, hóa đơn, combo, liệu trình và điểm thành viên.
- Kho, chấm công, hoa hồng, đánh giá và báo cáo.

## Cài đặt

Cần **JDK 17 trở lên**. Không cần Maven, Node.js hoặc cài database riêng. Giải nén source và mở terminal trong thư mục `Spa_Care` có thư mục `app`.

### Windows

Cài JDK, thêm thư mục `bin` của JDK vào `PATH`, sau đó kiểm tra trong Command Prompt:

```bat
java -version
javac -version
start-web.bat
```

Có thể mở trực tiếp `start-web.bat` từ File Explorer.

### macOS / Linux

Cài JDK 17+, sau đó chạy:

```bash
java -version
sh start-web.sh
```

Trên Arch Linux: `sudo pacman -S --needed jdk21-openjdk`.

Mở **http://127.0.0.1:8080**. Nhấn `Ctrl+C` tại terminal để dừng server.

## Tài khoản demo

Mật khẩu chung: **123456**.

| Tài khoản | Vai trò |
|---|---|
| `admin` | Quản trị |
| `ql_hoa` | Quản lý |
| `letan01` | Lễ tân |
| `ktv_lan` | Kỹ thuật viên Lan |
| `khach01` | Khách hàng |

Khách mới có thể đăng ký. Mật khẩu tài khoản mới cần ít nhất 8 ký tự.

## Đổi cổng

Nếu cổng 8080 đang được dùng:

**Windows — Command Prompt**

```bat
set PORT=8081
start-web.bat
```

**macOS / Linux**

```bash
PORT=8081 sh start-web.sh
```

Mở **http://127.0.0.1:8081**. Database được giữ lại khi khởi động lại.

## Tài liệu

| Nội dung | Tệp |
|---|---|
| Cấu hình, kiểm thử, sao lưu | [app/README.md](app/README.md) |
| Chức năng và giới hạn | [app/FEATURES.md](app/FEATURES.md) |
| Demo nhanh | [app/MANUAL_TESTS.md](app/MANUAL_TESTS.md) |
| 117 test case | [SpaCare_Test_Checklist.md](SpaCare_Test_Checklist.md) |
| Demo độc lập trước đây | [03_Demo/README.md](03_Demo/README.md) |

SMS/Zalo/email và cổng ngân hàng đang mô phỏng. Bản in hóa đơn là chứng từ demo. Báo cáo và slide cũ chưa cập nhật theo bản web.
