package spacare;

import java.sql.*;
import java.time.*;
import java.util.*;

/** Dời và hủy lịch hẹn (UC-05). Dùng lại BookingService để kiểm tra xung đột. */
public class RescheduleService extends BookingService {
    static final int MIN_NOTICE_HOURS = 2;

    private record Appt(String status, LocalDateTime start, LocalDateTime end, int staffId) {}

    private Appt load(int id) throws SQLException {
        try (PreparedStatement p = c().prepareStatement("SELECT a.status, a.start_time, a.end_time, (SELECT MIN(staff_id) FROM appointment_service WHERE appointment_id=a.id) FROM appointment a WHERE a.id=?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("Không tìm thấy lịch hẹn #" + id);
                return new Appt(r.getString(1), r.getTimestamp(2).toLocalDateTime(), r.getTimestamp(3).toLocalDateTime(), r.getInt(4));
            }
        }
    }

    private void checkPolicy(Appt a, boolean override) {
        if (!a.status().equals("CONFIRMED") && !a.status().equals("PENDING")) throw new IllegalStateException("Chỉ dời/hủy lịch ở trạng thái PENDING hoặc CONFIRMED (hiện tại: " + a.status() + ")");
        if (!override && LocalDateTime.now().plusHours(MIN_NOTICE_HOURS).isAfter(a.start()))
            throw new IllegalStateException("Quá hạn: phải dời/hủy trước giờ hẹn tối thiểu " + MIN_NOTICE_HOURS + " giờ. Lễ tân/Quản lý có thể ghi đè.");
    }

    /** UC-05: dời lịch. */
    public void reschedule(int id, LocalDateTime newStart, boolean override) throws SQLException {
        Auth.require("ADMIN", "MANAGER", "RECEPTIONIST");
        Appt a = load(id); checkPolicy(a, override);
        int minutes = (int) Duration.between(a.start(), a.end()).toMinutes();
        if (!findSlots(a.staffId(), newStart.toLocalDate(), minutes, id).contains(newStart)) throw new IllegalStateException("Khung giờ mới xung đột hoặc ngoài ca làm");
        Connection c = c(); c.setAutoCommit(false);
        try {
            int room = pickRoom(newStart, newStart.plusMinutes(minutes), id);
            Duration shift = Duration.between(a.start(), newStart);
            List<int[]> ids = new ArrayList<>(); List<LocalDateTime[]> times = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement("SELECT id,start_time,end_time FROM appointment_service WHERE appointment_id=?")) {
                p.setInt(1, id);
                try (ResultSet r = p.executeQuery()) { while (r.next()) { ids.add(new int[]{r.getInt(1)}); times.add(new LocalDateTime[]{r.getTimestamp(2).toLocalDateTime().plus(shift), r.getTimestamp(3).toLocalDateTime().plus(shift)}); } }
            }
            for (int i = 0; i < ids.size(); i++) try (PreparedStatement u = c.prepareStatement("UPDATE appointment_service SET start_time=?, end_time=?, room_id=? WHERE id=?")) {
                u.setTimestamp(1, Timestamp.valueOf(times.get(i)[0])); u.setTimestamp(2, Timestamp.valueOf(times.get(i)[1])); u.setInt(3, room); u.setInt(4, ids.get(i)[0]); u.executeUpdate();
            }
            try (PreparedStatement u = c.prepareStatement("UPDATE appointment SET start_time=?, end_time=? WHERE id=?")) {
                u.setTimestamp(1, Timestamp.valueOf(newStart)); u.setTimestamp(2, Timestamp.valueOf(newStart.plusMinutes(minutes))); u.setInt(3, id); u.executeUpdate();
            }
            c.commit();
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
    }

    /** UC-05: hủy lịch. */
    public void cancel(int id, String reason, boolean override) throws SQLException {
        Auth.require("ADMIN", "MANAGER", "RECEPTIONIST");
        if (reason == null || reason.isBlank() || reason.trim().length() > 200) throw new IllegalStateException("Lý do hủy phải từ 1 đến 200 ký tự");
        checkPolicy(load(id), override);
        try (PreparedStatement p = c().prepareStatement("UPDATE appointment SET status='CANCELLED', cancel_reason=? WHERE id=?")) { p.setString(1, reason); p.setInt(2, id); p.executeUpdate(); }
    }

}
