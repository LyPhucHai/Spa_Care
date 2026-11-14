# Câu hỏi chuẩn bị

Các câu trả lời dưới áp dụng cho bản web trong `app`.

**Vì sao chọn các chức năng này?**

Để xử lý lịch trùng, theo dõi dịch vụ, thu tiền và quyền lợi khách. Liên kết từng chức năng với [FR tương ứng](../app/FEATURES.md).

**Chặn trùng lịch bằng cách nào?**

Kiểm tra khoảng thời gian, ca, chuyên môn, chi nhánh và phòng. Các thay đổi được tuần tự hóa trong một tiến trình Java rồi commit/rollback nguyên khối.

**Vì sao tách `appointment_item`?**

Một lịch có nhiều dịch vụ; mỗi dòng có nhân viên, phòng, thời gian và giá lúc đặt riêng.

**Vì sao lưu giá lúc đặt?**

Sửa bảng giá không làm thay đổi lịch cũ và số tiền của lịch sử.

**Điểm và hạng được tính thế nào?**

Tích từ tiền thực thu theo hệ số hạng. Điểm có hạn 12 tháng; hạng dựa vào hóa đơn thực thu trong 12 tháng gần nhất.

**Dùng liệu trình có thu tiền lần nữa không?**

Buổi gói được giữ khi đặt, nhả khi hủy/vắng, trừ khi thanh toán; không thu lại giá dịch vụ đã trả trước.

**Thanh toán lỗi thì sao?**

Rollback hóa đơn, điểm, buổi gói, tồn bán lẻ và hoa hồng của giao dịch.

**Bảo mật gồm gì?**

PBKDF2, cookie HttpOnly/SameSite, CSRF, kiểm tra quyền/chủ sở hữu ở API, khóa 15 phút sau 5 lần sai. HTTPS tùy chọn dùng cookie Secure.

**Vì sao dùng H2?**

Chạy nhúng, lưu file và không cần cài DB riêng. Chuyển DB khác cần rà lại schema/SQL, không chỉ thay URL JDBC.

**Đã kiểm thử gì?**

Bộ API có 186 kiểm tra; kiểm thử trình duyệt bao gồm các vai trò và luồng chính. Kết quả trên máy trình bày cần chạy lại và ghi nhận riêng.

**Giới hạn hiện tại?**

Thông báo/ngân hàng mô phỏng, kho chung chi nhánh, chưa có hoàn tiền hoặc nhiều instance. Chưa chứng minh SLA 99% hay tuân thủ pháp lý chỉ bằng demo.

**Mỗi thành viên làm gì?**

Trả lời theo công việc thật và lịch sử Git. Không dùng bảng phân công cũ nếu đã thay đổi phạm vi.
