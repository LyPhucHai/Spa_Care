# Demo nhanh

Dùng bản web ở thư mục `app`. Checklist đầy đủ: [117 test case](../SpaCare_Test_Checklist.md).

## Chuẩn bị

**Windows — Command Prompt, từ gốc repo**

```bat
set SPACARE_DB=./data/spacare_manual_test
set PORT=8081
start-web.bat
```

**macOS / Linux**

```bash
SPACARE_DB=./data/spacare_manual_test PORT=8081 sh start-web.sh
```

Mở **http://127.0.0.1:8081**. Dùng cửa sổ thường và riêng tư cho hai vai trò; các tab thường dùng chung đăng nhập.

## Luồng chính

- [ ] Lễ tân `letan01` / `123456`: thêm khách hoặc dùng Nguyễn Thu Hà.
- [ ] Đặt Massage, Lan, Quận 1, **hôm nay, giờ còn trống**; ghi mã lịch.
- [ ] Đặt khách khác cùng Lan/cùng giờ → bị từ chối.
- [ ] Lễ tân check-in; `ktv_lan` cập nhật dữ liệu, bắt đầu và hoàn thành.
- [ ] Kho giảm 10 ml tinh dầu theo định mức mẫu.
- [ ] Lễ tân thu tiền, không voucher/điểm/sản phẩm: 500.000đ, Silver tích 50 điểm.
- [ ] Không thanh toán cùng lịch lần hai; hóa đơn in đúng nội dung.
- [ ] `khach01` xem hóa đơn và đánh giá nếu lịch thuộc Nguyễn Thu Hà.
- [ ] `ql_hoa` đối chiếu doanh thu, kho và hoa hồng.
- [ ] Tắt/mở server với cùng DB: dữ liệu giữ nguyên.

Các số trên áp dụng cho khách Silver và cấu hình mẫu chưa thay đổi. Chỉ check-in đúng ngày hẹn.

## Kiểm tra lỗi

- [ ] Dời vào giờ bận, hủy không lý do, khách đổi sát giờ → bị chặn.
- [ ] Nhân viên sai chuyên môn/chi nhánh, ngoài ca hoặc thiếu phòng → không nhận lịch.
- [ ] Gói sai chủ, sai dịch vụ, hết buổi/hạn → bị chặn.
- [ ] Thiếu vật tư → không hoàn thành; bổ sung rồi làm lại không trừ lặp.
- [ ] Bán lẻ vượt tồn → rollback toàn bộ thanh toán.
- [ ] Tài khoản khác không sửa lịch hoặc xem dữ liệu riêng của khách.

Ghi mã lịch/hóa đơn và thông báo khi có lỗi. Kiểm tra quyền API, sao lưu và HTTPS trong [checklist đầy đủ](../SpaCare_Test_Checklist.md).
