# Chức năng

Bản web hiện tại. SMS/Zalo/email và cổng ngân hàng đang mô phỏng.

| Mã | Chức năng |
|---|---|
| FR-01 | Đăng nhập, đăng ký khách, phân quyền 5 vai trò; khóa 15 phút sau 5 lần sai |
| FR-02 | Thêm, tìm, sửa, ngừng hồ sơ khách; lưu dị ứng và lịch sử |
| FR-03 | Quản lý dịch vụ, giá, thời lượng, loại phòng và combo |
| FR-04 | Đặt lịch tại quầy/online; chọn chi nhánh, nhân viên hoặc tự phân công |
| FR-05 | Tìm giờ trống; kiểm tra chuyên môn, ca, phòng và lịch giao nhau |
| FR-06 | Dời/hủy trước 2 giờ; lễ tân ghi đè kèm lý do; cảnh báo trùng giờ khách |
| FR-07 | Xác nhận, thay đổi, hủy và nhắc lịch trước 24 giờ; lưu nhật ký mô phỏng |
| FR-08 | Check-in, bắt đầu, hoàn thành; vắng mặt thủ công hoặc tự động sau kết thúc +15 phút |
| FR-09 | Hồ sơ nhân viên, chuyên môn và tài khoản kỹ thuật viên |
| FR-10 | Phân ca; chặn ca giao nhau và xóa ca đang có lịch |
| FR-11 | Chấm công vào/ra; hoa hồng dịch vụ và bán lẻ |
| FR-12 | Báo giá, thêm sản phẩm, thanh toán, chống thu trùng; in/lưu PDF |
| FR-13 | Tích, đổi, hết hạn điểm; sổ giao dịch EARN/REDEEM/EXPIRE |
| FR-14 | Cấu hình hạng; xác định hạng từ chi tiêu thực thu trong 12 tháng |
| FR-15 | Voucher có điều kiện/lượt dùng; đổi điểm tối đa 50% sau ưu đãi |
| FR-16 | Bán gói; kiểm tra chủ sở hữu, dịch vụ, buổi và hạn; giữ/nhả buổi |
| FR-17 | Kho, định mức, nhập/xuất, cảnh báo thấp; không cho âm tồn |
| FR-18 | Khách đánh giá lịch hoàn thành của mình, mỗi lịch một lần |
| FR-19 | Doanh thu, dịch vụ, hoa hồng, vắng mặt, khách mới/thân thiết; xuất CSV |

## Quy tắc

- Giá và thời lượng đã đặt giữ nguyên khi sửa dịch vụ.
- Combo/nhóm và voucher chọn mức giảm tốt hơn, sau đó giảm theo hạng và đổi điểm.
- 100 điểm = 10.000đ; điểm có hạn 12 tháng. Tiền lưu bằng số nguyên VND.
- Nhóm tạo lịch và tài nguyên riêng cho từng người, dùng hồ sơ người đại diện.
- Đặt giữ buổi gói; hủy/vắng nhả giữ; thanh toán mới trừ buổi chính thức.
- Hoàn thành trừ vật tư. Thanh toán ghi điểm, hoa hồng và hàng bán lẻ trong cùng giao dịch.

## Giới hạn

- Kho đang dùng chung giữa các chi nhánh.
- Chưa có hoàn tiền, đảo điểm và phân bổ hoa hồng gói theo từng buổi.
- Ưu đãi nhóm lưu lúc đặt; lễ tân cần kiểm tra lại nếu thành viên nhóm thay đổi.
- Chưa gửi tin hoặc đối soát ngân hàng thật; bản in không phải hóa đơn thuế.
- Màn hình giới hạn số bản ghi; cần phân trang nếu dữ liệu lớn.
- Khóa giao dịch áp dụng trong một tiến trình Java, chưa triển khai nhiều instance.
- Chưa chứng minh SLA 99%, tải dài hạn hoặc tuân thủ pháp lý bằng demo.

Xem [checklist kiểm thử](../SpaCare_Test_Checklist.md) để đối chiếu từng nghiệp vụ.
