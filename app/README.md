# SpaCare Web

Backend Java, giao diện HTML/CSS/JavaScript và H2 lưu file. Xem [README chính](../README.md) để cài JDK và đăng nhập.

## Chạy ứng dụng

Các lệnh dưới chạy từ thư mục gốc `Spa_Care`.

### Windows

```bat
start-web.bat
```

Đổi cổng trong Command Prompt:

```bat
set PORT=8081
start-web.bat
```

Nếu dùng PowerShell: `$env:PORT="8081"; .\start-web.bat`.

### macOS / Linux

```bash
sh start-web.sh
```

Đổi cổng: `PORT=8081 sh start-web.sh`.

Mặc định: **http://127.0.0.1:8080**. Database tạo ở `app/data/spacare_web.mv.db`; múi giờ `Asia/Ho_Chi_Minh`.

## Cấu hình

| Biến môi trường | Mặc định | Tác dụng |
|---|---|---|
| `PORT` | `8080` | Cổng web |
| `SPACARE_HOST` | `127.0.0.1` | Địa chỉ lắng nghe; dùng `0.0.0.0` nếu cần truy cập LAN |
| `SPACARE_DB` | `./data/spacare_web` | Đường dẫn DB tính từ `app`, không thêm `.mv.db` |
| `SPACARE_BACKUP_DIR` | `./backups` | Thư mục backup |
| `SPACARE_TLS_KEYSTORE` | Chưa đặt | Keystore PKCS12 để bật HTTPS |
| `SPACARE_TLS_PASSWORD` | Chưa đặt | Mật khẩu keystore |

Ca mẫu có hiệu lực 21 ngày từ lần tạo DB. Sau đó quản lý phân thêm ca trên giao diện.

## Kiểm thử

Cần thêm **Python 3**, `keytool` của JDK và cổng `18089` đang trống. Test dùng DB tạm, không sửa dữ liệu ứng dụng.

**Windows — Command Prompt**

```bat
cd app
javac -encoding UTF-8 -source 17 -target 17 -d target\classes src\main\java\spacare\web\*.java
xcopy /E /I /Y src\main\resources target\classes >nul
python tests\acceptance.py
```

Chạy ứng dụng một lần trước để có thư mục `target/classes`; sau đó có thể dùng các lệnh trên để build và test lại.

**macOS / Linux**

```bash
sh app/test.sh
```

Kết quả của bản đã kiểm tra: `PASS 186 acceptance checks`. Hướng dẫn test tay: [checklist](../SpaCare_Test_Checklist.md).

<details>
<summary>Kiểm thử giao diện tùy chọn</summary>

Cần Node.js. Từ thư mục `app`:

```bash
npm install
npx playwright install chromium
npm run test:e2e
```

Có thể đặt `CHROMIUM_PATH` nếu dùng Chromium đã cài. Bộ này dùng cổng `18090` và DB trong RAM.

</details>

## Sao lưu

Admin → **Hồ sơ & cài đặt → Sao lưu dữ liệu** để tải ZIP.

Worker chạy 30 giây/lần, tạo snapshot ít nhất một lần mỗi ngày khi server đang hoạt động. File nằm trong `app/backups/`.

### Khôi phục vào thư mục riêng

Dừng server trước. Từ thư mục `app`, chạy lệnh sau; trên Windows dùng Command Prompt, trên macOS/Linux dùng terminal:

```text
java -cp lib/h2-2.2.224.jar org.h2.tools.Restore -file "DUONG_DAN_BACKUP.zip" -dir restored -db spacare_web
```

Mở bản khôi phục, giữ nguyên DB gốc:

**Windows — Command Prompt**

```bat
set SPACARE_DB=./restored/spacare_web
run.bat
```

**macOS / Linux**

```bash
SPACARE_DB=./restored/spacare_web sh run.sh
```

Đối chiếu khách, lịch, hóa đơn và tồn trước khi dùng DB khôi phục làm bản chính.

## HTTPS

**Windows:** cung cấp keystore PKCS12 qua ba biến `SPACARE_TLS_KEYSTORE`, `SPACARE_TLS_PASSWORD`, `PORT`, rồi chạy `start-web.bat`. Không đặt mật khẩu keystore vào Git.

**macOS / Linux:** `sh start-https.sh` tự tạo chứng chỉ demo trong `app/certs/`; mở **https://localhost:8443**. Đây là chứng chỉ tự ký, trình duyệt có thể yêu cầu xác nhận.

## Mã nguồn

| Tệp | Nhiệm vụ |
|---|---|
| `SpaCareServer.java` | HTTP/HTTPS, phiên, CSRF, worker |
| `Business.java` | Nghiệp vụ và phân quyền |
| `Database.java` | JDBC, schema và dữ liệu mẫu |
| `Security.java` | Mật khẩu và token |
| `Backup.java` | Snapshot H2 |
| `Json.java` | Đọc/ghi JSON |
| `public/app.js` | Giao diện và gọi API |

Java nằm trong `src/main/java/spacare/web/`; giao diện và schema trong `src/main/resources/`.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `java` / `javac` không tìm thấy | Cài JDK và kiểm tra `PATH` |
| `Address already in use` | Dừng server cũ hoặc đổi `PORT` |
| Không có giờ trống | Kiểm tra ca, chi nhánh, chuyên môn và loại phòng |
| Không check-in được | Chọn lịch của hôm nay |
| Không hoàn thành được | Kiểm tra vật tư và định mức |
| Tài khoản khóa | Chờ 15 phút hoặc nhờ admin mở khóa |

Xem thêm [chức năng và giới hạn](FEATURES.md), [thành phần bên thứ ba](THIRD_PARTY.md).
