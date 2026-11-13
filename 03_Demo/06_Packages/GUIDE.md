# Hướng dẫn demo console tổng hợp

## Chạy chương trình

Từ thư mục gốc repo, chạy `sh run.sh` để dùng H2 lưu file. Lần sau mở lại vẫn còn khách, lịch, hóa đơn và trạng thái khóa tài khoản. `sh run.sh menu` tạo dữ liệu trong RAM. Không mở đồng thời hai console dùng chung file H2.

Đăng nhập `letan01` / `123456`. Xem menu 16 để biết mã dịch vụ; 1 để xem khách; nhân viên mẫu: 1 Lan (massage/gội/da), 2 Hà (da/nail), 3 Minh (gội/cắt).

## Kịch bản nghiệp vụ chính

1. Menu 6, loại 1, khách 2 (Mai), dịch vụ 1, nhân viên 1, Enter để chọn ngày mai, giờ `10:00`. Ghi lại mã lịch A.
2. Menu 6, cùng giờ/nhân viên, khách 1, dịch vụ 2. Bị từ chối do trùng lịch.
3. Menu 8, loại 1, lịch A, `N` khi hỏi ghi đè; chọn ngày mai `14:00`. Giờ cũ được nhả ra. Menu 7 để kiểm tra.
4. Menu 9, lịch A, bước 1 để check-in. Menu 99 đăng xuất.
5. Đăng nhập `ktv_lan`. Menu 7 thấy lịch Lan; menu 9, lịch A, bước 2 rồi bước 3. Menu này không cho Lan check-in hoặc cập nhật lịch của Hà/Minh.
6. Đăng nhập lại `letan01`; menu 10, lịch A, Enter để đổi 0 điểm, Enter để CASH. Hóa đơn 500.000 đ, Mai có 130 điểm, nâng Gold khi đạt 10 triệu chi tiêu mẫu. Thanh toán lần hai bị từ chối.
7. Menu 15, khách 2: kiểm tra lịch sử dịch vụ và tích điểm.
8. Đăng nhập `ql_hoa`; menu 20, nhập khoảng ngày gồm hôm nay: báo cáo tính theo ngày xuất hóa đơn, không phải ngày hẹn.
9. Thoát bằng 0, mở lại `sh run.sh`, tìm khách/lịch để xác nhận lưu file.

Các kết quả số trên giả định DB mẫu mới, chưa chạy thanh toán khác. Muốn khôi phục dữ liệu mẫu: đăng nhập admin, menu 12 và gõ `DONGY`; thao tác này xóa toàn bộ dữ liệu của DB đang mở.

## Chức năng bổ sung

- 2: Thêm khách, kiểm tra điện thoại trùng.
- 3–4: Xem combo, gói bán, gói đã mua và số buổi giữ chỗ.
- 6: Đặt combo hoặc dùng buổi liệu trình đúng chủ sở hữu, đúng dịch vụ, còn hạn/còn buổi.
- 11: Mua gói; thanh toán buổi trả trước không thu lại tiền gói.
- 13–15: Tìm khách theo tên/điện thoại, cập nhật hồ sơ, xem lịch sử.
- 17: Quản lý cập nhật/ngừng bán dịch vụ; giá tại thời điểm đặt của lịch cũ giữ nguyên.
- 18–19: Xem nhân viên/chuyên môn/ca, thêm ca không giao với ca cũ. Không hỗ trợ xóa/thay ca đã phân. Lan/Hà mặc định cả ngày, Minh buổi sáng trong 14 ngày mẫu; có thể thêm ca chiều cho Minh.
- 20: Báo cáo doanh thu đã thanh toán theo khoảng ngày, gồm bán gói.

## Giới hạn cần trình bày đúng

- Đây là ứng dụng Java console một người dùng. Năm UI HTML cũ độc lập, chưa gọi Java. Chưa có web backend hay giao diện đồ họa nối trực tiếp tới console.
- Luồng đặt đôi/nhóm chỉ mô phỏng giá; giữ một nhân viên và một phòng như code gốc, chưa cấp tài nguyên cho từng người.
- Chống trùng lịch đã kiểm tra trên các thao tác tuần tự. Chưa triển khai phục vụ nhiều người đặt đồng thời hoặc các khóa dành cho môi trường web.
- `spending_12m` là số dư chi tiêu mẫu cộng hóa đơn mới; chưa tự tính cửa sổ trượt 12 tháng. Điểm chưa tự hết hạn, chưa có hoàn tiền.
- Chưa tích hợp gửi nhắc lịch, cổng thanh toán, in PDF hóa đơn, chấm công/hoa hồng, tồn kho, đánh giá hay quản lý tài khoản qua menu.
- Thêm/sửa toàn bộ nhân viên, thêm dịch vụ mới, sửa cấu hình combo/gói/hạng chưa có menu; dữ liệu mẫu và SQL là nguồn cấu hình hiện tại.
- Bộ 01–05 được giữ cho thuyết trình cũ. Tính năng bảo mật mới nằm ở bộ 06; không tuyên bố toàn bộ 5 demo cũ đã dùng PBKDF2 và khóa 15 phút.

## Kiểm thử

Từ gốc repo chạy `sh run.sh test`: 36 phép kiểm tra tính giá + 59 phép kiểm tra nghiệp vụ. Bộ test dùng DB riêng trong RAM, không xóa DB saved. Ngoài bộ test đã kiểm tra lưu khách và khóa tài khoản qua các tiến trình Java độc lập, và build/chạy lại cả 5 demo gốc bằng source.
