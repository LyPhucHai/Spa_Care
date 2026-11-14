# Demo 03 Dời và hủy lịch

UC-05 · khoảng 2 phút.

## Chạy

Mở [UI/index.html](UI/index.html) để thử giao diện. Java cần JDK 17+ và Maven; vào thư mục `Java`:

**Windows:** `run.bat`.

**macOS / Linux:** `sh run.sh`.

UI và Java là hai demo độc lập, dữ liệu trong RAM.

## Kịch bản

| Thao tác | Kết quả |
|---|---|
| Dời lịch Mai vào 14:00 đang bận | Từ chối |
| Dời sang 16:00 | Thành công, nhả giờ cũ |
| Hủy lịch Thu H. | CANCELLED, nhả giờ |
| Hủy lại hoặc hủy lịch đã check-in | Từ chối |

Java chạy `RescheduleDemo`. Kết quả cuối của kịch bản Java: lịch 1 lúc 15:30, lịch 2 CANCELLED, lịch 3 CHECKED_IN.

Luật báo trước 2 giờ nằm ở `MIN_NOTICE_HOURS`; demo cho phép truyền cờ ghi đè. Không coi đó là bằng chứng toàn bộ phân quyền đã được tích hợp.
