package spacare;

import java.util.*;
import static spacare.DemoUtil.*;

/** DEMO 4 – UC-06: check-in, bắt đầu, hoàn thành dịch vụ (máy trạng thái). */
public class ServiceDemo {
    public static void main(String[] a) throws Exception {
        init("DEMO 4 – CHECK-IN VÀ THỰC HIỆN DỊCH VỤ (UC-06)");
        BookingService ap = new BookingService(); StatusService st = new StatusService();
        step("Chuẩn bị: lịch #1 Mai/Massage/Lan 10:00 (trạng thái CONFIRMED)", () -> { say("Đã tạo #" + ap.create(2, List.of(1), 1, at(10, 0))); ap.list().forEach(l -> say(l)); });
        step("Nhảy cóc: chuyển thẳng IN_PROGRESS -> COMPLETED -> TỪ CHỐI", () -> st.advance(1, "IN_PROGRESS", "COMPLETED"));
        step("Bước 1 – Lễ tân Check-in: CONFIRMED -> CHECKED_IN", () -> { st.advance(1, "CONFIRMED", "CHECKED_IN"); ap.list().forEach(l -> say(l)); });
        step("Bước 2 – Kỹ thuật viên Bắt đầu: CHECKED_IN -> IN_PROGRESS", () -> { st.advance(1, "CHECKED_IN", "IN_PROGRESS"); ap.list().forEach(l -> say(l)); });
        step("Check-in lần nữa -> TỪ CHỐI (không còn ở CONFIRMED)", () -> st.advance(1, "CONFIRMED", "CHECKED_IN"));
        step("Bước 3 – Hoàn thành: IN_PROGRESS -> COMPLETED", () -> { st.advance(1, "IN_PROGRESS", "COMPLETED"); ap.list().forEach(l -> say(l)); });
    }
}
