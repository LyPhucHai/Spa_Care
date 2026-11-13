package spacare;

import java.sql.*;
import java.util.*;

/** Danh sách và thêm khách hàng (UC-02). Khách mới đủ điều kiện ưu đãi trải nghiệm lần đầu. */
public class CustomerService {
    private Connection c() { return Db.get(); }

    public List<String> list() throws SQLException {
        Auth.require("ADMIN", "MANAGER", "RECEPTIONIST");
        List<String> out = new ArrayList<>();
        String sql = "SELECT cu.id, cu.full_name, cu.phone, t.name, cu.point_balance, cu.spending_12m, cu.is_new FROM customer cu JOIN membership_tier t ON t.id=cu.tier_id ORDER BY cu.id";
        try (Statement s = c().createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) out.add(String.format("#%d %s (%s) | Hạng %s | %d điểm | Chi tiêu 12 tháng %,d đ%s",
                    r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getInt(5), r.getLong(6), r.getBoolean(7) ? " | KHÁCH MỚI (đủ điều kiện gói trải nghiệm)" : ""));
        }
        return out;
    }

    /** Thêm khách mới, hạng SILVER, 0 điểm. Trả về mã khách. */
    public int add(String fullName, String phone) throws SQLException {
        Auth.require("ADMIN", "MANAGER", "RECEPTIONIST");
        if (fullName == null || phone == null) throw new IllegalStateException("Thiếu họ tên hoặc số điện thoại");
        String name = fullName.trim(), ph = phone.trim();
        if (name.isEmpty() || name.length() > 100) throw new IllegalStateException("Họ tên phải từ 1 đến 100 ký tự");
        if (!ph.matches("0\\d{9}")) throw new IllegalStateException("Số điện thoại phải gồm 10 số và bắt đầu bằng 0");
        try (PreparedStatement p = c().prepareStatement("SELECT 1 FROM customer WHERE phone=?")) {
            p.setString(1, ph);
            try (ResultSet r = p.executeQuery()) { if (r.next()) throw new IllegalStateException("Số điện thoại " + ph + " đã có trong hệ thống"); }
        }
        int id;
        try (Statement s = c().createStatement(); ResultSet r = s.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM customer")) { r.next(); id = r.getInt(1); }
        try (PreparedStatement p = c().prepareStatement("INSERT INTO customer(id,tier_id,full_name,phone,point_balance,spending_12m,is_new) VALUES (?,1,?,?,0,0,TRUE)")) {
            p.setInt(1, id); p.setString(2, name); p.setString(3, ph); p.executeUpdate();
        }
        return id;
    }
}
