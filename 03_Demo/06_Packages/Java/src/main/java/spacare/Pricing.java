package spacare;

import java.util.List;

/**
 * Luật tính giá thuần, không dùng CSDL nên kiểm tra được bằng PricingCheck.
 * Thứ tự áp dụng:
 *  1) Buổi liệu trình: dòng dịch vụ trả bằng buổi của gói thì không tính tiền.
 *  2) MỘT ưu đãi gói tốt nhất trong: Combo, Đôi/Nhóm, Trải nghiệm lần đầu (không cộng dồn).
 *  3) Giảm theo hạng thành viên.
 *  4) Đổi điểm (100 điểm = 10.000 đ, tối đa 50% giá trị sau bước 3).
 */
public final class Pricing {
    private Pricing() {}

    public static final int POINT_BLOCK = 100, POINT_BLOCK_VND = 10_000, VND_PER_POINT = 10_000;

    /** Một dòng dịch vụ: giá niêm yết, có trả bằng buổi liệu trình không, % giảm cho khách mới (0 = không áp dụng). */
    public record Line(long price, boolean byPackage, int trialPercent) {}

    /** comboPercent = 0 nếu lịch hẹn không đặt theo combo. */
    public record Input(List<Line> lines, int guests, int comboPercent, boolean newCustomer,
                        int tierPercent, double pointMultiplier, int balance, int redeemPoints) {}

    public record Result(long subtotal, long packageDiscount, String promoType, long promoDiscount,
                         long tierDiscount, long pointDiscount, long total, int earnedPoints) {}

    /** Đôi (2 người): tính tiền 1 người. Nhóm 3-4 người: giảm 20%. Nhóm từ 5 người: giảm 30%. */
    public static long groupDiscount(long base, int guests) {
        if (guests == 2) return base / 2;
        if (guests >= 5) return base * 30 / 100;
        if (guests >= 3) return base * 20 / 100;
        return 0;
    }

    /** Điểm tích được = tổng tiền / 10.000 nhân hệ số hạng, làm tròn xuống. */
    public static int earn(long total, double multiplier) {
        return (int) Math.floor(total / (double) VND_PER_POINT * multiplier);
    }

    public static Result price(Input in) {
        long retail = 0, byPkg = 0, trial = 0;
        for (Line l : in.lines()) {
            if (l.byPackage()) byPkg += l.price();
            else { retail += l.price(); trial += l.price() * l.trialPercent() / 100; }
        }
        long base = retail * in.guests();
        long combo = in.comboPercent() > 0 ? base * in.comboPercent() / 100 : 0;
        long group = groupDiscount(base, in.guests());
        long first = in.newCustomer() && in.guests() == 1 ? trial : 0;

        String type = "NONE";
        long best = 0;
        if (combo > best) { best = combo; type = "COMBO"; }
        if (group > best) { best = group; type = "GROUP"; }
        if (first > best) { best = first; type = "TRIAL"; }

        long afterPromo = base - best;
        long tier = afterPromo * in.tierPercent() / 100;
        long afterTier = afterPromo - tier;

        int redeem = in.redeemPoints();
        if (redeem < 0 || redeem % POINT_BLOCK != 0) throw new IllegalStateException("Số điểm đổi phải là bội số của " + POINT_BLOCK);
        if (redeem > in.balance()) throw new IllegalStateException("Không đủ điểm (còn " + in.balance() + ")");
        long pointDisc = (long) (redeem / POINT_BLOCK) * POINT_BLOCK_VND;
        if (pointDisc > afterTier / 2) throw new IllegalStateException("Chỉ được đổi điểm tối đa 50% giá trị hóa đơn (" + afterTier / 2 + " đ)");

        long total = afterTier - pointDisc;
        return new Result(base + byPkg, byPkg, type, best, tier, pointDisc, total, earn(total, in.pointMultiplier()));
    }
}
