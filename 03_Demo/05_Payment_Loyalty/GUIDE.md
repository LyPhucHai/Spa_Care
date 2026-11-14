# Demo 05 Thanh toán và điểm

UC-07, UC-08 · khoảng 3 phút.

## Chạy

Mở [UI/index.html](UI/index.html) để thử giao diện. Java cần JDK 17+ và Maven; vào thư mục `Java`:

**Windows:** `run.bat`.

**macOS / Linux:** `sh run.sh`.

UI và Java là hai demo độc lập, dữ liệu trong RAM.

## Kịch bản

| Tình huống | Kết quả |
|---|---|
| Mai thu 500.000đ, không đổi điểm | +50 điểm, số dư 130, nâng Gold |
| Thu H. thu 900.000đ, Gold, đổi 200 điểm | Tổng 835.000đ, +100 điểm, số dư 320 |
| Linh đổi 900 điểm trên dịch vụ 150.000đ | Từ chối do vượt 50% |
| Linh đổi 600 điểm | Tổng 75.000đ, +11 điểm |
| Thu tiền trước hoàn thành hoặc thu lần hai | Từ chối |

Java chạy `PaymentDemo`. Dữ liệu chi tiêu/điểm đã có sẵn; không dùng các số này làm dữ liệu khởi tạo của bản web.
