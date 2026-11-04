package spacare;

import java.time.*;
import java.util.*;
import static spacare.DemoUtil.*;

/** DEMO 2 – UC-04 + UC-17: đặt lịch, khung giờ trống, phát hiện xung đột, chuyên môn, ca làm. */
public class BookingDemo {
    public static void main(String[] a) throws Exception {
        init("DEMO 2 – ĐẶT LỊCH VÀ KIỂM TRA XUNG ĐỘT (UC-04, UC-17)");
        BookingService ap = new BookingService(); CustomerView bill = new CustomerView();
        step("Danh sách khách hàng", () -> bill.customers().forEach(c -> say(c)));
        step("Khung giờ trống của Lan (NV #1) cho Massage 90 phút ngày mai", () -> {
            List<LocalDateTime> s = ap.findSlots(1, LocalDate.now().plusDays(1), ap.totalMinutes(List.of(1)), 0);
            say(s.size() + " khung giờ, 8 khung đầu: " + s.stream().limit(8).map(t -> t.toLocalTime().toString()).toList());
        });
        step("ĐẶT LỊCH 1: Trần Mai (#2), Massage, Lan, 10:00 -> phải THÀNH CÔNG", () -> say("Mã lịch hẹn #" + ap.create(2, List.of(1), 1, at(10, 0))));
        step("ĐẶT TRÙNG: Nguyễn Thu H (#1), Gội đầu, Lan, 10:00 -> phải bị TỪ CHỐI (Lan đang làm Massage 10:00–11:30)", () -> say("Mã lịch hẹn #" + ap.create(1, List.of(2), 1, at(10, 0))));
        step("ĐẶT LIỀN KỀ: Thu H, Gội đầu, Lan, 11:30 -> THÀNH CÔNG (kết thúc 11:30 không tính là giao nhau)", () -> say("Mã lịch hẹn #" + ap.create(1, List.of(2), 1, at(11, 30))));
        step("SAI CHUYÊN MÔN: Thu H, Massage, Minh (#3 chỉ làm gội/cắt) -> TỪ CHỐI", () -> say("Mã lịch hẹn #" + ap.create(1, List.of(1), 3, at(9, 0))));
        step("NGOÀI CA: Linh, Cắt tóc, Minh, 15:00 (Minh chỉ làm ca sáng đến 14:00) -> TỪ CHỐI", () -> say("Mã lịch hẹn #" + ap.create(3, List.of(4), 3, at(15, 0))));
        step("NHIỀU DỊCH VỤ: Linh, Gội đầu + Cắt tóc (105 phút), Minh, 08:00 -> THÀNH CÔNG", () -> say("Mã lịch hẹn #" + ap.create(3, List.of(2, 4), 3, at(8, 0))));
        step("Danh sách lịch hẹn sau khi đặt", () -> ap.list().forEach(l -> say(l)));
    }
}
