<<<<<<< HEAD
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
=======
# Demo 6 - Gói dịch vụ + tích điểm (combo, liệu trình, đôi/nhóm, trải nghiệm lần đầu)

**Chứng minh được:** hệ thống vẫn tích điểm và nâng hạng như cũ, đồng thời bán và áp dụng 4 loại gói; mỗi hóa đơn chỉ áp MỘT ưu đãi gói tốt nhất; buổi liệu trình được giữ chỗ khi đặt lịch và chỉ trừ khi thanh toán.
**Thời gian:** khoảng 4 phút (kịch bản tự động) hoặc tự nhập bằng menu.
**Use case liên quan:** UC-04, UC-07, UC-08, UC-12 (liệu trình) và các ưu đãi mới (combo, đôi/nhóm, lần đầu).

## Cách chạy (Windows, không cần Maven)

Trong thư mục `Java`:

| Lệnh | Việc |
|---|---|
| `.\run_nomaven.bat check` | Kiểm tra luật tính giá (36 phép kiểm tra, không cần CSDL) |
| `.\run_nomaven.bat demo` | Chạy kịch bản tự động A đến E (bảng bên dưới) |
| `.\run_nomaven.bat` | Menu tự nhập, dữ liệu trong RAM (thoát là mất) |
| `.\run_nomaven.bat saved` | Menu tự nhập, lưu ra `data\spacare` (chạy lại vẫn còn) |

Có Maven thì dùng `run.bat`, `run_demo.bat`, `run_saved.bat`. Lần đầu cần mạng để tải H2. Cần JDK 17 trở lên.

## Luật ưu đãi (giả định của nhóm, cấu hình được trong data.sql)

Thứ tự tính trên mỗi hóa đơn:
1. Buổi trả bằng gói liệu trình: không tính tiền.
2. Chọn **một** ưu đãi gói tốt nhất trong: Combo (30-50%), Đôi (tính tiền 1 người), Nhóm 3-4 người (-20%), nhóm từ 5 người (-30%), Trải nghiệm lần đầu (50-80% theo dịch vụ, chỉ khách mới, 1 khách).
3. Giảm theo hạng thành viên (Silver 0%, Gold 5%, Platinum 10%).
4. Đổi điểm: 100 điểm = 10.000 đ, tối đa 50% giá trị còn lại, số điểm đổi phải là bội số của 100.
5. Tích điểm = tổng tiền / 10.000 nhân hệ số hạng (1.0 / 1.2 / 1.5), làm tròn xuống. Chi tiêu tích lũy vượt 10 triệu lên Gold, 30 triệu lên Platinum.

## Kịch bản tự động (`demo`) và kết quả mong đợi

Các con số dưới đây được tính tay và kiểm tra bằng `PricingCheck`; phần ghi CSDL cần bạn chạy `demo` để xác nhận.

| Bước | Việc | Kết quả mong đợi |
|---|---|---|
| A | Mai (Silver) đặt Combo Thư giãn (-40%) với Lan 09:00, hoàn thành, thanh toán | 1.050.000 - 420.000 = **630.000 đ**, +63 điểm (số dư 143), chi tiêu 10.130.000 -> **NÂNG HẠNG GOLD** |
| B1 | Mai dùng gói của Thu H | **[BỊ TỪ CHỐI]** gói không thuộc Mai |
| B2 | Thu H dùng Thẻ Massage cho lịch Gội đầu | **[BỊ TỪ CHỐI]** sai dịch vụ của gói |
| B3 | Thu H đặt Massage 14:00 dùng buổi gói #1 | Thành công, gói còn 6/10 nhưng đang giữ 1 buổi |
| B4 | Hoàn thành và thanh toán | **0 đ**, không tích điểm, gói còn **5/10** |
| C | Linh (Platinum) mua Liệu trình trị mụn 5 buổi (1.200.000 đ), rồi đặt lịch dùng gói (gói của Linh là #2) | +180 điểm (số dư 1.080), chi tiêu 36.200.000; gói còn 5/5, đang giữ 1 buổi |
| D1 | Thu H đi đôi: Da cơ bản 400.000 x2, Hà 10:00 | 800.000 - đôi 400.000 - Gold 20.000 = **380.000 đ**, +45 điểm (số dư 465) |
| D2 | Linh đi nhóm 3 người: Gội đầu 150.000 x3, Minh 08:00 | 450.000 - nhóm 90.000 - Platinum 36.000 = **324.000 đ**, +48 điểm (số dư 1.128) |
| E1 | Thêm khách mới Phạm An | Hạng Silver, **KHÁCH MỚI** |
| E2 | Phạm An đặt Da cơ bản 400.000, Hà 13:00 | Trải nghiệm 70% = 280.000, trả **120.000 đ**, +12 điểm |
| E3 | Phạm An quay lại Sơn gel 250.000, Hà 14:30 | **Hết** ưu đãi lần đầu, trả đủ **250.000 đ**, +25 điểm (số dư 37) |

## Thử "lần sau vào vẫn còn" (chế độ lưu file)

1. `.\run_nomaven.bat saved`, chọn 11 (mua gói), rồi chọn 6 (đặt lịch), thoát bằng 0.
2. Chạy lại `.\run_nomaven.bat saved`: menu 4 và 7 vẫn thấy gói và lịch vừa tạo.
3. Muốn về dữ liệu mẫu: menu 12 (gõ DONGY) hoặc xóa thư mục `data`.
4. Chế độ lưu file mở khóa file CSDL, nên chỉ chạy một cửa sổ tại một thời điểm. Đừng chạy `demo` ở chế độ này.
5. Khi thuyết trình nên dùng chế độ RAM để mỗi lần chạy đều sạch dữ liệu.

## Giả định và hạn chế (nên nói thẳng khi bị hỏi)

- Lịch nhiều khách (đôi/nhóm) chỉ giữ 1 nhân viên và 1 phòng; số khách chỉ ảnh hưởng giá. Bản thật cần giữ đủ nhân viên và phòng cho từng người.
- Combo và buổi liệu trình không dùng chung trong một lịch; buổi liệu trình chỉ dùng cho 1 khách.
- Combo đặt với đúng một nhân viên có đủ chuyên môn cho mọi dịch vụ trong combo.
- Mua gói không được giảm thêm nhưng vẫn tích điểm và cộng chi tiêu.
- Bảng gói trong code tên `service_package` (tránh từ khóa); báo cáo cần đổi tên tương ứng khi cập nhật ERD.
- Đổi điểm nay yêu cầu bội số của 100 (bản cũ cho nhập tùy ý và mất phần dư).
>>>>>>> 477f45f (Lưu thay đổi tạm thời của tôi)
