package spacare;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static spacare.DemoUtil.*;

/** Menu tương tác cho UC-04 + UC-17: người dùng tự nhập khách, dịch vụ, nhân viên và giờ. */
public class BookingMenu {
    static List<Integer> ids(String s) {
        return Arrays.stream(s.split(",")).map(String::trim).map(Integer::parseInt).toList();
    }

    public static void main(String[] args) throws Exception {
        init("ĐẶT LỊCH – MENU TƯƠNG TÁC (UC-04, UC-17)");
        BookingService ap = new BookingService();
        CustomerView cv = new CustomerView();
        Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
        LocalDate day = LocalDate.now().plusDays(1);
        while (true) {
            System.out.println("\n1) Danh sách khách   2) Khung giờ trống   3) Đặt lịch   4) Danh sách lịch hẹn   0) Thoát");
            System.out.print("Chọn: ");
            if (!in.hasNextLine()) return;
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> cv.customers().forEach(c -> say(c));
                    case "2" -> {
                        System.out.print("Mã nhân viên (1 Lan, 2 Hà, 3 Minh): ");
                        int staff = Integer.parseInt(in.nextLine().trim());
                        System.out.print("Mã dịch vụ, cách nhau dấu phẩy (1 Massage, 2 Gội, 3 Da, 4 Cắt, 5 Gel): ");
                        List<Integer> sv = ids(in.nextLine());
                        List<LocalDateTime> slots = ap.findSlots(staff, day, ap.totalMinutes(sv), 0);
                        say(slots.size() + " khung giờ trống ngày " + day + ": " + slots.stream().map(t -> t.toLocalTime().toString()).toList());
                    }
                    case "3" -> {
                        System.out.print("Mã khách hàng: ");
                        int cust = Integer.parseInt(in.nextLine().trim());
                        System.out.print("Mã dịch vụ, cách nhau dấu phẩy: ");
                        List<Integer> sv = ids(in.nextLine());
                        System.out.print("Mã nhân viên: ");
                        int staff = Integer.parseInt(in.nextLine().trim());
                        System.out.print("Giờ bắt đầu ngày " + day + " (dạng HH:mm, ví dụ 09:30): ");
                        LocalDateTime start = day.atTime(LocalTime.parse(in.nextLine().trim()));
                        say("Đặt lịch thành công, mã lịch hẹn #" + ap.create(cust, sv, staff, start));
                    }
                    case "4" -> ap.list().forEach(l -> say(l));
                    case "0" -> { say("Kết thúc."); return; }
                    default -> say("Lựa chọn không hợp lệ.");
                }
            } catch (Exception e) {
                say("[BỊ TỪ CHỐI] " + (e.getMessage() == null ? e.toString() : e.getMessage()));
            }
        }
    }
}