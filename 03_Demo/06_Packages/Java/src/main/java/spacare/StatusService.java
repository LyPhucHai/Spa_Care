package spacare;

import java.sql.*;

/** Check-in, bắt đầu, hoàn thành dịch vụ (UC-06): CONFIRMED -> CHECKED_IN -> IN_PROGRESS -> COMPLETED. */
public class StatusService {
    private Connection c() { return Db.get(); }

    public void advance(int id, String from, String to) throws SQLException {
        Auth.require("ADMIN", "MANAGER", "RECEPTIONIST", "TECHNICIAN");
        if (Auth.allowed("TECHNICIAN")) {
            if ("CHECKED_IN".equals(to)) throw new IllegalStateException("Lễ tân thực hiện check-in");
            try (PreparedStatement q = c().prepareStatement("SELECT COUNT(*) FROM appointment_service s JOIN staff st ON st.id=s.staff_id JOIN user_account u ON u.id=st.user_account_id WHERE s.appointment_id=? AND u.username=?")) {
                q.setInt(1, id); q.setString(2, Auth.user());
                try (ResultSet r = q.executeQuery()) { r.next(); if (r.getInt(1) == 0) throw new IllegalStateException("Lịch hẹn không được phân công cho bạn"); }
            }
        }
        if (!("CONFIRMED".equals(from) && "CHECKED_IN".equals(to)
                || "CHECKED_IN".equals(from) && "IN_PROGRESS".equals(to)
                || "IN_PROGRESS".equals(from) && "COMPLETED".equals(to)))
            throw new IllegalStateException("Chuyển trạng thái không hợp lệ");
        try (PreparedStatement p = c().prepareStatement("UPDATE appointment SET status=? WHERE id=? AND status=?")) {
            p.setString(1, to); p.setInt(2, id); p.setString(3, from);
            if (p.executeUpdate() == 0) throw new IllegalStateException("Lịch #" + id + " không ở trạng thái " + from);
        }
    }

    /** Đi hết 3 bước cho nhanh khi demo. */
    public void complete(int id) throws SQLException {
        advance(id, "CONFIRMED", "CHECKED_IN");
        advance(id, "CHECKED_IN", "IN_PROGRESS");
        advance(id, "IN_PROGRESS", "COMPLETED");
    }
}
