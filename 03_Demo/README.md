# Demo độc lập

Các demo trước bản web. Mỗi bộ có UI HTML và chương trình Java riêng, không chia sẻ dữ liệu với web trong `app/`.

| Demo | Nội dung | Hướng dẫn |
|---|---|---|
| 01 | Đăng nhập, vai trò, khóa tài khoản | [Mở](01_Login/GUIDE.md) |
| 02 | Đặt lịch, kiểm tra xung đột | [Mở](02_Booking_Conflict/GUIDE.md) |
| 03 | Dời và hủy lịch | [Mở](03_Reschedule_Cancel/GUIDE.md) |
| 04 | Check-in, thực hiện dịch vụ | [Mở](04_CheckIn_Service/GUIDE.md) |
| 05 | Thanh toán, điểm, nâng hạng | [Mở](05_Payment_Loyalty/GUIDE.md) |
| 06 | Console tổng hợp, combo, liệu trình | [Mở](06_Packages/GUIDE.md) |

## Chạy

Demo 01–05 cần **JDK 17+ và Maven**. Từ thư mục `Java` của demo:

**Windows:** chạy `run.bat`.

**macOS / Linux:** chạy `sh run.sh`.

UI mở trực tiếp `UI/index.html`. Mỗi lần mở lại UI hoặc chạy Java, dữ liệu mẫu được tạo lại trong RAM. Maven cần Internet ở lần tải dependency đầu.

Dòng `[BỊ TỪ CHỐI]` là kết quả mong đợi của tình huống sai. Demo 06 có chế độ lưu file riêng.

Để chạy sản phẩm web tích hợp, xem [README chính](../README.md).
