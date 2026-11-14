# Demo 04 Thực hiện dịch vụ

UC-06 · khoảng 1–2 phút.

## Chạy

Mở [UI/index.html](UI/index.html) để thử giao diện. Java cần JDK 17+ và Maven; vào thư mục `Java`:

**Windows:** `run.bat`.

**macOS / Linux:** `sh run.sh`.

UI và Java là hai demo độc lập, dữ liệu trong RAM.

## Kịch bản

1. Chọn lịch đã xác nhận.
2. **Check in** → `CHECKED_IN`.
3. **Start service** → `IN_PROGRESS`.
4. **Complete service** → `COMPLETED`.

Không được nhảy bước hoặc check-in lặp. Java chạy `ServiceDemo`, in kết quả từng bước và các trường hợp bị từ chối.
