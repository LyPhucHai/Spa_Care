package spacare;

import java.sql.*;

/** Danh sách khách hàng, hạng và điểm (dùng để minh họa trước/sau). */
public class CustomerView {
    public java.util.List<String> customers() throws SQLException {
        java.util.List<String> out = new java.util.ArrayList<>();
        try (Statement s = Db.get().createStatement(); ResultSet r = s.executeQuery("SELECT cu.id, cu.full_name, cu.phone, t.name, cu.point_balance, cu.spending_12m FROM customer cu JOIN membership_tier t ON t.id=cu.tier_id ORDER BY cu.id")) {
            while (r.next()) out.add(String.format("#%d %s (%s) | Hạng %s | %d điểm | Chi tiêu 12 tháng %,d đ", r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getInt(5), r.getLong(6)));
        }
        return out;
    }
}
