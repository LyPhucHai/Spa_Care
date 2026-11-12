package spacare;

import java.util.List;
import spacare.Pricing.*;

/** Kiểm tra luật tính giá bằng số liệu tính tay. Không cần CSDL: java spacare.PricingCheck */
public class PricingCheck {
    static int fails = 0, total = 0;

    static void eq(String name, Object expected, Object actual) {
        total++;
        boolean ok = expected.equals(actual);
        if (!ok) fails++;
        System.out.println((ok ? "  OK   " : "  LỖI  ") + name + (ok ? "" : "  (mong đợi " + expected + ", thực tế " + actual + ")"));
    }

    static Result p(List<Line> lines, int guests, int combo, boolean isNew, int tier, double mult, int balance, int redeem) {
        return Pricing.price(new Input(lines, guests, combo, isNew, tier, mult, balance, redeem));
    }

    static boolean throwsIllegal(Runnable r) {
        try { r.run(); return false; } catch (IllegalStateException e) { return true; }
    }

    public static void main(String[] args) {
        System.out.println("A) Combo 40%, Mai (Silver): Massage 500k + Gội 150k + Da 400k");
        Result a = p(List.of(new Line(500_000, false, 0), new Line(150_000, false, 50), new Line(400_000, false, 70)), 1, 40, false, 0, 1.0, 80, 0);
        eq("tạm tính", 1_050_000L, a.subtotal());
        eq("loại ưu đãi", "COMBO", a.promoType());
        eq("giảm combo", 420_000L, a.promoDiscount());
        eq("tổng", 630_000L, a.total());
        eq("điểm tích", 63, a.earnedPoints());

        System.out.println("B) Dùng 1 buổi liệu trình Massage (Thu H, Gold)");
        Result b = p(List.of(new Line(500_000, true, 0)), 1, 0, false, 5, 1.2, 420, 0);
        eq("tạm tính", 500_000L, b.subtotal());
        eq("giảm do gói", 500_000L, b.packageDiscount());
        eq("tổng", 0L, b.total());
        eq("điểm tích", 0, b.earnedPoints());

        System.out.println("C) Đôi (2 khách) Da cơ bản 400k, Thu H (Gold 5%, x1.2)");
        Result c = p(List.of(new Line(400_000, false, 70)), 2, 0, false, 5, 1.2, 420, 0);
        eq("loại ưu đãi", "GROUP", c.promoType());
        eq("giảm đôi (tính 1 người)", 400_000L, c.promoDiscount());
        eq("giảm hạng 5%", 20_000L, c.tierDiscount());
        eq("tổng", 380_000L, c.total());
        eq("điểm tích", 45, c.earnedPoints());

        System.out.println("D) Nhóm 3 khách Gội 150k, Linh (Platinum 10%, x1.5)");
        Result d = p(List.of(new Line(150_000, false, 50)), 3, 0, false, 10, 1.5, 1080, 0);
        eq("giảm nhóm 20%", 90_000L, d.promoDiscount());
        eq("giảm hạng 10%", 36_000L, d.tierDiscount());
        eq("tổng", 324_000L, d.total());
        eq("điểm tích", 48, d.earnedPoints());

        System.out.println("E) Khách mới, Da cơ bản 400k, trải nghiệm 70%");
        Result e = p(List.of(new Line(400_000, false, 70)), 1, 0, true, 0, 1.0, 0, 0);
        eq("loại ưu đãi", "TRIAL", e.promoType());
        eq("giảm lần đầu", 280_000L, e.promoDiscount());
        eq("tổng", 120_000L, e.total());
        eq("điểm tích", 12, e.earnedPoints());

        System.out.println("E2) Khách cũ cùng dịch vụ: không còn ưu đãi lần đầu");
        Result e2 = p(List.of(new Line(250_000, false, 60)), 1, 0, false, 0, 1.0, 12, 0);
        eq("loại ưu đãi", "NONE", e2.promoType());
        eq("tổng", 250_000L, e2.total());

        System.out.println("F) Khách mới đặt combo 30%: chỉ áp ưu đãi tốt nhất, không cộng dồn");
        Result f = p(List.of(new Line(400_000, false, 70), new Line(250_000, false, 60)), 1, 30, true, 0, 1.0, 0, 0);
        eq("giảm combo sẽ là", 195_000L, 650_000L * 30 / 100);
        eq("ưu đãi được chọn", "TRIAL", f.promoType());
        eq("giảm lần đầu", 430_000L, f.promoDiscount());
        eq("tổng", 220_000L, f.total());

        System.out.println("G) Đổi điểm tối đa 50%, Linh (Platinum), Gội 150k");
        List<Line> g = List.of(new Line(150_000, false, 50));
        eq("đổi 900 điểm bị từ chối", true, throwsIllegal(() -> p(g, 1, 0, false, 10, 1.5, 900, 900)));
        Result g2 = p(g, 1, 0, false, 10, 1.5, 900, 600);
        eq("giảm điểm", 60_000L, g2.pointDiscount());
        eq("tổng", 75_000L, g2.total());
        eq("điểm tích", 11, g2.earnedPoints());
        eq("đổi quá số dư bị từ chối", true, throwsIllegal(() -> p(g, 1, 0, false, 0, 1.0, 50, 100)));
        eq("đổi 150 điểm (không phải bội số 100) bị từ chối", true, throwsIllegal(() -> p(g, 1, 0, false, 0, 1.0, 900, 150)));

        System.out.println("H) Nhóm 5 khách, 100k/người, giảm 30%");
        Result h = p(List.of(new Line(100_000, false, 0)), 5, 0, false, 0, 1.0, 0, 0);
        eq("giảm nhóm", 150_000L, h.promoDiscount());
        eq("tổng", 350_000L, h.total());

        System.out.println();
        System.out.println(fails == 0 ? "TẤT CẢ " + total + " KIỂM TRA ĐỀU ĐÚNG." : fails + "/" + total + " KIỂM TRA SAI.");
        if (fails > 0) System.exit(1);
    }
}
