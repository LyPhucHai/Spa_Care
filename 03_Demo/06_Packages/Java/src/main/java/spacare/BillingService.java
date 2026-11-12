package spacare;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * UC-07 + UC-08 + UC-12: thanh toán, ưu đãi gói, tích điểm, đổi điểm, nâng hạng, bán gói liệu trình.
 * Mọi con số do Pricing tính; lớp này chỉ đọc và ghi CSDL trong một giao dịch.
 */
public class BillingService {
    private record Cust(int tierId, int balance, long spending, int tierPercent, double mult, String tierName, boolean isNew) {}

    private Cust loadCust(Connection c, int id) throws SQLException {
        String sql = "SELECT cu.tier_id, cu.point_balance, cu.spending_12m, t.discount_percent, t.point_multiplier, t.name, cu.is_new "
                + "FROM customer cu JOIN membership_tier t ON t.id=cu.tier_id WHERE cu.id=?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("Không tìm thấy khách hàng #" + id);
                return new Cust(r.getInt(1), r.getInt(2), r.getLong(3), r.getInt(4), r.getDouble(5), r.getString(6), r.getBoolean(7));
            }
        }
    }

    /** UC-07: thanh toán lịch đã hoàn thành. */
    public String pay(int apptId, int redeemPoints, String method) throws SQLException {
        Connection c = Db.get(); c.setAutoCommit(false);
        try {
            int custId, guests, comboId; String status;
            try (PreparedStatement p = c.prepareStatement("SELECT customer_id,status,guest_count,combo_id FROM appointment WHERE id=?")) {
                p.setInt(1, apptId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) throw new IllegalStateException("Không tìm thấy lịch hẹn #" + apptId);
                    custId = r.getInt(1); status = r.getString(2); guests = r.getInt(3); comboId = r.getInt(4);
                }
            }
            if (!status.equals("COMPLETED")) throw new IllegalStateException("Chỉ thanh toán lịch đã hoàn thành (hiện tại: " + status + ")");
            try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM invoice WHERE appointment_id=?")) {
                p.setInt(1, apptId); try (ResultSet r = p.executeQuery()) { if (r.next()) throw new IllegalStateException("Lịch hẹn này đã được thanh toán"); }
            }
            List<Pricing.Line> lines = new ArrayList<>(); List<Integer> pkgIds = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement("SELECT s.price_at_booking, s.customer_package_id, sv.trial_percent FROM appointment_service s JOIN service sv ON sv.id=s.service_id WHERE s.appointment_id=? ORDER BY s.id")) {
                p.setInt(1, apptId);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        long price = r.getLong(1); int cp = r.getInt(2); boolean byPkg = !r.wasNull();
                        lines.add(new Pricing.Line(price, byPkg, r.getInt(3)));
                        if (byPkg) pkgIds.add(cp);
                    }
                }
            }
            int comboPct = 0;
            if (comboId > 0) try (PreparedStatement p = c.prepareStatement("SELECT discount_percent FROM combo WHERE id=?")) {
                p.setInt(1, comboId); try (ResultSet r = p.executeQuery()) { if (r.next()) comboPct = r.getInt(1); }
            }
            Cust cu = loadCust(c, custId);
            Pricing.Result res = Pricing.price(new Pricing.Input(lines, guests, comboPct, cu.isNew(), cu.tierPercent(), cu.mult(), cu.balance(), redeemPoints));

            int invId;
            try (PreparedStatement p = c.prepareStatement("INSERT INTO invoice(appointment_id,customer_id,subtotal,package_discount,promo_type,promo_discount,tier_discount,point_discount,total,status) VALUES (?,?,?,?,?,?,?,?,?,'PAID')", Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, apptId); p.setInt(2, custId); p.setLong(3, res.subtotal()); p.setLong(4, res.packageDiscount()); p.setString(5, res.promoType());
                p.setLong(6, res.promoDiscount()); p.setLong(7, res.tierDiscount()); p.setLong(8, res.pointDiscount()); p.setLong(9, res.total()); p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) { k.next(); invId = k.getInt(1); }
            }
            if (res.total() > 0) try (PreparedStatement p = c.prepareStatement("INSERT INTO payment(invoice_id,method,amount) VALUES (?,?,?)")) {
                p.setInt(1, invId); p.setString(2, method); p.setLong(3, res.total()); p.executeUpdate();
            }
            for (int cp : pkgIds) try (PreparedStatement p = c.prepareStatement("UPDATE customer_package SET remaining_sessions = remaining_sessions - 1 WHERE id=? AND remaining_sessions > 0")) {
                p.setInt(1, cp);
                if (p.executeUpdate() == 0) throw new IllegalStateException("Gói #" + cp + " đã hết buổi");
            }
            String loyalty = applyLoyalty(c, custId, cu, invId, res.total(), redeemPoints, res.earnedPoints());
            c.commit();
            String pkgText = pkgIds.isEmpty() ? "" : String.format(" | Dùng %d buổi gói -%,d", pkgIds.size(), res.packageDiscount());
            return String.format("HÓA ĐƠN #%d | Tạm tính %,d%s | Ưu đãi %s -%,d | Giảm hạng %s -%,d | Đổi điểm -%,d%n  => TỔNG %,d đ (%s)%n  %s",
                    invId, res.subtotal(), pkgText, res.promoType(), res.promoDiscount(), cu.tierName(), res.tierDiscount(), res.pointDiscount(), res.total(), method, loyalty);
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
    }

    /** UC-12: bán gói liệu trình cho khách. Giá gói đã là giá ưu đãi nên không giảm thêm, nhưng vẫn tích điểm. */
    public String sellPackage(int custId, int packageId, String method) throws SQLException {
        Connection c = Db.get(); c.setAutoCommit(false);
        try {
            Cust cu = loadCust(c, custId);
            String name; long price; int sessions, days;
            try (PreparedStatement p = c.prepareStatement("SELECT name, price, total_sessions, valid_days FROM service_package WHERE id=?")) {
                p.setInt(1, packageId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) throw new IllegalStateException("Không tìm thấy gói #" + packageId);
                    name = r.getString(1); price = r.getLong(2); sessions = r.getInt(3); days = r.getInt(4);
                }
            }
            int invId;
            try (PreparedStatement p = c.prepareStatement("INSERT INTO invoice(customer_id,subtotal,package_discount,promo_type,promo_discount,tier_discount,point_discount,total,status) VALUES (?,?,0,'PACKAGE',0,0,0,?,'PAID')", Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, custId); p.setLong(2, price); p.setLong(3, price); p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) { k.next(); invId = k.getInt(1); }
            }
            if (price > 0) try (PreparedStatement p = c.prepareStatement("INSERT INTO payment(invoice_id,method,amount) VALUES (?,?,?)")) {
                p.setInt(1, invId); p.setString(2, method); p.setLong(3, price); p.executeUpdate();
            }
            LocalDateTime expires = LocalDateTime.now().plusDays(days);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO customer_package(customer_id,package_id,remaining_sessions,expires_at) VALUES (?,?,?,?)")) {
                p.setInt(1, custId); p.setInt(2, packageId); p.setInt(3, sessions); p.setTimestamp(4, Timestamp.valueOf(expires)); p.executeUpdate();
            }
            String loyalty = applyLoyalty(c, custId, cu, invId, price, 0, Pricing.earn(price, cu.mult()));
            c.commit();
            return String.format("BÁN GÓI \"%s\" | HÓA ĐƠN #%d | %,d đ (%s)%n  Khách có %d buổi, hạn %s%n  %s", name, invId, price, method, sessions, expires.toLocalDate(), loyalty);
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
    }

    /** UC-08: ghi đổi điểm, tích điểm, cộng chi tiêu và tự nâng hạng. Khách không còn là khách mới sau hóa đơn đầu tiên. */
    private String applyLoyalty(Connection c, int custId, Cust cu, int invId, long total, int redeem, int earn) throws SQLException {
        int balance = cu.balance();
        if (redeem > 0) { balance -= redeem; log(c, custId, invId, "REDEEM", -redeem, balance); }
        if (earn > 0) { balance += earn; log(c, custId, invId, "EARN", earn, balance); }
        long spending = cu.spending() + total;
        int newTier = cu.tierId(); String newName = cu.tierName();
        try (PreparedStatement p = c.prepareStatement("SELECT id,name FROM membership_tier WHERE min_spending<=? ORDER BY min_spending DESC LIMIT 1")) {
            p.setLong(1, spending);
            try (ResultSet r = p.executeQuery()) { if (r.next() && r.getInt(1) > cu.tierId()) { newTier = r.getInt(1); newName = r.getString(2); } }
        }
        try (PreparedStatement p = c.prepareStatement("UPDATE customer SET point_balance=?, spending_12m=?, tier_id=?, is_new=FALSE WHERE id=?")) {
            p.setInt(1, balance); p.setLong(2, spending); p.setInt(3, newTier); p.setInt(4, custId); p.executeUpdate();
        }
        return String.format("+%d điểm (số dư %d) | Chi tiêu 12 tháng %,d | Hạng: %s%s", earn, balance, spending, newName, newTier != cu.tierId() ? "  <<< NÂNG HẠNG!" : "");
    }

    private void log(Connection c, int cust, int inv, String type, int pts, int after) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO loyalty_transaction(customer_id,invoice_id,type,points,balance_after) VALUES (?,?,?,?,?)")) {
            p.setInt(1, cust); p.setInt(2, inv); p.setString(3, type); p.setInt(4, pts); p.setInt(5, after); p.executeUpdate();
        }
    }
}
