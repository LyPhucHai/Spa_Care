package spacare;

import java.sql.*;

/** Check-in, bắt đầu, hoàn thành dịch vụ (UC-06): CONFIRMED -> CHECKED_IN -> IN_PROGRESS -> COMPLETED. */
public class StatusService {
    private Connection c() { return Db.get(); }

    public void advance(int id, String from, String to) throws SQLException {
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
