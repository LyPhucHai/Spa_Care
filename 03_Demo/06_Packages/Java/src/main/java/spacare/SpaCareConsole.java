package spacare;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import static spacare.DemoUtil.*;

/** Menu tương tác: đặt lịch, combo, liệu trình, đôi/nhóm, trải nghiệm lần đầu, thanh toán và tích điểm. */
public class SpaCareConsole {
    static class Eof extends RuntimeException {}

    static Scanner in;
    static final RescheduleService bk = new RescheduleService();
    static final StatusService st = new StatusService();
    static final BillingService bill = new BillingService();
    static final PackageService pk = new PackageService();
    static final CustomerService cs = new CustomerService();
    static final ManagementService management = new ManagementService();

    static String ask(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) throw new Eof();
        return in.nextLine().trim();
    }

    static int askInt(String prompt) { return Integer.parseInt(ask(prompt)); }

    static List<Integer> ids(String s) {
        List<Integer> out = new ArrayList<>();
        for (String part : s.split(",")) if (!part.isBlank()) out.add(Integer.parseInt(part.trim()));
        return out;
    }

    static LocalDate askDate() {
        String s = ask("Ngày (yyyy-MM-dd, Enter = ngày mai): ");
        return s.isEmpty() ? LocalDate.now().plusDays(1) : LocalDate.parse(s);
    }

    static LocalDateTime askStart() {
        LocalDate day = askDate();
        return day.atTime(LocalTime.parse(ask("Giờ bắt đầu (HH:mm, ví dụ 09:30): ")));
    }

    static void printMenu() {
        say("Đăng nhập: " + Auth.user() + " | " + Auth.role());
        if (Auth.allowed("TECHNICIAN")) {
            say("7) Lịch được phân công   9) Bắt đầu / Hoàn thành dịch vụ   99) Đăng xuất   0) Thoát");
            return;
        }
        System.out.println("\n KHÁCH HÀNG   1) Danh sách khách        2) Thêm khách mới");
        System.out.println(" ƯU ĐÃI       3) Combo, gói, đôi/nhóm   4) Gói của một khách");
        System.out.println(" ĐẶT LỊCH     5) Khung giờ trống   6) Đặt lịch   7) Danh sách lịch hẹn   8) Dời/Hủy lịch");
        System.out.println(" DỊCH VỤ      9) Check-in / Bắt đầu / Hoàn thành");
        System.out.println(" THANH TOÁN   10) Thanh toán lịch hẹn   11) Mua gói liệu trình");
        System.out.println(" KHÁCH HÀNG   13) Tìm khách   14) Sửa hồ sơ   15) Lịch sử dịch vụ và điểm");
        System.out.println(" DANH MỤC     16) Xem dịch vụ");
        if (Auth.allowed("ADMIN", "MANAGER")) {
            System.out.println(" QUẢN LÝ      17) Sửa/ngừng dịch vụ   18) Nhân viên và ca   19) Phân ca   20) Doanh thu");
        }
        if (Auth.allowed("ADMIN")) System.out.println(" HỆ THỐNG     12) Đặt lại dữ liệu mẫu");
        System.out.println("              99) Đăng xuất   0) Thoát");
    }

    public static void main(String[] args) throws Exception {
        init("SPACARE - ĐẶT LỊCH, GÓI DỊCH VỤ VÀ TÍCH ĐIỂM");
        in = new Scanner(System.in, StandardCharsets.UTF_8);
        say(Db.persistent() ? "Chế độ: LƯU RA FILE (chạy lại vẫn còn dữ liệu)" : "Chế độ: RAM (thoát chương trình là mất dữ liệu)");
        try {
            while (true) {
                try {
                    if (Auth.user() == null) {
                        say("ĐĂNG NHẬP (tài khoản mẫu: admin / letan01 / ktv_lan / ql_hoa; mật khẩu 123456)");
                        String username = ask("Tài khoản (0 để thoát): ");
                        if (username.equals("0")) return;
                        String password;
                        if (System.console() != null) {
                            char[] secret = System.console().readPassword("Mật khẩu: ");
                            if (secret == null) return;
                            password = new String(secret); java.util.Arrays.fill(secret, '\0');
                        } else password = ask("Mật khẩu: ");
                        Auth.login(username, password);
                    }
                    printMenu();
                    switch (ask("Chọn: ")) {
                        case "1" -> cs.list().forEach(l -> say(l));
                        case "2" -> {
                            int id = cs.add(ask("Họ tên: "), ask("Số điện thoại (10 số): "));
                            say("Đã thêm khách #" + id + " (hạng SILVER, KHÁCH MỚI nên được ưu đãi trải nghiệm lần đầu)");
                        }
                        case "3" -> showOffers();
                        case "4" -> pk.customerPackages(askInt("Mã khách hàng: ")).forEach(l -> say(l));
                        case "5" -> {
                            int staff = askInt("Mã nhân viên (1 Lan, 2 Hà, 3 Minh): ");
                            List<Integer> sv = ids(ask("Mã dịch vụ, cách nhau dấu phẩy (với combo: nhập các dịch vụ trong combo): "));
                            LocalDate day = askDate();
                            List<LocalDateTime> slots = bk.findSlots(staff, day, bk.totalMinutes(sv), 0);
                            say(slots.size() + " khung giờ trống ngày " + day + ": " + slots.stream().map(t -> t.toLocalTime().toString()).toList());
                        }
                        case "6" -> book();
                        case "7" -> (Auth.allowed("TECHNICIAN") ? management.ownSchedule() : bk.list()).forEach(l -> say(l));
                        case "8" -> rescheduleOrCancel();
                        case "9" -> advance();
                        case "10" -> {
                            int appt = askInt("Mã lịch hẹn (phải đã Hoàn thành): ");
                            int redeem = Integer.parseInt(orDefault(ask("Điểm muốn đổi (bội số của 100, Enter = 0): "), "0"));
                            say(bill.pay(appt, redeem, orDefault(ask("Hình thức (CASH/CARD/TRANSFER/QR, Enter = CASH): ").toUpperCase(), "CASH")));
                        }
                        case "11" -> {
                            pk.packages().forEach(l -> say(l));
                            int cust = askInt("Mã khách hàng: ");
                            int pkg = askInt("Mã gói muốn mua: ");
                            say(bill.sellPackage(cust, pkg, orDefault(ask("Hình thức (CASH/CARD/TRANSFER/QR, Enter = CASH): ").toUpperCase(), "CASH")));
                        }
                        case "12" -> {
                            Auth.require("ADMIN");
                            if (ask("Xóa toàn bộ dữ liệu và nạp lại dữ liệu mẫu? Gõ DONGY để xác nhận: ").equals("DONGY")) { Db.reset(); Auth.logout(); say("Đã đặt lại dữ liệu mẫu. Vui lòng đăng nhập lại."); }
                            else say("Đã hủy, dữ liệu giữ nguyên.");
                        }
                        case "13" -> management.customers(ask("Tên hoặc số điện thoại: ")).forEach(l -> say(l));
                        case "14" -> {
                            management.updateCustomer(askInt("Mã khách: "), ask("Họ tên mới: "), ask("Số điện thoại mới: "));
                            say("Đã cập nhật hồ sơ.");
                        }
                        case "15" -> management.history(askInt("Mã khách: ")).forEach(l -> say(l));
                        case "16" -> management.services().forEach(l -> say(l));
                        case "17" -> {
                            management.updateService(askInt("Mã dịch vụ: "), ask("Tên: "), Long.parseLong(ask("Giá (đ): ")),
                                askInt("Thời lượng (phút): "), ask("Đang bán? (y/N): ").equalsIgnoreCase("y"));
                            say("Đã cập nhật; giá và thời gian của lịch cũ giữ nguyên.");
                        }
                        case "18" -> management.staff().forEach(l -> say(l));
                        case "19" -> {
                            management.assignShift(askInt("Mã nhân viên: "), askDate(), askInt("Mã ca: "));
                            say("Đã phân ca.");
                        }
                        case "20" -> management.revenue(LocalDate.parse(ask("Từ ngày yyyy-MM-dd: ")),
                            LocalDate.parse(ask("Đến ngày yyyy-MM-dd: "))).forEach(l -> say(l));
                        case "99" -> { Auth.logout(); say("Đã đăng xuất."); }
                        case "0" -> { say("Kết thúc."); return; }
                        default -> say("Lựa chọn không hợp lệ.");
                    }
                } catch (Eof e) {
                    return;
                } catch (NumberFormatException | DateTimeParseException e) {
                    say("[BỊ TỪ CHỐI] Dữ liệu nhập không đúng định dạng.");
                } catch (Exception e) {
                    say("[BỊ TỪ CHỐI] " + (e.getMessage() == null ? e.toString() : e.getMessage()));
                }
            }
        } finally {
            Auth.logout();
            if (Db.get() != null) Db.get().close();
        }
    }

    static String orDefault(String s, String d) { return s.isEmpty() ? d : s; }

    static void showOffers() throws Exception {
        System.out.println("  == COMBO (mua trọn gói trong 1 buổi) ==");
        pk.combos().forEach(l -> say(l));
        System.out.println("  == GÓI LIỆU TRÌNH / THẺ MEMBERSHIP (mua theo số buổi) ==");
        pk.packages().forEach(l -> say(l));
        System.out.println("  == ĐÔI / NHÓM ==");
        say("Đi 2 người: tính tiền 1 người | Nhóm 3-4 người: giảm 20% | Nhóm từ 5 người: giảm 30%");
        System.out.println("  == TRẢI NGHIỆM LẦN ĐẦU ==");
        pk.trials().forEach(l -> say(l));
        say("Mỗi hóa đơn chỉ áp MỘT ưu đãi gói tốt nhất, sau đó mới giảm theo hạng và đổi điểm (tối đa 50%).");
    }

    static void book() throws Exception {
        System.out.println("  Loại đặt lịch: 1) Dịch vụ lẻ   2) Combo   3) Dùng 1 buổi liệu trình   4) Đôi/Nhóm");
        int kind = askInt("  Chọn loại: ");
        if (kind < 1 || kind > 4) { say("Lựa chọn không hợp lệ."); return; }
        int cust = askInt("Mã khách hàng: ");
        List<Integer> sv = List.of();
        Integer combo = null, cp = null;
        int guests = 1;
        switch (kind) {
            case 1 -> sv = ids(ask("Mã dịch vụ, cách nhau dấu phẩy: "));
            case 2 -> combo = askInt("Mã combo: ");
            case 3 -> {
                cp = askInt("Mã gói của khách (xem menu 4): ");
                sv = ids(ask("Mã dịch vụ trong lịch (phải có dịch vụ của gói): "));
            }
            default -> {
                guests = askInt("Số khách (2 = đôi, từ 3 = nhóm): ");
                sv = ids(ask("Mã dịch vụ, cách nhau dấu phẩy: "));
            }
        }
        int staff = askInt("Mã nhân viên: ");
        LocalDateTime start = askStart();
        say("Đặt lịch thành công, mã lịch hẹn #" + bk.create(cust, sv, staff, start, guests, combo, cp));
    }

    static void rescheduleOrCancel() throws Exception {
        int action = askInt("  1) Dời lịch   2) Hủy lịch: ");
        int id = askInt("Mã lịch hẹn: ");
        boolean override = ask("Ghi đè chính sách 'trước giờ hẹn ít nhất 2 giờ'? (y/N): ").equalsIgnoreCase("y");
        if (action == 1) { bk.reschedule(id, askStart(), override); say("Đã dời lịch #" + id); }
        else if (action == 2) { bk.cancel(id, ask("Lý do hủy: "), override); say("Đã hủy lịch #" + id + " (buổi liệu trình đang giữ chỗ sẽ được nhả ra)"); }
        else say("Lựa chọn không hợp lệ.");
    }

    static void advance() throws Exception {
        int id = askInt("Mã lịch hẹn: ");
        switch (askInt(Auth.allowed("TECHNICIAN") ? "  2) Bắt đầu   3) Hoàn thành: " : "  1) Check-in   2) Bắt đầu   3) Hoàn thành: ")) {
            case 1 -> { st.advance(id, "CONFIRMED", "CHECKED_IN"); say("Lịch #" + id + " -> CHECKED_IN"); }
            case 2 -> { st.advance(id, "CHECKED_IN", "IN_PROGRESS"); say("Lịch #" + id + " -> IN_PROGRESS"); }
            case 3 -> { st.advance(id, "IN_PROGRESS", "COMPLETED"); say("Lịch #" + id + " -> COMPLETED (có thể thanh toán, menu 10)"); }
            default -> say("Lựa chọn không hợp lệ.");
        }
    }
}
