package spacare;

import java.util.*;
import static spacare.DemoUtil.*;

/** DEMO 5 – UC-07 + UC-08: thanh toán, giảm giá theo hạng, đổi điểm, tích điểm, nâng hạng. */
public class PaymentDemo {
    static void complete(StatusService ap, int id) throws Exception {
        ap.advance(id, "CONFIRMED", "CHECKED_IN"); ap.advance(id, "CHECKED_IN", "IN_PROGRESS"); ap.advance(id, "IN_PROGRESS", "COMPLETED");
    }
    public static void main(String[] x) throws Exception {
        init("DEMO 5 – THANH TOÁN, TÍCH ĐIỂM, NÂNG HẠNG (UC-07, UC-08)");
        BookingService bk = new BookingService(); StatusService st = new StatusService(); BillingService bill = new BillingService();
        step("Trạng thái khách hàng TRƯỚC khi thanh toán", () -> bill.customers().forEach(c -> say(c)));
        step("Chuẩn bị 4 lịch: #1 Mai/Massage, #2 Thu H/Massage+Da, #3 Linh/Gội đầu, #4 Mai/Sơn gel (giữ CONFIRMED)", () -> {
            say("#" + bk.create(2, List.of(1), 1, at(10, 0)));
            say("#" + bk.create(1, List.of(1, 3), 1, at(12, 0)));
            say("#" + bk.create(3, List.of(2), 1, at(15, 0)));
            say("#" + bk.create(2, List.of(5), 2, at(9, 0)));
            complete(st, 1); complete(st, 2); complete(st, 3);
            say("Đã hoàn thành #1, #2, #3");
        });
        step("A) Mai (SILVER, 9,5 triệu) thanh toán #1, đổi 0 điểm, CASH -> 500.000 đ, +50 điểm, chi tiêu 10 triệu -> NÂNG HẠNG GOLD", () -> say(bill.pay(1, 0, "CASH")));
        step("B) Thu H (GOLD) thanh toán #2 (900.000 đ), đổi 200 điểm, CARD -> giảm hạng 5%, giảm điểm 20.000 đ", () -> say(bill.pay(2, 200, "CARD")));
        step("C1) Linh (PLATINUM) đổi 900 điểm cho #3 -> TỪ CHỐI (vượt 50% giá trị hóa đơn)", () -> say(bill.pay(3, 900, "QR")));
        step("C2) Linh đổi 600 điểm cho #3 -> THÀNH CÔNG (giảm hạng 10% + 60.000 đ điểm)", () -> say(bill.pay(3, 600, "QR")));
        step("D1) Thanh toán #4 khi chưa hoàn thành -> TỪ CHỐI", () -> say(bill.pay(4, 0, "CASH")));
        step("D2) Thanh toán lại #1 -> TỪ CHỐI (đã thanh toán)", () -> say(bill.pay(1, 0, "CASH")));
        step("Trạng thái khách hàng SAU khi thanh toán", () -> bill.customers().forEach(c -> say(c)));
    }
}
