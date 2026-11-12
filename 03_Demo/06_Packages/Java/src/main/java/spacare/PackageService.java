package spacare;

import java.sql.*;
import java.util.*;

/** Xem combo, gói liệu trình, ưu đãi trải nghiệm và gói của từng khách (UC-12). */
public class PackageService {
    private Connection c() { return Db.get(); }

    /** Số buổi còn dùng được = số buổi còn lại - số buổi đang giữ cho lịch chưa hủy và chưa thanh toán. */
    static int available(Connection c, int cpId) throws SQLException {
        int remaining;
        try (PreparedStatement p = c.prepareStatement("SELECT remaining_sessions FROM customer_package WHERE id=?")) {
            p.setInt(1, cpId);
            try (ResultSet r = p.executeQuery()) { if (!r.next()) return 0; remaining = r.getInt(1); }
        }
        String sql = "SELECT COUNT(*) FROM appointment_service s JOIN appointment a ON a.id=s.appointment_id "
                + "WHERE s.customer_package_id=? AND a.status NOT IN ('CANCELLED','NO_SHOW') "
                + "AND NOT EXISTS (SELECT 1 FROM invoice i WHERE i.appointment_id=a.id)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, cpId);
            try (ResultSet r = p.executeQuery()) { r.next(); return remaining - r.getInt(1); }
        }
    }

    public List<String> combos() throws SQLException {
        List<int[]> head = new ArrayList<>(); List<String> names = new ArrayList<>();
        try (Statement s = c().createStatement(); ResultSet r = s.executeQuery("SELECT id, name, discount_percent FROM combo WHERE is_active=TRUE ORDER BY id")) {
            while (r.next()) { head.add(new int[]{r.getInt(1), r.getInt(3)}); names.add(r.getString(2)); }
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < head.size(); i++) {
            List<String> items = new ArrayList<>(); long retail = 0;
            try (PreparedStatement p = c().prepareStatement("SELECT sv.name, sv.price FROM combo_item ci JOIN service sv ON sv.id=ci.service_id WHERE ci.combo_id=? ORDER BY sv.id")) {
                p.setInt(1, head.get(i)[0]);
                try (ResultSet r = p.executeQuery()) { while (r.next()) { items.add(r.getString(1)); retail += r.getLong(2); } }
            }
            int pct = head.get(i)[1];
            out.add(String.format("Combo #%d %s | giảm %d%% | %s | giá lẻ %,d đ -> còn %,d đ", head.get(i)[0], names.get(i), pct, String.join(" + ", items), retail, retail * (100 - pct) / 100));
        }
        return out;
    }

    public List<String> packages() throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = "SELECT p.id, p.name, sv.name, p.total_sessions, p.price, sv.price, p.valid_days FROM service_package p JOIN service sv ON sv.id=p.service_id ORDER BY p.id";
        try (Statement s = c().createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                long retail = r.getLong(6) * r.getInt(4), price = r.getLong(5);
                out.add(String.format("Gói #%d %s | %d buổi %s | giá gói %,d đ (lẻ %,d đ, rẻ hơn %d%%) | hạn %d ngày",
                        r.getInt(1), r.getString(2), r.getInt(4), r.getString(3), price, retail, retail == 0 ? 0 : (retail - price) * 100 / retail, r.getInt(7)));
            }
        }
        return out;
    }

    /** Dịch vụ có ưu đãi trải nghiệm cho khách mới. */
    public List<String> trials() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement s = c().createStatement(); ResultSet r = s.executeQuery("SELECT name, trial_percent FROM service WHERE trial_percent > 0 AND is_active=TRUE ORDER BY id")) {
            while (r.next()) out.add(String.format("Trải nghiệm lần đầu: %s giảm %d%% (chỉ khách mới)", r.getString(1), r.getInt(2)));
        }
        return out;
    }

    public List<String> customerPackages(int customerId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT cp.id, p.name, cp.remaining_sessions, p.total_sessions, cp.expires_at FROM customer_package cp JOIN service_package p ON p.id=cp.package_id WHERE cp.customer_id=? ORDER BY cp.id";
        try (PreparedStatement p = c().prepareStatement(sql)) {
            p.setInt(1, customerId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) rows.add(new Object[]{r.getInt(1), r.getString(2), r.getInt(3), r.getInt(4), r.getTimestamp(5).toString().substring(0, 10)});
            }
        }
        List<String> out = new ArrayList<>();
        for (Object[] x : rows) {
            int id = (Integer) x[0], remaining = (Integer) x[2];
            int reserved = remaining - available(c(), id);
            out.add(String.format("Gói của khách #%d: %s | còn %d/%d buổi%s | hạn %s", id, x[1], remaining, x[3], reserved > 0 ? " (đang giữ " + reserved + " buổi cho lịch chưa thanh toán)" : "", x[4]));
        }
        if (out.isEmpty()) out.add("Khách #" + customerId + " chưa có gói liệu trình nào.");
        return out;
    }
}
