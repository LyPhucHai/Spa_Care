# Thành phần bên thứ ba

| Thành phần | Sử dụng |
|---|---|
| Java SE 17+ | HTTP/HTTPS, JDBC, thời gian, PBKDF2; người dùng tự cài JDK |
| H2 2.2.224 | Database nhúng, JAR trong `app/lib/` |
| Python 3 | Kiểm thử API, không cần để chạy web |
| Playwright / Chromium | Kiểm thử giao diện tùy chọn |

H2 được giữ từ repo gốc. Thông tin: [dự án](https://h2database.com/) · [giấy phép](https://h2database.com/html/license.html).

Giao diện dùng HTML/CSS/JavaScript và SVG trong source; không tải ảnh/font từ CDN.
