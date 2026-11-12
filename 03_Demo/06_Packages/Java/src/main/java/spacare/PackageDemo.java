package spacare;

import java.util.*;
import static spacare.DemoUtil.*;

/** DEMO 6 - gói dịch vụ: combo, liệu trình, đôi/nhóm, trải nghiệm lần đầu, cùng tích điểm và nâng hạng. */
public class PackageDemo {
    static int a1, a2, a3, a4, a5, a6, a7, newCustomer;

    public static void main(String[] x) throws Exception {
        init("DEMO 6 - GÓI DỊCH VỤ + TÍCH ĐIỂM (COMBO, LIỆU TRÌNH, ĐÔI/NHÓM, LẦN ĐẦU)");
        BookingService bk = new BookingService(); StatusService st = new StatusService(); BillingService bill = new BillingService();
        PackageService pk = new PackageService(); CustomerService cs = new CustomerService();

        step("Khách hàng ban đầu", () -> cs.list().forEach(l -> say(l)));
        step("Combo, gói liệu trình và ưu đãi trải nghiệm đang bán", () -> {
            pk.combos().forEach(l -> say(l)); pk.packages().forEach(l -> say(l)); pk.trials().forEach(l -> say(l));
        });
        step("Thu H (#1) đã mua sẵn Thẻ Massage 10 buổi, còn 6 buổi", () -> pk.customerPackages(1).forEach(l -> say(l)));

        step("A) COMBO: Mai (Silver) đặt 'Combo Thư giãn' (Massage + Gội + Da, -40%) với Lan lúc 09:00", () -> {
            a1 = bk.create(2, List.of(), 1, at(9, 0), 1, 1, null);
            say("Mã lịch hẹn #" + a1 + " (3 dịch vụ liền nhau, 210 phút)");
        });
        step("A) Hoàn thành rồi thanh toán: 1.050.000 - combo 420.000 = 630.000 đ, +63 điểm, chi tiêu vượt 10 triệu -> NÂNG HẠNG GOLD", () -> {
            st.complete(a1); say(bill.pay(a1, 0, "CASH"));
        });

        step("B1) LIỆU TRÌNH: Mai dùng gói của Thu H -> TỪ CHỐI (gói không thuộc Mai)", () -> say("#" + bk.create(2, List.of(1), 1, at(14, 0), 1, null, 1)));
        step("B2) Thu H dùng Thẻ Massage cho lịch Gội đầu -> TỪ CHỐI (sai dịch vụ của gói)", () -> say("#" + bk.create(1, List.of(2), 1, at(14, 0), 1, null, 1)));
        step("B3) Thu H đặt Massage với Lan 14:00, dùng 1 buổi của gói #1 -> THÀNH CÔNG, buổi được giữ chỗ", () -> {
            a2 = bk.create(1, List.of(1), 1, at(14, 0), 1, null, 1);
            say("Mã lịch hẹn #" + a2);
            pk.customerPackages(1).forEach(l -> say(l));
        });
        step("B4) Hoàn thành và thanh toán: buổi đã trả bằng gói -> 0 đ, gói còn 5/10, không tích điểm", () -> {
            st.complete(a2); say(bill.pay(a2, 0, "CASH"));
            pk.customerPackages(1).forEach(l -> say(l));
        });

        step("C) MUA GÓI: Linh (Platinum) mua 'Liệu trình trị mụn 5 buổi' 1.200.000 đ -> +180 điểm (x1.5)", () -> {
            say(bill.sellPackage(3, 1, "CARD"));
            pk.customerPackages(3).forEach(l -> say(l));
        });
        step("C) Linh đặt Da cơ bản với Hà 11:00, dùng buổi của gói vừa mua (gói của Linh là #2) -> THÀNH CÔNG", () -> {
            a3 = bk.create(3, List.of(3), 2, at(11, 0), 1, null, 2);
            say("Mã lịch hẹn #" + a3);
            pk.customerPackages(3).forEach(l -> say(l));
        });

        step("D1) ĐÔI: Thu H đi 2 người, Da cơ bản 400.000 x2 = 800.000, tính tiền 1 người, Hà 10:00", () -> {
            a4 = bk.create(1, List.of(3), 2, at(10, 0), 2, null, null);
            st.complete(a4);
            say("Mã lịch hẹn #" + a4 + " | giảm đôi 400.000 + giảm Gold 5% = 20.000 -> 380.000 đ, +45 điểm");
            say(bill.pay(a4, 0, "CASH"));
        });
        step("D2) NHÓM: Linh đi 3 người, Gội đầu 150.000 x3 = 450.000, giảm nhóm 20% = 90.000, Platinum 10% = 36.000 -> 324.000 đ, +48 điểm", () -> {
            a5 = bk.create(3, List.of(2), 3, at(8, 0), 3, null, null);
            st.complete(a5);
            say("Mã lịch hẹn #" + a5);
            say(bill.pay(a5, 0, "QR"));
        });

        step("E1) LẦN ĐẦU: thêm khách mới Phạm An (Silver, khách mới)", () -> {
            newCustomer = cs.add("Phạm An", "0933000111");
            say("Mã khách #" + newCustomer);
        });
        step("E2) Phạm An đặt Da cơ bản 400.000 với Hà 13:00 -> trải nghiệm 70% = 280.000, còn 120.000 đ, +12 điểm", () -> {
            a6 = bk.create(newCustomer, List.of(3), 2, at(13, 0));
            st.complete(a6);
            say("Mã lịch hẹn #" + a6);
            say(bill.pay(a6, 0, "CASH"));
        });
        step("E3) Phạm An quay lại Sơn gel 250.000 với Hà 14:30 -> HẾT ưu đãi lần đầu, trả đủ 250.000 đ, +25 điểm", () -> {
            a7 = bk.create(newCustomer, List.of(5), 2, at(14, 30));
            st.complete(a7);
            say("Mã lịch hẹn #" + a7);
            say(bill.pay(a7, 0, "CASH"));
        });

        step("Kết quả cuối: khách hàng, gói của Thu H và Linh, danh sách lịch hẹn", () -> {
            cs.list().forEach(l -> say(l));
            pk.customerPackages(1).forEach(l -> say(l));
            pk.customerPackages(3).forEach(l -> say(l));
            bk.list().forEach(l -> say(l));
        });
    }
}
