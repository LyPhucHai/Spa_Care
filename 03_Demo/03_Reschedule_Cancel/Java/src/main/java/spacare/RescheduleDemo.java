package spacare;

import java.util.*;
import static spacare.DemoUtil.*;

/** DEMO 3 – UC-05: dời lịch và hủy lịch. */
public class RescheduleDemo {
    public static void main(String[] a) throws Exception {
        init("DEMO 3 – DỜI VÀ HỦY LỊCH HẸN (UC-05)");
        RescheduleService ap = new RescheduleService(); StatusService st = new StatusService();
        step("Chuẩn bị: lịch #1 Mai/Massage/Lan 10:00 và lịch #2 Thu H/Gội đầu/Lan 14:00", () -> {
            say("Đã tạo #" + ap.create(2, List.of(1), 1, at(10, 0)));
            say("Đã tạo #" + ap.create(1, List.of(2), 1, at(14, 0)));
        });
        step("DỜI #1 sang 14:00 -> TỪ CHỐI (trùng lịch #2 của Lan)", () -> ap.reschedule(1, at(14, 0), false));
        step("DỜI #1 sang 16:00 -> THÀNH CÔNG", () -> { ap.reschedule(1, at(16, 0), false); say("Đã dời #1 sang 16:00"); });
        step("DỜI #1 sang 15:30 -> THÀNH CÔNG (dời sát lịch #2 kết thúc 15:00; lịch cũ của chính #1 không tự chặn mình)", () -> { ap.reschedule(1, at(15, 30), false); say("Đã dời #1 sang 15:30"); });
        step("HỦY #2 với lý do 'Khách bận' -> THÀNH CÔNG", () -> { ap.cancel(2, "Khách bận", false); say("Đã hủy #2, khung 14:00 được giải phóng"); });
        step("HỦY #2 lần nữa -> TỪ CHỐI (đã ở trạng thái CANCELLED)", () -> ap.cancel(2, "Hủy lại", false));
        step("Tạo lịch #3 Linh/Cắt tóc/Minh 09:00, check-in, rồi thử hủy -> TỪ CHỐI (đã CHECKED_IN)", () -> {
            say("Đã tạo #" + ap.create(3, List.of(4), 3, at(9, 0)));
            st.advance(3, "CONFIRMED", "CHECKED_IN");
            ap.cancel(3, "Thử hủy", false);
        });
        step("Danh sách lịch hẹn cuối cùng", () -> ap.list().forEach(l -> say(l)));
        say("Ghi chú: chính sách 'dời/hủy trước giờ hẹn tối thiểu 2 giờ' nằm ở AppointmentService.MIN_NOTICE_HOURS; lễ tân/quản lý có quyền ghi đè (tham số override).");
    }
}
