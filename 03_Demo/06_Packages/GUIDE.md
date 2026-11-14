# Demo 06 Console tổng hợp

Đặt lịch, combo, liệu trình, thanh toán và thành viên trong một menu Java. Xem [cách chạy](Java/README.md).

## Luồng chính

1. Đăng nhập `letan01` / `123456`.
2. Menu **6**: đặt Massage, Mai, Lan, ngày mai 10:00; ghi mã lịch.
3. Thử đặt trùng Lan → bị từ chối.
4. Menu **8**: dời sang giờ trống hoặc hủy.
5. Menu **9**: check-in → bắt đầu → hoàn thành.
6. Menu **10**: thu tiền, 0 điểm, CASH.
7. Menu **15**: xem lịch sử dịch vụ và điểm.
8. `ql_hoa` dùng menu **20** để xem doanh thu.

## Các menu khác

| Menu | Chức năng |
|---|---|
| 1–2 | Xem/thêm khách |
| 3–4 | Ưu đãi và gói đã mua |
| 5–8 | Tìm giờ, đặt, xem, dời/hủy lịch |
| 9–11 | Thực hiện, thanh toán, bán gói |
| 12 | Admin đặt lại dữ liệu, cần gõ DONGY |
| 13–16 | Tìm/sửa khách, lịch sử, dịch vụ |
| 17–20 | Sửa dịch vụ, nhân viên/ca, phân ca, doanh thu |
| 99 / 0 | Đăng xuất / thoát |

## Giới hạn

Console và web dùng DB riêng. Đặt nhóm ở console chỉ mô phỏng giá, không cấp tài nguyên riêng từng người. Chi tiêu `spending_12m` ở console là số mẫu cộng dồn; kho, nhắc lịch và một số màn hình chỉ có trong bản web.

Menu 12 xóa dữ liệu của DB đang mở. Chạy chế độ RAM nếu chỉ muốn thử nhanh.
