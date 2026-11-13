package spacare;

import java.sql.*;
import java.time.*;
import java.util.*;

/** Quản lý dữ liệu danh mục và báo cáo của bản console. */
public final class ManagementService {
    private Connection db() { return Db.get(); }
    private void manager() { Auth.require("ADMIN", "MANAGER"); }
    private void reception() { Auth.require("ADMIN", "MANAGER", "RECEPTIONIST"); }

    public List<String> services() throws SQLException {
        Auth.requireLogin();
        List<String> out = new ArrayList<>();
        try (Statement p = db().createStatement(); ResultSet r = p.executeQuery("SELECT id,name,price,duration_min,is_active FROM service ORDER BY id")) {
            while (r.next()) out.add(String.format("#%d | %s | %,d đ | %d phút | %s", r.getInt(1), r.getString(2), r.getLong(3), r.getInt(4), r.getBoolean(5) ? "Đang bán" : "Ngừng bán"));
        }
        return out;
    }
    public void updateService(int id, String name, long price, int minutes, boolean active) throws SQLException {
        manager();
        if (name == null || name.isBlank() || name.trim().length() > 100 || price < 0 || price > 1_000_000_000L || minutes < 1 || minutes > 720)
            throw new IllegalStateException("Tên 1–100 ký tự, giá 0–1 tỷ, thời lượng 1–720 phút");
        try (PreparedStatement p = db().prepareStatement("UPDATE service SET name=?,price=?,duration_min=?,is_active=? WHERE id=?")) {
            p.setString(1, name.trim()); p.setLong(2, price); p.setInt(3, minutes); p.setBoolean(4, active); p.setInt(5, id);
            if (p.executeUpdate() == 0) throw new IllegalStateException("Không tìm thấy dịch vụ");
        }
    }
    public List<String> customers(String query) throws SQLException {
        reception();
        List<String> out = new ArrayList<>();
        try (PreparedStatement p = db().prepareStatement("SELECT id,full_name,phone,point_balance FROM customer WHERE LOCATE(LOWER(?),LOWER(full_name))>0 OR LOCATE(?,phone)>0 ORDER BY id")) {
            p.setString(1, query.trim()); p.setString(2, query.trim());
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.add(String.format("#%d | %s | %s | %d điểm", r.getInt(1), r.getString(2), r.getString(3), r.getInt(4))); }
        }
        return out;
    }
    public void updateCustomer(int id, String name, String phone) throws SQLException {
        reception();
        if (name == null || name.isBlank() || name.trim().length() > 100 || phone == null || !phone.trim().matches("0\\d{9}"))
            throw new IllegalStateException("Tên 1–100 ký tự, số điện thoại 10 chữ số bắt đầu bằng 0");
        try (PreparedStatement p = db().prepareStatement("SELECT 1 FROM customer WHERE phone=? AND id<>?")) {
            p.setString(1, phone.trim()); p.setInt(2, id);
            try (ResultSet r = p.executeQuery()) { if (r.next()) throw new IllegalStateException("Số điện thoại đã được sử dụng"); }
        }
        try (PreparedStatement p = db().prepareStatement("UPDATE customer SET full_name=?,phone=? WHERE id=?")) {
            p.setString(1, name.trim()); p.setString(2, phone.trim()); p.setInt(3, id);
            if (p.executeUpdate() == 0) throw new IllegalStateException("Không tìm thấy khách hàng");
        }
    }
    public List<String> history(int customerId) throws SQLException {
        reception();
        List<String> out = new ArrayList<>();
        try (PreparedStatement p = db().prepareStatement("SELECT a.id,a.start_time,a.status,i.total FROM appointment a LEFT JOIN invoice i ON i.appointment_id=a.id WHERE a.customer_id=? ORDER BY a.start_time DESC")) {
            p.setInt(1, customerId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(String.format("Lịch #%d | %s | %s | %s", r.getInt(1), r.getTimestamp(2), r.getString(3), r.getObject(4) == null ? "Chưa thanh toán" : String.format("%,d đ", r.getLong(4))));
            }
        }
        try (PreparedStatement p = db().prepareStatement("SELECT type,points,balance_after,created_at FROM loyalty_transaction WHERE customer_id=? ORDER BY id DESC")) {
            p.setInt(1, customerId);
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.add(String.format("Điểm %s | %+d | Số dư %d | %s", r.getString(1), r.getInt(2), r.getInt(3), r.getTimestamp(4))); }
        }
        return out;
    }
    public List<String> staff() throws SQLException {
        manager();
        List<String> out = new ArrayList<>();
        try (Statement p = db().createStatement(); ResultSet r = p.executeQuery("SELECT st.id,st.full_name,sv.name FROM staff st LEFT JOIN staff_service ss ON ss.staff_id=st.id LEFT JOIN service sv ON sv.id=ss.service_id ORDER BY st.id,sv.id")) {
            while (r.next()) out.add(String.format("#%d | %s | %s", r.getInt(1), r.getString(2), r.getString(3)));
        }
        try (Statement p = db().createStatement(); ResultSet r = p.executeQuery("SELECT id,name,start_time,end_time FROM shift ORDER BY id")) {
            while (r.next()) out.add(String.format("Ca #%d | %s | %s–%s", r.getInt(1), r.getString(2), r.getTime(3), r.getTime(4)));
        }
        return out;
    }
    public void assignShift(int staffId, LocalDate date, int shiftId) throws SQLException {
        manager();
        if (date == null || date.isBefore(LocalDate.now())) throw new IllegalStateException("Không phân ca cho ngày quá khứ");
        String sql = "SELECT COUNT(*) FROM work_schedule w JOIN shift old ON old.id=w.shift_id JOIN shift wanted ON wanted.id=? WHERE w.staff_id=? AND w.work_date=? AND old.start_time<wanted.end_time AND old.end_time>wanted.start_time";
        try (PreparedStatement p = db().prepareStatement(sql)) {
            p.setInt(1, shiftId); p.setInt(2, staffId); p.setDate(3, java.sql.Date.valueOf(date));
            try (ResultSet r = p.executeQuery()) { r.next(); if (r.getInt(1) > 0) throw new IllegalStateException("Ca mới trùng ca đã phân; không thay ca có lịch hẹn"); }
        }
        try (PreparedStatement p = db().prepareStatement("INSERT INTO work_schedule(staff_id,work_date,shift_id) VALUES (?,?,?)")) {
            p.setInt(1, staffId); p.setDate(2, java.sql.Date.valueOf(date)); p.setInt(3, shiftId); p.executeUpdate();
        }
    }
    public List<String> revenue(LocalDate from, LocalDate to) throws SQLException {
        manager();
        if (from == null || to == null || to.isBefore(from)) throw new IllegalStateException("Khoảng ngày báo cáo không hợp lệ");
        List<String> out = new ArrayList<>();
        try (PreparedStatement p = db().prepareStatement("SELECT CAST(created_at AS DATE),COUNT(*),SUM(total) FROM invoice WHERE status='PAID' AND created_at>=? AND created_at<? GROUP BY CAST(created_at AS DATE) ORDER BY 1")) {
            p.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay())); p.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            long sum = 0, count = 0;
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) { count += r.getLong(2); sum += r.getLong(3); out.add(String.format("%s | %d hóa đơn | %,d đ", r.getDate(1), r.getLong(2), r.getLong(3))); }
            }
            out.add(String.format("TỔNG | %d hóa đơn | %,d đ (gồm tiền bán gói, không cộng lại buổi gói đã trả trước)", count, sum));
        }
        return out;
    }
    public List<String> ownSchedule() throws SQLException {
        Auth.require("TECHNICIAN");
        List<String> out = new ArrayList<>();
        try (PreparedStatement p = db().prepareStatement("SELECT DISTINCT a.id,a.start_time,a.status,cu.full_name FROM appointment a JOIN appointment_service s ON s.appointment_id=a.id JOIN staff st ON st.id=s.staff_id JOIN user_account u ON u.id=st.user_account_id JOIN customer cu ON cu.id=a.customer_id WHERE u.username=? ORDER BY a.start_time")) {
            p.setString(1, Auth.user());
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.add(String.format("#%d | %s | %s | %s", r.getInt(1), r.getTimestamp(2), r.getString(3), r.getString(4))); }
        }
        return out;
    }
}
