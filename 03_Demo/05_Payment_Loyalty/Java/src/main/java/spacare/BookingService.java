package spacare;

import java.sql.*;
import java.time.*;
import java.util.*;

/** Đặt lịch hẹn: khung giờ trống, kiểm tra xung đột nhân viên/phòng, tạo lịch (UC-04, UC-17). */
public class BookingService {
    Connection c() { return Db.get(); }

    public int totalMinutes(List<Integer> serviceIds) throws SQLException {
        int sum = 0;
        for (int id : serviceIds) try (PreparedStatement p = c().prepareStatement("SELECT duration_min FROM service WHERE id=? AND is_active=TRUE")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new IllegalStateException("Không có dịch vụ #" + id); sum += r.getInt(1); }
        }
        return sum;
    }

    /** UC-17: kiểm tra nhân viên có lịch hẹn chưa hủy giao nhau [from,to) hay không. */
    boolean staffBusy(int staffId, LocalDateTime from, LocalDateTime to, int excludeAppt) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointment_service s JOIN appointment a ON a.id=s.appointment_id "
                + "WHERE a.status NOT IN ('CANCELLED','NO_SHOW') AND s.staff_id=? AND s.start_time<? AND s.end_time>? AND a.id<>?";
        try (PreparedStatement p = c().prepareStatement(sql)) {
            p.setInt(1, staffId); p.setTimestamp(2, Timestamp.valueOf(to)); p.setTimestamp(3, Timestamp.valueOf(from)); p.setInt(4, excludeAppt);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1) > 0; }
        }
    }

    /** Phòng đầu tiên còn trống trong khoảng [from,to); -1 nếu hết phòng. */
    int pickRoom(LocalDateTime from, LocalDateTime to, int excludeAppt) throws SQLException {
        String sql = "SELECT r.id FROM room r WHERE NOT EXISTS (SELECT 1 FROM appointment_service s JOIN appointment a ON a.id=s.appointment_id "
                + "WHERE s.room_id=r.id AND a.status NOT IN ('CANCELLED','NO_SHOW') AND s.start_time<? AND s.end_time>? AND a.id<>?) ORDER BY r.id LIMIT 1";
        try (PreparedStatement p = c().prepareStatement(sql)) {
            p.setTimestamp(1, Timestamp.valueOf(to)); p.setTimestamp(2, Timestamp.valueOf(from)); p.setInt(3, excludeAppt);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : -1; }
        }
    }

    public List<LocalDateTime> findSlots(int staffId, LocalDate date, int minutes, int excludeAppt) throws SQLException {
        List<LocalDateTime> out = new ArrayList<>();
        List<LocalTime[]> shifts = new ArrayList<>();
        try (PreparedStatement p = c().prepareStatement("SELECT sh.start_time, sh.end_time FROM work_schedule w JOIN shift sh ON sh.id=w.shift_id WHERE w.staff_id=? AND w.work_date=?")) {
            p.setInt(1, staffId); p.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet r = p.executeQuery()) { while (r.next()) shifts.add(new LocalTime[]{r.getTime(1).toLocalTime(), r.getTime(2).toLocalTime()}); }
        }
        for (LocalTime[] sh : shifts) {
            LocalDateTime end = date.atTime(sh[1]);
            for (LocalDateTime t = date.atTime(sh[0]); !t.plusMinutes(minutes).isAfter(end); t = t.plusMinutes(30))
                if (t.isAfter(LocalDateTime.now()) && !staffBusy(staffId, t, t.plusMinutes(minutes), excludeAppt) && pickRoom(t, t.plusMinutes(minutes), excludeAppt) > 0) out.add(t);
        }
        return out;
    }

    /** UC-04: đặt lịch. Trả về mã lịch hẹn. */
    public int create(int customerId, List<Integer> serviceIds, int staffId, LocalDateTime start) throws SQLException {
        Connection c = c(); c.setAutoCommit(false);
        try {
            int total = totalMinutes(serviceIds);
            for (int sv : serviceIds) try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM staff_service WHERE staff_id=? AND service_id=?")) {
                p.setInt(1, staffId); p.setInt(2, sv);
                try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new IllegalStateException("Nhân viên #" + staffId + " không có chuyên môn dịch vụ #" + sv); }
            }
            if (!findSlots(staffId, start.toLocalDate(), total, 0).contains(start))
                throw new IllegalStateException("Xung đột lịch: nhân viên/phòng không trống hoặc ngoài ca làm lúc " + start + ". Hãy chọn khung giờ khác (menu 2).");
            LocalDateTime end = start.plusMinutes(total);
            int room = pickRoom(start, end, 0), apptId;
            try (PreparedStatement p = c.prepareStatement("INSERT INTO appointment(customer_id,start_time,end_time,status) VALUES (?,?,?,'CONFIRMED')", Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, customerId); p.setTimestamp(2, Timestamp.valueOf(start)); p.setTimestamp(3, Timestamp.valueOf(end)); p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) { k.next(); apptId = k.getInt(1); }
            }
            LocalDateTime t = start;
            for (int sv : serviceIds) try (PreparedStatement q = c.prepareStatement("SELECT duration_min, price FROM service WHERE id=?")) {
                q.setInt(1, sv);
                try (ResultSet r = q.executeQuery()) {
                    r.next(); LocalDateTime e = t.plusMinutes(r.getInt(1));
                    try (PreparedStatement i = c.prepareStatement("INSERT INTO appointment_service(appointment_id,service_id,staff_id,room_id,start_time,end_time,price_at_booking) VALUES (?,?,?,?,?,?,?)")) {
                        i.setInt(1, apptId); i.setInt(2, sv); i.setInt(3, staffId); i.setInt(4, room); i.setTimestamp(5, Timestamp.valueOf(t)); i.setTimestamp(6, Timestamp.valueOf(e)); i.setLong(7, r.getLong(2)); i.executeUpdate();
                    }
                    t = e;
                }
            }
            c.commit();
            return apptId;
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
    }

    public List<String> list() throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = "SELECT a.id, a.start_time, a.end_time, a.status, cu.full_name, st.full_name, sv.name FROM appointment a JOIN customer cu ON cu.id=a.customer_id "
                + "JOIN appointment_service s ON s.appointment_id=a.id JOIN staff st ON st.id=s.staff_id JOIN service sv ON sv.id=s.service_id ORDER BY a.start_time, s.start_time";
        try (Statement s = c().createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) out.add(String.format("#%d  %s -> %s  [%s]  Khách: %s | NV: %s | %s", r.getInt(1), r.getTimestamp(2).toString().substring(0, 16), r.getTimestamp(3).toString().substring(11, 16), r.getString(4), r.getString(5), r.getString(6), r.getString(7)));
        }
        return out;
    }
}
