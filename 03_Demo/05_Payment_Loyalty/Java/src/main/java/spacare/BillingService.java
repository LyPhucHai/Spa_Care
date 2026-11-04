package spacare;

import java.sql.*;

/** UC-07 + UC-08: thanh toán, tích điểm, đổi điểm, tự động nâng hạng. */
public class BillingService {
    static final int POINT_BLOCK = 100, POINT_BLOCK_VND = 10_000, VND_PER_POINT = 10_000;

    public String pay(int apptId, int redeemPoints, String method) throws SQLException {
        Connection c = Db.get(); c.setAutoCommit(false);
        try {
            int custId; String status;
            try (PreparedStatement p = c.prepareStatement("SELECT customer_id,status FROM appointment WHERE id=?")) {
                p.setInt(1, apptId);
                try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new IllegalStateException("Không tìm thấy lịch hẹn #" + apptId); custId = r.getInt(1); status = r.getString(2); }
            }
            if (!status.equals("COMPLETED")) throw new IllegalStateException("Chỉ thanh toán lịch đã hoàn thành (hiện tại: " + status + ")");
            try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM invoice WHERE appointment_id=?")) {
                p.setInt(1, apptId); try (ResultSet r = p.executeQuery()) { if (r.next()) throw new IllegalStateException("Lịch hẹn này đã được thanh toán"); }
            }
            long subtotal;
            try (PreparedStatement p = c.prepareStatement("SELECT SUM(price_at_booking) FROM appointment_service WHERE appointment_id=?")) {
                p.setInt(1, apptId); try (ResultSet r = p.executeQuery()) { r.next(); subtotal = r.getLong(1); }
            }
            int tierId, balance; long spending; int discPct; double mult; String tierName;
            try (PreparedStatement p = c.prepareStatement("SELECT cu.tier_id, cu.point_balance, cu.spending_12m, t.discount_percent, t.point_multiplier, t.name FROM customer cu JOIN membership_tier t ON t.id=cu.tier_id WHERE cu.id=?")) {
                p.setInt(1, custId);
                try (ResultSet r = p.executeQuery()) { r.next(); tierId = r.getInt(1); balance = r.getInt(2); spending = r.getLong(3); discPct = r.getInt(4); mult = r.getDouble(5); tierName = r.getString(6); }
            }
            long tierDisc = subtotal * discPct / 100, afterTier = subtotal - tierDisc;
            if (redeemPoints > balance) throw new IllegalStateException("Không đủ điểm (còn " + balance + ")");
            long pointDisc = (long) (redeemPoints / POINT_BLOCK) * POINT_BLOCK_VND;
            if (pointDisc > afterTier / 2) throw new IllegalStateException("Chỉ được đổi điểm tối đa 50% giá trị hóa đơn (" + afterTier / 2 + " đ)");
            long total = afterTier - pointDisc;
            int invId;
            try (PreparedStatement p = c.prepareStatement("INSERT INTO invoice(appointment_id,customer_id,subtotal,tier_discount,point_discount,total,status) VALUES (?,?,?,?,?,?,'PAID')", Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, apptId); p.setInt(2, custId); p.setLong(3, subtotal); p.setLong(4, tierDisc); p.setLong(5, pointDisc); p.setLong(6, total); p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) { k.next(); invId = k.getInt(1); }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO payment(invoice_id,method,amount) VALUES (?,?,?)")) { p.setInt(1, invId); p.setString(2, method); p.setLong(3, Math.max(total, 1)); p.executeUpdate(); }
            if (redeemPoints > 0) { balance -= redeemPoints; log(c, custId, invId, "REDEEM", -redeemPoints, balance); }
            int earn = (int) Math.floor(total / (double) VND_PER_POINT * mult);
            balance += earn; log(c, custId, invId, "EARN", earn, balance);
            spending += total;
            int newTier = tierId; String newName = tierName;
            try (PreparedStatement p = c.prepareStatement("SELECT id,name FROM membership_tier WHERE min_spending<=? ORDER BY min_spending DESC LIMIT 1")) {
                p.setLong(1, spending);
                try (ResultSet r = p.executeQuery()) { if (r.next() && r.getInt(1) > tierId) { newTier = r.getInt(1); newName = r.getString(2); } }
            }
            try (PreparedStatement p = c.prepareStatement("UPDATE customer SET point_balance=?, spending_12m=?, tier_id=? WHERE id=?")) { p.setInt(1, balance); p.setLong(2, spending); p.setInt(3, newTier); p.setInt(4, custId); p.executeUpdate(); }
            c.commit();
            return String.format("HÓA ĐƠN #%d | Tạm tính %,d | Giảm hạng %s %,d | Giảm điểm %,d | TỔNG %,d đ (%s)%n+%d điểm (số dư %d) | Chi tiêu 12 tháng %,d | Hạng: %s%s",
                    invId, subtotal, tierName, tierDisc, pointDisc, total, method, earn, balance, spending, newName, newTier != tierId ? "  <<< NÂNG HẠNG!" : "");
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
    }

    private void log(Connection c, int cust, int inv, String type, int pts, int after) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO loyalty_transaction(customer_id,invoice_id,type,points,balance_after) VALUES (?,?,?,?,?)")) {
            p.setInt(1, cust); p.setInt(2, inv); p.setString(3, type); p.setInt(4, pts); p.setInt(5, after); p.executeUpdate();
        }
    }

    public java.util.List<String> customers() throws SQLException {
        java.util.List<String> out = new java.util.ArrayList<>();
        try (Statement s = Db.get().createStatement(); ResultSet r = s.executeQuery("SELECT cu.id, cu.full_name, cu.phone, t.name, cu.point_balance, cu.spending_12m FROM customer cu JOIN membership_tier t ON t.id=cu.tier_id ORDER BY cu.id")) {
            while (r.next()) out.add(String.format("#%d %s (%s) | Hạng %s | %d điểm | Chi tiêu 12 tháng %,d đ", r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getInt(5), r.getLong(6)));
        }
        return out;
    }
}
