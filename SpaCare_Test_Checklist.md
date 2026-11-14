# Kiểm thử SpaCare

117 test case cho bản web. Chỉ đánh dấu khi đã thao tác và đối chiếu kết quả; giữ trống các case chưa chạy.

## Chạy riêng database test

Từ thư mục gốc `Spa_Care`, dừng server cũ bằng Ctrl+C trước khi chạy.

### Windows — Command Prompt

```bat
set SPACARE_DB=./data/spacare_manual_test
set PORT=8081
start-web.bat
```

### macOS / Linux

```bash
SPACARE_DB=./data/spacare_manual_test PORT=8081 sh start-web.sh
```

Mở **http://127.0.0.1:8081**. Chạy lại cùng cấu hình để kiểm tra lưu dữ liệu; đổi tên DB nếu cần đợt test sạch khác. Không xóa DB chính.

## Dữ liệu

| Ký hiệu | Chuẩn bị |
|---|---|
| KH-A | Đăng ký `qa_khach01` / `TestSpa@2026`, Khách QA 01, 0909000101 |
| KH-B | Lễ tân thêm Khách QA 02, 0909000102 |
| T / T+1 | Hôm nay / ngày mai |
| S | Giờ hôm nay còn ở tương lai, đủ thời gian trong ca |
| A | Massage của KH-A với Lan tại S; ghi mã thật |
| B, C | Lịch riêng ngày mai để dời/hủy |
| Q | Tồn ghi nhận trước thao tác |

Tài khoản mẫu dùng mật khẩu `123456`: admin, ql_hoa, letan01, ktv_lan, khach01. Tạo `qa_lock01` riêng để thử khóa, không thử khóa admin.

Giá mẫu: Massage **500.000đ/90 phút**, Serum **300.000đ**, hoa hồng Lan **10%**; Silver hệ số **1**. Gói Massage **3.500.000đ/10 buổi/180 ngày**. Nếu đã sửa cấu hình, tính lại số mong đợi.

Ca web mẫu: 08:00–20:00 trong 21 ngày từ lúc tạo DB. Chỉ check-in đúng ngày. Hai tab thường dùng chung phiên; dùng cửa sổ thường/riêng tư hoặc trình duyệt khác để đổi vai trò.

## Thứ tự

1. Nhóm 1–3: chuẩn bị, tài khoản, khách.
2. Nhóm 4 → 6 → 7: đặt A, thực hiện, thanh toán.
3. Nhóm 5: dùng B/C để dời/hủy.
4. Nhóm 8–13: nghiệp vụ mở rộng và bảo mật.

Các số tiền giả định dữ liệu mẫu. Với mỗi phép thử, ghi điểm/hạng/tồn trước đó. Case chưa có đủ dữ liệu ghi **CHƯA TEST**, không đánh dấu PASS.

## Checklist

<details>
<summary>1. Khởi động và trình duyệt · 5 case</summary>

**Vai trò:** Tất cả.

- [ ] **TC-001 · Mở web** — Chạy server ở cổng 8081; mở http://127.0.0.1:8081.<br>
  → Hiện trang đăng nhập, không có lỗi BindException.

- [ ] **TC-002 · Dữ liệu lưu riêng** — Dùng SPACARE_DB=./data/spacare_manual_test theo hướng dẫn đầu tài liệu.<br>
  → App tạo dữ liệu mẫu trong DB test riêng; DB spacare_web cũ được giữ nguyên.

- [ ] **TC-003 · Tải lại trang** — Đăng nhập rồi nhấn F5.<br>
  → Vẫn đăng nhập, nội dung tải lại từ server.

- [ ] **TC-004 · Mở trên điện thoại giả lập** — Firefox: Ctrl+Shift+M, chọn chiều rộng khoảng 390 px; mở menu và các trang.<br>
  → Nút/form không chồng lên nhau; bảng dài cuộn ngang bên trong; toàn trang không bị tràn ngang.

- [ ] **TC-005 · Không phụ thuộc Internet** — Sau khi mở web trên localhost, ngắt Internet rồi chuyển trang/tạo một hồ sơ thử.<br>
  → Giao diện và Java/H2 vẫn hoạt động trên máy; không cần CDN.

</details>

<details>
<summary>2. Đăng nhập, đăng ký và phiên · 12 case</summary>

**Vai trò:** Tài khoản mẫu; dùng qa_lock01 cho khóa tài khoản.

- [ ] **TC-006 · Đăng nhập đủ 5 vai trò** — Lần lượt đăng nhập admin, ql_hoa, letan01, ktv_lan, khach01; mật khẩu mẫu 123456.<br>
  → Vào đúng tài khoản, menu phù hợp vai trò. Đăng xuất giữa các lần đổi vai trò.

- [ ] **TC-007 · Sai mật khẩu** — Đăng nhập một tài khoản test với mật khẩu sai một lần.<br>
  → Báo sai tài khoản/mật khẩu; không vào được hệ thống.

- [ ] **TC-008 · Tài khoản không tồn tại** — Nhập no_such_qa_user và mật khẩu bất kỳ.<br>
  → Bị từ chối; không có phiên đăng nhập mới.

- [ ] **TC-009 · Đăng ký KH-A** — Ở trang đăng nhập chọn Tạo tài khoản; qa_khach01 / TestSpa@2026, họ tên Khách QA 01, điện thoại 0909000101.<br>
  → Đăng ký thành công; tài khoản có quyền Khách hàng và hồ sơ riêng.

- [ ] **TC-010 · Đăng ký trùng** — Thử đăng ký lại cùng username hoặc điện thoại KH-A.<br>
  → Bị từ chối, không sinh tài khoản/hồ sơ trùng.

- [ ] **TC-011 · Mật khẩu mới quá ngắn** — Đăng ký username khác, mật khẩu 12345.<br>
  → Không đăng ký được; yêu cầu ít nhất 8 ký tự.

- [ ] **TC-012 · Đổi mật khẩu** — KH-A → Hồ sơ & cài đặt → Đổi mật khẩu; thử mật khẩu hiện tại sai, sau đó đổi đúng sang TestSpa@2027; đăng xuất/đăng nhập lại.<br>
  → Lần nhập sai bị chặn; mật khẩu cũ không dùng được sau khi đổi; mật khẩu mới đăng nhập được. Ghi lại mật khẩu mới.

- [ ] **TC-013 · Đăng xuất** — Đăng xuất rồi tải lại trang hoặc thử mở trang cũ.<br>
  → Quay về trang đăng nhập; API yêu cầu phiên không còn truy cập được.

- [ ] **TC-014 · Khóa sau 5 lần sai** — Admin tạo tài khoản lễ tân qa_lock01, mật khẩu TestSpa@2026; đăng xuất. Nhập sai mật khẩu tài khoản đó 5 lần rồi thử mật khẩu đúng.<br>
  → Tài khoản bị khóa 15 phút; nhập đúng vẫn không vào được trong thời gian khóa.

- [ ] **TC-015 · Khóa được lưu trong DB** — Khi qa_lock01 đang khóa, tắt server rồi khởi động lại với cùng SPACARE_DB.<br>
  → qa_lock01 vẫn khóa; không tự mở khóa khi server khởi động lại.

- [ ] **TC-016 · Mở khóa bằng admin** — Admin → Tài khoản → qa_lock01 → Sửa/Mở khóa → Lưu; thử đăng nhập qa_lock01.<br>
  → Bộ đếm/khóa được đặt lại; đăng nhập đúng thành công.

- [ ] **TC-017 · Vô hiệu hóa tài khoản** — Đăng nhập qa_lock01 ở cửa sổ riêng. Admin tắt Đang hoạt động cho qa_lock01. Cửa sổ kia bấm Cập nhật dữ liệu.<br>
  → Phiên bị từ chối; không tiếp tục thao tác được. Admin có thể kích hoạt lại.

</details>

<details>
<summary>3. Khách hàng · 7 case</summary>

**Vai trò:** Lễ tân letan01, quản lý và KH-A.

- [ ] **TC-018 · Thêm KH-B** — Lễ tân → Khách hàng → Thêm khách: Khách QA 02, 0909000102; ghi lưu ý Da nhạy cảm.<br>
  → Thêm đúng một hồ sơ; lúc mới tạo là Silver, 0 điểm, 0 chi tiêu trong DB test sạch.

- [ ] **TC-019 · Trùng số điện thoại** — Thêm khách khác dùng 0909000102.<br>
  → Bị từ chối; hồ sơ KH-B không bị thay thế.

- [ ] **TC-020 · Điện thoại sai** — Nhập số 123 hoặc số không bắt đầu bằng 0.<br>
  → Bị chặn; không lưu dữ liệu sai.

- [ ] **TC-021 · Tìm kiếm** — Tìm Khách QA 02 bằng tên rồi bằng 0909000102.<br>
  → Trả về đúng hồ sơ; xóa từ khóa thấy danh sách đầy đủ.

- [ ] **TC-022 · Cập nhật hồ sơ** — Sửa tên, email, dị ứng của KH-B; lưu, tải lại trang.<br>
  → Thông tin mới được lưu; mã khách và lịch sử không đổi.

- [ ] **TC-023 · Ngày sinh tương lai** — Thử sửa ngày sinh thành ngày tương lai.<br>
  → Bị từ chối; thông tin cũ giữ nguyên.

- [ ] **TC-024 · Khách sửa hồ sơ chính mình** — KH-A đăng nhập → Hồ sơ → Cập nhật hồ sơ.<br>
  → Sửa được hồ sơ của mình; không có chức năng quản lý các khách khác.

</details>

<details>
<summary>4. Đặt lịch và kiểm tra tài nguyên · 12 case</summary>

**Vai trò:** Lễ tân hoặc KH-A.

- [ ] **TC-025 · Đặt lịch A hợp lệ** — Chọn KH-A, Quận 1, Massage thư giãn, Lan, hôm nay giờ S còn ở tương lai, 1 người; Tìm giờ trống → chọn giờ → Xác nhận. Ghi mã A.<br>
  → Có lịch CONFIRMED, 90 phút, giá tại lúc đặt 500.000đ; có nhân viên và phòng SPA.

- [ ] **TC-026 · Đặt trùng nhân viên** — Tạo lịch cho KH-B cùng giờ S, cùng Lan, dịch vụ Massage.<br>
  → Bị từ chối; danh sách không sinh thêm lịch chồng nhau.

- [ ] **TC-027 · Sai chuyên môn** — Chọn Minh để làm Massage.<br>
  → Không có giờ phù hợp hoặc bị từ chối khi xác nhận.

- [ ] **TC-028 · Sai chi nhánh nhân viên** — Chọn Thủ Đức nhưng vẫn chọn Lan của Quận 1.<br>
  → Không nhận lịch; không tự đặt nhầm cơ sở.

- [ ] **TC-029 · Ngoài ca làm** — Với dữ liệu mẫu, thử lịch lúc 22:00 khi chưa phân thêm ca.<br>
  → Không có giờ phù hợp hoặc bị từ chối. Bản web mẫu có ca 08:00–20:00; không dùng giả định Minh chỉ làm sáng của console cũ.

- [ ] **TC-030 · Giờ quá khứ** — Chọn ngày hôm qua hoặc giờ đã qua.<br>
  → Không nhận lịch quá khứ.

- [ ] **TC-031 · Chưa chọn dịch vụ** — Để trống dịch vụ và không chọn combo rồi xác nhận.<br>
  → Bị từ chối; không tạo lịch rỗng.

- [ ] **TC-032 · Lịch liền kề** — Đặt Gội đầu cho KH-B với Lan đúng lúc lịch A kết thúc: S + 90 phút.<br>
  → Được nhận nếu ghế HAIR và ca còn trống; không coi hai khoảng chạm nhau là trùng.

- [ ] **TC-033 · Khách bị trùng giờ** — KH-A đã có lịch A; thử đặt thêm cùng giờ ở Thủ Đức với An. Không đánh dấu đặt thay người đi cùng.<br>
  → Cảnh báo hồ sơ khách đã có lịch; không tạo thêm tự động.

- [ ] **TC-034 · Khách xác nhận đặt thay người đi cùng** — Thử lại trường hợp trên, đánh dấu xác nhận đặt thay người đi cùng, chọn tài nguyên còn trống.<br>
  → Được nhận nếu đủ tài nguyên; không bỏ qua kiểm tra nhân viên/phòng.

- [ ] **TC-035 · Tự chọn nhân viên** — Đặt một lịch ở khung khác, chọn Tự chọn nhân viên phù hợp.<br>
  → Hệ thống chọn người đúng chuyên môn, chi nhánh và ca; xem Chi tiết để kiểm tra.

- [ ] **TC-036 · Hai người đặt đồng thời** — Hai phiên trình duyệt khác nhau, chọn KH-A và KH-B, cùng dịch vụ Massage, cùng giờ trống ở Thủ Đức, nhân viên An; bấm xác nhận gần như đồng thời.<br>
  → Chỉ một lịch được tạo; phiên còn lại bị từ chối. Refresh cả hai và đối chiếu tổng số lịch.

</details>

<details>
<summary>5. Đổi lịch và hủy lịch · 7 case</summary>

**Vai trò:** Dùng lịch mới ngày mai; không dùng lịch A đã thực hiện.

- [ ] **TC-037 · Dời sang giờ trống** — Tạo lịch B ngày mai; Chi tiết → Dời lịch → chọn giờ trống khác.<br>
  → Giờ/phòng cập nhật; giá và thời lượng đã đặt giữ nguyên; giờ cũ được nhả.

- [ ] **TC-038 · Dời vào giờ bận** — Tạo lịch C cùng nhân viên ở khung khác; dời B vào khung của C.<br>
  → Bị từ chối; lịch B vẫn giữ giờ cũ.

- [ ] **TC-039 · Hủy có lý do** — Hủy lịch C, nhập lý do Khách đổi kế hoạch.<br>
  → Trạng thái CANCELLED; lưu lý do; có thể đặt lại tài nguyên ở giờ cũ.

- [ ] **TC-040 · Hủy không lý do** — Tạo một lịch khác rồi thử hủy với lý do trống.<br>
  → Không hủy được; lịch giữ nguyên.

- [ ] **TC-041 · Giới hạn báo trước 2 giờ** — KH-A tự đặt một lịch gần hiện tại dưới 2 giờ rồi thử đổi/hủy.<br>
  → Khách bị chặn bởi chính sách báo trước.

- [ ] **TC-042 · Lễ tân ghi đè** — Lễ tân mở lịch sát giờ trên, đánh dấu ghi đè và nhập lý do.<br>
  → Đổi/hủy được; không có lý do thì không ghi đè thành công.

- [ ] **TC-043 · Không sửa lịch đã bắt đầu** — Sau khi check-in một lịch riêng, thử dời/hủy.<br>
  → Không có thao tác hợp lệ hoặc API từ chối; lịch không quay về trạng thái xác nhận.

</details>

<details>
<summary>6. Check-in và thực hiện dịch vụ · 8 case</summary>

**Vai trò:** Lễ tân, kỹ thuật viên Lan, quản lý.

- [ ] **TC-044 · Check-in đúng ngày** — Lễ tân mở lịch A của hôm nay → Check-in.<br>
  → CONFIRMED → CHECKED_IN.

- [ ] **TC-045 · Không check-in ngày mai** — Mở một lịch ngày mai và thử Check-in.<br>
  → Bị từ chối; vẫn CONFIRMED.

- [ ] **TC-046 · Lan bắt đầu lịch được giao** — Đăng nhập ktv_lan ở phiên khác; bấm Cập nhật; mở A → Bắt đầu dịch vụ.<br>
  → Chỉ thấy lịch được phân công; A chuyển IN_PROGRESS.

- [ ] **TC-047 · Lan không sửa lịch người khác** — Tạo một lịch của Hà và thử dùng API cập nhật bằng phiên ktv_lan theo phụ lục.<br>
  → 403; lịch của Hà không đổi. Chỉ việc không thấy nút chưa đủ chứng minh API an toàn.

- [ ] **TC-048 · Không nhảy trạng thái** — Thử qua API chuyển một lịch CONFIRMED thẳng thành COMPLETED.<br>
  → Bị từ chối; dữ liệu và kho không thay đổi.

- [ ] **TC-049 · Hoàn thành và trừ vật tư** — Ghi tồn tinh dầu trước khi hoàn thành A là Q; Lan bấm Hoàn thành.<br>
  → IN_PROGRESS → COMPLETED; tinh dầu giảm đúng 10 ml: Q − 10, với định mức mẫu.

- [ ] **TC-050 · Không hoàn thành lặp** — Tải lại và thử hoàn thành A lần nữa hoặc gọi lại API.<br>
  → Không trừ kho thêm; không tạo giao dịch tiêu hao lặp.

- [ ] **TC-051 · Tự đánh dấu vắng** — Tạo lịch riêng trong ngày, giữ CONFIRMED, chờ qua giờ kết thúc +15 phút; đợi thêm một chu kỳ worker 30 giây rồi cập nhật.<br>
  → Thành NO_SHOW, không thu tiền/không trừ kho dịch vụ, không giữ buổi gói. Nếu chưa đủ thời gian ghi CHƯA TEST, không chỉnh giờ máy để lách.

</details>

<details>
<summary>7. Thanh toán và hóa đơn · 9 case</summary>

**Vai trò:** Lễ tân; chỉ dùng lịch COMPLETED chưa thanh toán.

- [ ] **TC-052 · Không thanh toán sớm** — Mở lịch còn CONFIRMED/CHECKED_IN/IN_PROGRESS.<br>
  → Không có nút thanh toán hợp lệ; API cũng phải từ chối.

- [ ] **TC-053 · Báo giá lịch A** — A đã COMPLETED. Thanh toán: không voucher, 0 điểm, không sản phẩm → Tính hóa đơn.<br>
  → Với KH-A mới chưa đổi hạng và giá mẫu: tổng 500.000đ, tích 50 điểm. Chưa xác nhận thì chưa có hóa đơn/điểm mới.

- [ ] **TC-054 · Thu tiền lịch A** — Xác nhận đã thu tiền mặt; xem hóa đơn và số dư KH-A.<br>
  → Tạo đúng một hóa đơn PAID 500.000đ; cộng 50 điểm; ghi nhận hoa hồng; lịch vẫn COMPLETED, có liên kết hóa đơn.

- [ ] **TC-055 · Không thu tiền hai lần** — Mở lại A, thử thanh toán lần nữa.<br>
  → Không thu thêm; không cộng điểm/hoa hồng hoặc trừ buổi lần nữa.

- [ ] **TC-056 · In và lưu PDF** — Mở hóa đơn → In / Lưu PDF, chọn Lưu thành PDF trong hộp in Firefox.<br>
  → Đúng khách, dịch vụ, khoản giảm và tổng tiền; không in cả sidebar hoặc nút điều khiển.

- [ ] **TC-057 · Sản phẩm và voucher** — Tạo lịch Massage COMPLETED khác cho KH-B còn Silver; thêm 1 Serum 300.000đ, nhập SPACARE10, 0 điểm, Tính hóa đơn.<br>
  → Nếu mã mẫu còn hiệu lực/lượt: 500.000 + 300.000 = 800.000; giảm 80.000; tổng 720.000đ; tích 72 điểm.

- [ ] **TC-058 · Chưa chọn người bán lẻ** — Thanh toán ví dụ 720.000đ trên với Lan thực hiện, không chọn nhân viên bán sản phẩm.<br>
  → Hoa hồng dịch vụ Lan: 500.000 × (720.000/800.000) × 10% = 45.000đ với tỷ lệ mẫu. Không tự tính thêm phần sản phẩm cho Lan.

- [ ] **TC-059 · Có nhân viên bán lẻ** — Trên một hóa đơn tương tự mới, chọn Lan là người bán sản phẩm.<br>
  → Với tỷ lệ mẫu 10% cho cả phần dịch vụ và bán lẻ: hoa hồng Lan tổng 72.000đ. Đối chiếu theo đúng hóa đơn mới.

- [ ] **TC-060 · Thiếu kho khi bán lẻ** — Trên lịch COMPLETED chưa thu, thêm Serum với số lượng lớn hơn tồn; bấm xác nhận.<br>
  → Thanh toán thất bại; không có hóa đơn mới, không cộng/trừ điểm, không trừ buổi gói; tồn và lịch sử kho giữ nguyên.

</details>

<details>
<summary>8. Điểm, hạng và mã khuyến mãi · 10 case</summary>

**Vai trò:** Quản lý và lễ tân; thực hiện sau các ví dụ Silver ở phần 7.

- [ ] **TC-061 · Tích điểm theo tiền thực thu** — Đối chiếu hóa đơn mới với sổ điểm: Silver ×1, Gold ×1,2, Platinum ×1,5.<br>
  → Điểm = floor(tổng thực thu / 10.000 × hệ số hạng áp dụng trước thanh toán).

- [ ] **TC-062 · Đổi điểm hợp lệ** — Cho KH-A đủ ít nhất 100 điểm, trên hóa đơn đủ lớn nhập 100 điểm rồi Tính hóa đơn.<br>
  → Giảm 10.000đ; sau thu, trừ 100 điểm rồi cộng điểm mới trên tiền thực thu; có lịch sử REDEEM/EARN.

- [ ] **TC-063 · Điểm không theo bội số** — Nhập 150 điểm.<br>
  → Bị từ chối; không làm tròn hoặc tự trừ điểm.

- [ ] **TC-064 · Vượt số dư** — Nhập số điểm bội 100 lớn hơn điểm khả dụng.<br>
  → Bị từ chối, không ghi hóa đơn.

- [ ] **TC-065 · Vượt mức 50%** — Dùng khách đủ điểm; lấy tổng sau các ưu đãi là P. Nhập số điểm quy đổi hơn P/2.<br>
  → Bị từ chối. Ví dụ P=500.000đ: 2.600 điểm tương đương 260.000đ vượt trần 250.000đ; phải có đủ số dư để kiểm tra riêng quy tắc này.

- [ ] **TC-066 · Mã không tồn tại/hết hạn** — Thử mã KHONGTONTAI; tạo mã QA_EXPIRED có ngày kết thúc hôm qua rồi dùng.<br>
  → Bị từ chối. Mã hết hạn không giảm tiền hoặc tăng lượt dùng.

- [ ] **TC-067 · Điều kiện và giới hạn lượt** — Tạo mã QA_ONCE: 10%, tối đa 1 lượt, hóa đơn tối thiểu 200.000đ. Thử hóa đơn dưới ngưỡng; sau đó dùng một lần hợp lệ rồi thử trên hóa đơn khác.<br>
  → Dưới ngưỡng bị chặn; chỉ một hóa đơn hợp lệ dùng được mã; không tăng lượt khi mới xem báo giá.

- [ ] **TC-068 · Không cộng dồn combo và voucher** — Tạo lịch combo giảm 30%; khi thu nhập mã giảm 10%.<br>
  → Áp dụng mức combo 30% tốt hơn, không trừ thêm 10%; sau đó mới giảm hạng và điểm.

- [ ] **TC-069 · Nâng hạng từ chi tiêu thật** — Dùng một khách test riêng chưa có chi tiêu, mua 3 gói Massage × 3.500.000đ.<br>
  → Chi tiêu 12 tháng 10.500.000đ, hạng Gold; không cần sửa trực tiếp hạng hồ sơ.

- [ ] **TC-070 · Hết hạn điểm và cửa sổ 12 tháng** — Chạy bộ tự động ở phụ lục thay cho việc chờ 12 tháng hoặc sửa giờ máy.<br>
  → Bộ test xác nhận điểm hết hạn không đổi được, chi tiêu quá 12 tháng không giữ hạng. Ghi đây là kết quả tự động, không đánh dấu đã kiểm tra thủ công.

</details>

<details>
<summary>9. Combo, nhóm và liệu trình · 12 case</summary>

**Vai trò:** Quản lý và lễ tân.

- [ ] **TC-071 · Combo nhiều dịch vụ** — Tạo combo gồm ít nhất 2 dịch vụ; đặt ở giờ trống, để nhân viên tự chọn.<br>
  → Có các dòng dịch vụ nối tiếp; mỗi dòng đúng người/phòng; tổng thời gian bằng tổng thời lượng.

- [ ] **TC-072 · Không đủ tài nguyên combo** — Chọn nhân viên không có đủ chuyên môn cho các dịch vụ trong combo.<br>
  → Không tạo lịch dở chỉ có một phần dịch vụ.

- [ ] **TC-073 · Nhóm 2 đủ tài nguyên** — Chọn KH-B, Massage, Quận 1, tự chọn nhân viên, 2 người, ngày/giờ trống riêng.<br>
  → Tạo 2 lịch; hai người không trùng nhân viên/phòng cùng giờ; mức giảm nhóm lưu khi đặt là 50%.

- [ ] **TC-074 · Nhóm 2 thiếu tài nguyên** — Thử Massage nhóm 2 ở Thủ Đức với dữ liệu mẫu chỉ một nhân viên/phòng SPA; không thêm phòng trước khi test.<br>
  → Cả nhóm bị từ chối; không có một lịch lẻ được ghi sót.

- [ ] **TC-075 · Bán gói liệu trình** — Bán gói Massage 10 buổi cho KH-A, giá 3.500.000đ.<br>
  → Có hóa đơn bán gói, 10 buổi, hạn 180 ngày; tích điểm một lần từ tiền mua gói.

- [ ] **TC-076 · Giữ buổi khi đặt** — Đặt Massage dùng gói đã mua, ở giờ không trùng lịch khác.<br>
  → Còn lại 10, khả dụng 9; chưa trừ chính thức một buổi.

- [ ] **TC-077 · Hủy nhả buổi** — Hủy lịch giữ gói trên với lý do hợp lệ.<br>
  → Khả dụng trở về 10, còn lại vẫn 10.

- [ ] **TC-078 · Dùng buổi không thu tiền lại** — Đặt lại gói trong ngày → Check-in → Bắt đầu → Hoàn thành → Thanh toán không sản phẩm/đổi điểm.<br>
  → Hóa đơn 0đ; còn 9 buổi, không tích điểm từ giá niêm yết 500.000đ lần nữa. Vật tư vẫn tiêu hao theo dịch vụ.

- [ ] **TC-079 · Gói sai chủ/sai dịch vụ** — Lễ tân chọn gói KH-A cho KH-B hoặc dùng gói Massage cho Gội đầu.<br>
  → Bị từ chối; buổi gói không thay đổi.

- [ ] **TC-080 · Sau hạn gói** — Chọn ngày hẹn sau hạn gói. Nếu ngày đó chưa có ca, tạo ca/phòng phù hợp trước để tách riêng lỗi hết hạn.<br>
  → Bị từ chối do gói hết hạn trước ngày hẹn; không giữ thêm buổi.

- [ ] **TC-081 · Gói hết buổi/đã giữ hết** — Tạo gói QA một buổi, bán cho khách test. Đặt một lịch giữ buổi rồi thử đặt lịch thứ hai ở giờ khác bằng cùng gói.<br>
  → Lịch thứ hai bị từ chối dù buổi đầu chưa thanh toán; không bán vượt buổi khả dụng.

- [ ] **TC-082 · Không sửa quyền lợi gói đã bán** — Quản lý thử sửa cấu hình một gói đã có khách mua.<br>
  → Bị chặn; phải tạo gói mới để không đổi quyền lợi đã bán.

</details>

<details>
<summary>10. Nhân viên, ca làm và chấm công · 10 case</summary>

**Vai trò:** Quản lý; kỹ thuật viên QA mới.

- [ ] **TC-083 · Thêm dịch vụ QA** — Tạo QA Facial, giá 200.000đ, 30 phút, loại phòng SPA.<br>
  → Xuất hiện trong danh mục, chưa tự có nhân viên làm được nếu chưa gán chuyên môn.

- [ ] **TC-084 · Thêm nhân viên QA** — Tạo Nhân viên QA, 0909000201, Quận 1, hoa hồng 15%; chọn chuyên môn QA Facial, tài khoản qa_ktv01 / TestSpa@2026.<br>
  → Có hồ sơ và tài khoản TECHNICIAN liên kết đúng người.

- [ ] **TC-085 · Phân ca hợp lệ** — Cho Nhân viên QA ca ngày T+3 từ 08:00 đến 12:00; đặt một lịch QA Facial 09:00 với người này.<br>
  → Đặt được nếu còn phòng SPA; thấy đúng ca và lịch trong tài khoản qa_ktv01.

- [ ] **TC-086 · Ca giao nhau** — Thêm ca 10:00–14:00 cùng ngày cho Nhân viên QA.<br>
  → Bị từ chối; không có ca chồng.

- [ ] **TC-087 · Ca liền kề** — Thêm ca 12:00–16:00 cùng ngày.<br>
  → Được nhận, không coi hai ca chạm nhau là giao nhau.

- [ ] **TC-088 · Xóa ca có lịch** — Thử xóa ca 08:00–12:00 đang chứa lịch 09:00 trên.<br>
  → Bị từ chối; phải dời/hủy lịch trước.

- [ ] **TC-089 · Ngừng nhân viên có lịch** — Thử bỏ Đang hoạt động của Nhân viên QA khi còn lịch chưa hoàn thành.<br>
  → Bị từ chối; không bỏ rơi lịch đã phân công.

- [ ] **TC-090 · Chấm công** — Đăng nhập qa_ktv01 → Chấm công: bấm một lần vào, lần hai ra; thử lần ba.<br>
  → Có đúng một bản ghi hôm nay với giờ vào/ra; lần ba bị chặn.

- [ ] **TC-091 · Giữ giá lịch sử** — Sau khi đã đặt QA Facial 200.000đ/30 phút, sửa dịch vụ thành 300.000đ/45 phút.<br>
  → Lịch cũ vẫn 200.000đ/30 phút; lịch mới dùng thông số mới.

- [ ] **TC-092 · Ngừng bán dịch vụ** — Ngừng QA Facial rồi thử tạo lịch mới bằng dịch vụ đó.<br>
  → Không đặt mới được; lịch cũ và lịch sử không bị xóa.

</details>

<details>
<summary>11. Chi nhánh, phòng và kho · 9 case</summary>

**Vai trò:** Quản lý.

- [ ] **TC-093 · Thêm chi nhánh/phòng** — Thêm Chi nhánh QA và Phòng QA loại SPA.<br>
  → Lưu và hiển thị đúng chi nhánh/loại phòng; chưa nhận lịch nếu thiếu nhân viên/chuyên môn/ca.

- [ ] **TC-094 · Phòng đúng loại** — Thử đặt Gội đầu ở chi nhánh QA chỉ có SPA, dù đã có nhân viên chuyên môn/ca.<br>
  → Bị từ chối vì thiếu HAIR; không lấy phòng SPA thay ghế tóc.

- [ ] **TC-095 · Không đóng phòng còn lịch** — Ở Quận 1, chọn phòng đang có lịch CONFIRMED/CHECKED_IN/IN_PROGRESS, thử ngừng hoạt động hoặc đổi chi nhánh.<br>
  → Bị chặn; lịch và phòng cũ giữ nguyên.

- [ ] **TC-096 · Nhập vật tư QA** — Tạo Vật tư QA, SKU QA-001, đơn vị ml, ngưỡng cảnh báo 10; nhập 50, lý do Nhập thử.<br>
  → Tồn 50, nhật ký +50 có lý do.

- [ ] **TC-097 · Xuất vật tư QA** — Xuất -45, lý do Dùng thử.<br>
  → Tồn 5; có cảnh báo thấp và nhật ký -45.

- [ ] **TC-098 · Không âm kho** — Tiếp tục xuất -10 khi tồn 5.<br>
  → Bị từ chối; tồn vẫn 5, không có bản ghi xuất sai.

- [ ] **TC-099 · Thiếu vật tư không hoàn thành** — Gắn định mức QA Facial dùng 10 Vật tư QA; tạo lịch hôm nay với nhân viên/ca hợp lệ, đưa tới IN_PROGRESS khi tồn 5, rồi hoàn thành.<br>
  → Bị chặn; trạng thái vẫn IN_PROGRESS, kho không trừ dở.

- [ ] **TC-100 · Bổ sung rồi hoàn thành lại** — Nhập thêm 20 vật tư QA, tồn thành 25; hoàn thành lại lịch trên.<br>
  → Thành COMPLETED, tồn 15; chỉ trừ 10 một lần.

- [ ] **TC-101 · Bỏ định mức** — Sửa định mức QA Facial/Vật tư QA về 0.<br>
  → Định mức được gỡ; lịch sau không trừ vật tư này theo định mức đã bỏ.

</details>

<details>
<summary>12. Đánh giá, thông báo, báo cáo và phục hồi · 12 case</summary>

**Vai trò:** Khách, lễ tân, quản lý và admin.

- [ ] **TC-102 · Khách đánh giá lịch của mình** — KH-A → Đánh giá → chọn A đã COMPLETED → 5 sao, nhận xét Tốt.<br>
  → Hiện đánh giá đúng khách và lịch.

- [ ] **TC-103 · Không đánh giá lặp** — Thử đánh giá lại A.<br>
  → Không chọn được lịch hoặc API từ chối; không có bản ghi thứ hai.

- [ ] **TC-104 · Không đánh giá lịch chưa xong/người khác** — KH-A thử chọn lịch chưa COMPLETED hoặc gọi API đánh giá mã lịch của KH-B.<br>
  → Bị chặn; không ghi đánh giá sai chủ.

- [ ] **TC-105 · Thông báo xác nhận/đổi/hủy** — Xem thông báo tương ứng các lịch đã tạo/dời/hủy.<br>
  → Nội dung đúng lịch/khách; lịch bị hủy không còn nhắc PENDING.

- [ ] **TC-106 · Nhắc lịch đến hạn** — Tạo lịch trong 24 giờ tới; chờ tối đa một chu kỳ worker 30 giây, hoặc lễ tân bấm Xử lý thông báo đến hạn; cập nhật trang.<br>
  → Nhắc chuyển thành Đã mô phỏng gửi; không coi đây là SMS/email thật.

- [ ] **TC-107 · Đối chiếu doanh thu** — Lọc báo cáo theo ngày xuất hóa đơn. Cộng các hóa đơn PAID và tiền bán gói trong cùng khoảng.<br>
  → Tổng khớp; không lấy ngày hẹn thay ngày thu; không cộng lại giá niêm yết buổi gói đã trả trước.

- [ ] **TC-108 · Đối chiếu báo cáo phụ** — Kiểm tra dịch vụ được sử dụng, hoa hồng, số lịch vắng, khách mới và khách thân thiết với dữ liệu đã tạo.<br>
  → Số liệu khớp phạm vi ngày tương ứng; giá niêm yết dịch vụ được ghi là trước giảm, không coi là tổng thực thu.

- [ ] **TC-109 · Xuất CSV** — Quản lý → Báo cáo → chọn ngày → Xuất CSV; mở bằng LibreOffice Calc.<br>
  → Tiếng Việt đọc đúng; ngày, số hóa đơn, doanh thu khớp giao diện.

- [ ] **TC-110 · Tải lại sau thay đổi từ phiên khác** — Giữ hai phiên lễ tân/kỹ thuật viên; một bên đổi trạng thái; bên kia bấm Cập nhật hoặc đợi khoảng 20 giây khi không nhập liệu/mở modal.<br>
  → Nhìn thấy trạng thái mới; không cần tạo dữ liệu lần nữa.

- [ ] **TC-111 · Dữ liệu sau khởi động lại** — Ghi vài mã khách, lịch, hóa đơn và tồn kho; Ctrl+C server; chạy lại đúng lệnh SPACARE_DB test ban đầu.<br>
  → Các bản ghi và số dư giữ nguyên; không nạp lại từ đầu.

- [ ] **TC-112 · Sao lưu và khôi phục riêng** — Admin → Hồ sơ & cài đặt → Sao lưu dữ liệu; lưu ZIP. Dừng server, dùng H2 Restore vào ./restored_manual rồi mở DB khôi phục theo phụ lục.<br>
  → Mở được bản khôi phục, đủ khách/lịch/hóa đơn/gói/kho; DB gốc không bị ghi đè.

- [ ] **TC-113 · HTTPS tùy chọn** — Sau khi dừng bản HTTP test, chạy SPACARE_DB=./data/spacare_manual_test sh start-https.sh; mở https://localhost:8443.<br>
  → Kết nối HTTPS hoạt động sau khi xác nhận chứng chỉ tự ký cục bộ; đăng nhập được. Cookie phiên có Secure/HttpOnly.

</details>

<details>
<summary>13. Kiểm tra API bảo mật nâng cao · 4 case</summary>

**Vai trò:** Dùng F12 Console theo phụ lục.

- [ ] **TC-114 · API thiếu đăng nhập** — Đăng xuất rồi mở /api/state trực tiếp.<br>
  → HTTP 401, không trả danh sách khách/lịch.

- [ ] **TC-115 · API sai vai trò** — Đăng nhập KH-A rồi gọi service.save theo phụ lục.<br>
  → HTTP 403; không tạo dịch vụ, dù người dùng tự gọi API thay vì bấm nút.

- [ ] **TC-116 · API thiếu CSRF** — Trong phiên đã đăng nhập, gọi POST /api/action không gửi X-CSRF-Token.<br>
  → HTTP 403; không thay đổi dữ liệu.

- [ ] **TC-117 · API sai chủ sở hữu** — Dùng phiên KH-A gọi hủy một lịch CONFIRMED thuộc KH-B; dùng mã đã ghi trước.<br>
  → HTTP 403; lịch KH-B không đổi.

</details>

<details>
<summary>Kiểm tra API bằng Firefox Console</summary>

Dùng DB test, mở F12 → Console. Các ví dụ dưới dự kiến bị từ chối; đọc lệnh trước khi chạy.

### Thiếu CSRF

Đăng nhập một tài khoản bất kỳ rồi chạy:

```javascript
var res = await fetch('/api/action', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ action: 'service.save', name: 'KHONG_DUOC_LUU' })
});
console.log(res.status, await res.json());
```

Mong đợi 403. Kiểm tra danh mục không có dịch vụ mới.

### Khách hàng thử gọi API quản lý

Đăng nhập KH-A, chạy:

```javascript
var me = await (await fetch('/api/me')).json();
var res = await fetch('/api/action', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-CSRF-Token': me.csrf
  },
  body: JSON.stringify({ action: 'service.save', name: 'KHONG_DUOC_LUU' })
});
console.log(res.status, await res.json());
```

Mong đợi 403. Đây là kiểm tra phân quyền, khác với ví dụ thiếu CSRF.

### Khách thử hủy lịch người khác

Ghi mã một lịch CONFIRMED của KH-B bằng tài khoản lễ tân trước. Sau đó đăng nhập KH-A; thay `123` bằng đúng mã lịch KH-B, không dùng lịch của KH-A:

```javascript
var otherAppointmentId = 123; // thay bằng mã lịch KH-B đã ghi
var me = await (await fetch('/api/me')).json();
var res = await fetch('/api/action', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-CSRF-Token': me.csrf
  },
  body: JSON.stringify({
    action: 'appointment.cancel',
    id: otherAppointmentId,
    reason: 'Kiem tra quyen'
  })
});
console.log(res.status, await res.json());
```

Mong đợi 403; lễ tân cập nhật lại để xác nhận lịch KH-B không đổi.

Để kiểm tra nhảy trạng thái/nhân viên sai lịch, dùng cùng cấu trúc, thay body bằng:

```javascript
{ action: 'appointment.status', id: 123, status: 'COMPLETED' }
```

Dùng lễ tân và mã lịch đang CONFIRMED thì mong đợi 400 vì nhảy trạng thái. Dùng tài khoản kỹ thuật viên không được phân công thì mong đợi 403. Phải lấy lại CSRF sau khi đổi tài khoản, nếu không sẽ chỉ kiểm tra lỗi phiên/CSRF.

</details>

## Chạy tự động

Cần Python 3 và keytool của JDK; cổng 18089 đang trống. DB tạm riêng, không sửa dữ liệu test tay.

**Windows — Command Prompt, từ gốc repo**

```bat
cd app
javac -encoding UTF-8 -source 17 -target 17 -d target\classes src\main\java\spacare\web\*.java
xcopy /E /I /Y src\main\resources target\classes >nul
python tests\acceptance.py
```

Chạy web một lần trước để có `target/classes`.

**macOS / Linux, từ gốc repo**

```bash
sh run.sh test
```

Kết quả mong đợi của bản đã giao: `PASS 186 acceptance checks`. Ghi kết quả thật trên máy bạn, không thay bằng kết quả từ máy khác.

## Sao lưu và khôi phục

Admin → Hồ sơ & cài đặt → Sao lưu dữ liệu. Dừng server rồi dùng H2 Restore vào thư mục khác; xem [hướng dẫn](app/README.md#sao-lưu). Đối chiếu mã khách/lịch/hóa đơn và tồn trước khi dùng bản khôi phục.

## Ghi nhận kết quả

| Nội dung | Kết quả |
|---|---|
| Người test / ngày | |
| OS / trình duyệt / Java | |
| Database | |
| Đã chạy / PASS / FAIL | |
| Chưa test / bị chặn | |
| Mã lỗi cần sửa | |

Khi có lỗi, ghi **mã TC, tài khoản, mã lịch/hóa đơn, bước tái hiện, mong đợi, thực tế và thông báo/ảnh**.

Thông báo và ngân hàng đang mô phỏng. Không đánh dấu đã gửi tin thật, nhận tiền ngân hàng hoặc phát hành hóa đơn thuế.
