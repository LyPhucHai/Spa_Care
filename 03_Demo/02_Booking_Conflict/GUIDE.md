# Demo 02 Đặt lịch

Kiểm tra thời gian, chuyên môn và ca làm. UC-04, UC-17 · khoảng 3 phút.

## Chạy

Mở [UI/index.html](UI/index.html) để thử giao diện. Java cần JDK 17+ và Maven; vào thư mục `Java`:

**Windows:** `run.bat`.

**macOS / Linux:** `sh run.sh`.

UI và Java là hai demo độc lập, dữ liệu trong RAM.

## Kịch bản

| Thao tác | Kết quả |
|---|---|
| Mai đặt Massage với Lan lúc 10:00 | Thành công |
| Khách khác đặt Lan lúc 10:00 | Từ chối do trùng |
| Đặt Gội đầu với Lan lúc 11:30 | Thành công, liền sau Massage |
| Chọn Minh làm Massage | Từ chối do sai chuyên môn |
| Chọn Minh lúc 15:00 | Từ chối do ngoài ca mẫu |
| Gội + Cắt với Minh lúc 08:00 | Thành công, tổng 105 phút |

Java chạy `BookingDemo`, dùng ngày mai. Minh chỉ làm ca sáng trong demo này; bản web có dữ liệu ca riêng.
