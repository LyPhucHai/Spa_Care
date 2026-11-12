package spacare;

import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Đặt lịch hẹn: khung giờ trống, chặn xung đột nhân viên/phòng (UC-04, UC-17).
 * Mở rộng: đặt theo combo, dùng 1 buổi của gói liệu trình, đặt cho nhiều khách (đôi/nhóm).
 * Giả định của bản demo: lịch nhiều khách vẫn chỉ giữ 1 nhân viên và 1 phòng (số khách chỉ ảnh hưởng giá).
 */
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

    /** Danh sách mã dịch vụ của một combo đang bán. */
    List<Integer> comboServices(int comboId) throws SQLException {
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement p = c().prepareStatement("SELECT ci.service_id FROM combo_item ci JOIN combo cb ON cb.id=ci.combo_id WHERE ci.combo_id=? AND cb.is_active=TRUE ORDER BY ci.service_id")) {
            p.setInt(1, comboId);
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.add(r.getInt(1)); }
        }
        if (out.isEmpty()) throw new IllegalStateException("Combo #" + comboId + " không tồn tại hoặc đã ngừng bán");
        return out;
    }

    /** Kiểm tra khách được dùng 1 buổi của gói cho lịch này; trả về mã dịch vụ mà gói chi trả. */
    private int checkPackage(int customerId, int cpId, List<Integer> serviceIds, LocalDateTime start) throws SQLException {
        int pkgService;
        try (PreparedStatement p = c().prepareStatement("SELECT cp.customer_id, p.service_id, cp.expires_at FROM customer_package cp JOIN service_package p ON p.id=cp.package_id WHERE cp.id=?")) {
            p.setInt(1, cpId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("Không tìm thấy gói của khách #" + cpId);
                if (r.getInt(1) != customerId) throw new IllegalStateException("Gói #" + cpId + " không thuộc khách hàng #" + customerId);
                pkgService = r.getInt(2);
                if (r.getTimestamp(3).toLocalDateTime().isBefore(start)) throw new IllegalStateException("Gói #" + cpId + " đã hết hạn trước ngày hẹn");
            }
        }
        if (!serviceIds.contains(pkgService)) throw new IllegalStateException("Gói #" + cpId + " chỉ dùng cho dịch vụ #" + pkgService + ", lịch hẹn không có dịch vụ đó");
        if (PackageService.available(c(), cpId) <= 0) throw new IllegalStateException("Gói #" + cpId + " đã hết buổi (hoặc các buổi còn lại đã được giữ cho lịch khác)");
        return pkgService;
    }

    /** UC-04: đặt lịch dịch vụ lẻ. Trả về mã lịch hẹn. */
    public int create(int customerId, List<Integer> serviceIds, int staffId, LocalDateTime start) throws SQLException {
        return create(customerId, serviceIds, staffId, start, 1, null, null);
    }

    /**
     * UC-04 mở rộng.
     * @param guests số khách (1 = một người, 2 = đôi, từ 3 = nhóm)
     * @param comboId mã combo (khi có thì danh sách dịch vụ lấy từ combo), null nếu không đặt combo
     * @param cpId mã gói của khách (customer_package) để dùng 1 buổi, null nếu không dùng
     */
    public int create(int customerId, List<Integer> serviceIds, int staffId, LocalDateTime start,
                      int guests, Integer comboId, Integer cpId) throws SQLException {
        if (guests < 1 || guests > 10) throw new IllegalStateException("Số khách phải từ 1 đến 10");
        if (comboId != null && cpId != null) throw new IllegalStateException("Không dùng chung combo và buổi liệu trình trong một lịch hẹn");
        if (cpId != null && guests != 1) throw new IllegalStateException("Buổi liệu trình chỉ dùng cho 1 khách");
        Connection c = c(); c.setAutoCommit(false);
        try {
            try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM customer WHERE id=?")) {
                p.setInt(1, customerId);
                try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new IllegalStateException("Không tìm thấy khách hàng #" + customerId); }
            }
            List<Integer> svs = comboId != null ? comboServices(comboId) : serviceIds;
            if (svs.isEmpty()) throw new IllegalStateException("Chưa chọn dịch vụ nào");
            int pkgService = cpId != null ? checkPackage(customerId, cpId, svs, start) : -1;
            int total = totalMinutes(svs);
            for (int sv : svs) try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM staff_service WHERE staff_id=? AND service_id=?")) {
                p.setInt(1, staffId); p.setInt(2, sv);
                try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new IllegalStateException("Nhân viên #" + staffId + " không có chuyên môn dịch vụ #" + sv); }
            }
            if (!findSlots(staffId, start.toLocalDate(), total, 0).contains(start))
                throw new IllegalStateException("Xung đột lịch: nhân viên/phòng không trống hoặc ngoài ca làm lúc " + start + ". Hãy chọn khung giờ khác (menu 5).");
            LocalDateTime end = start.plusMinutes(total);
            int room = pickRoom(start, end, 0), apptId;
            try (PreparedStatement p = c.prepareStatement("INSERT INTO appointment(customer_id,start_time,end_time,status,guest_count,combo_id) VALUES (?,?,?,'CONFIRMED',?,?)", Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, customerId); p.setTimestamp(2, Timestamp.valueOf(start)); p.setTimestamp(3, Timestamp.valueOf(end)); p.setInt(4, guests);
                if (comboId == null) p.setNull(5, Types.INTEGER); else p.setInt(5, comboId);
                p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) { k.next(); apptId = k.getInt(1); }
            }
            LocalDateTime t = start;
            boolean pkgUsed = false;
            for (int sv : svs) {
                long price; int minutes;
                try (PreparedStatement q = c.prepareStatement("SELECT duration_min, price FROM service WHERE id=?")) {
                    q.setInt(1, sv);
                    try (ResultSet r = q.executeQuery()) { r.next(); minutes = r.getInt(1); price = r.getLong(2); }
                }
                LocalDateTime e = t.plusMinutes(minutes);
                boolean byPkg = cpId != null && !pkgUsed && sv == pkgService;
                try (PreparedStatement i = c.prepareStatement("INSERT INTO appointment_service(appointment_id,service_id,staff_id,room_id,start_time,end_time,price_at_booking,customer_package_id) VALUES (?,?,?,?,?,?,?,?)")) {
                    i.setInt(1, apptId); i.setInt(2, sv); i.setInt(3, staffId); i.setInt(4, room);
                    i.setTimestamp(5, Timestamp.valueOf(t)); i.setTimestamp(6, Timestamp.valueOf(e)); i.setLong(7, price);
                    if (byPkg) i.setInt(8, cpId); else i.setNull(8, Types.INTEGER);
                    i.executeUpdate();
                }
                if (byPkg) pkgUsed = true;
                t = e;
            }
            c.commit();
            return apptId;
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
    }

    public List<String> list() throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = "SELECT a.id, a.start_time, a.end_time, a.status, cu.full_name, st.full_name, sv.name, a.guest_count, cb.name, s.customer_package_id "
                + "FROM appointment a JOIN customer cu ON cu.id=a.customer_id "
                + "JOIN appointment_service s ON s.appointment_id=a.id JOIN staff st ON st.id=s.staff_id JOIN service sv ON sv.id=s.service_id "
                + "LEFT JOIN combo cb ON cb.id=a.combo_id ORDER BY a.start_time, s.start_time";
        try (Statement s = c().createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                StringBuilder extra = new StringBuilder();
                if (r.getInt(8) > 1) extra.append(" | ").append(r.getInt(8)).append(" khách");
                if (r.getString(9) != null) extra.append(" | ").append(r.getString(9));
                int cp = r.getInt(10);
                if (!r.wasNull()) extra.append(" | dùng buổi gói #").append(cp);
                out.add(String.format("#%d  %s -> %s  [%s]  Khách: %s | NV: %s | %s%s", r.getInt(1), r.getTimestamp(2).toString().substring(0, 16),
                        r.getTimestamp(3).toString().substring(11, 16), r.getString(4), r.getString(5), r.getString(6), r.getString(7), extra));
            }
        }
        return out;
    }
}
