# SpaCare Console

Ứng dụng console tổng hợp. Cần JDK 17+; Maven là tùy chọn.

## Windows

Từ thư mục `Java`, chạy `run_saved.bat` nếu có Maven; hoặc `run_nomaven.bat saved` để biên dịch trực tiếp bằng JDK. Cách thứ hai tải H2 nếu máy chưa có.

## macOS / Linux

Từ thư mục `Java`:

| Lệnh | Chế độ |
|---|---|
| `sh run.sh saved` | Menu, lưu file |
| `sh run.sh menu` | Menu, dữ liệu RAM |
| `sh run.sh demo` | Kịch bản tự động |
| `sh run.sh check` | 36 kiểm tra tính giá |
| `sh run.sh test` | Tính giá và 59 kiểm tra nghiệp vụ |

Tài khoản: `admin`, `ql_hoa`, `letan01`, `ktv_lan`; mật khẩu `123456`.

DB lưu tại `Java/data/spacare.mv.db`, khác DB web. Xem [hướng dẫn](../GUIDE.md).
