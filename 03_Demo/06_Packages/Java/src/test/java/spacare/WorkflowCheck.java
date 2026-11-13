package spacare;

import java.sql.*;
import java.time.*;
import java.util.*;

/** Kiểm thử chấp nhận trên H2 thật, không cần JUnit hoặc kết nối mạng. */
public final class WorkflowCheck {
    interface Action { void run() throws Exception; }
    static int checks;
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++; System.out.println("OK " + message);
    }
    static void reject(String name, Action action) throws Exception {
        try { action.run(); } catch (IllegalStateException expected) { check(true, name); return; }
        throw new AssertionError("Không từ chối: " + name);
    }
    static long value(String sql) throws SQLException {
        try (Statement p = Db.get().createStatement(); ResultSet r = p.executeQuery(sql)) { r.next(); return r.getLong(1); }
    }
    static void execute(String sql) throws SQLException { try (Statement p = Db.get().createStatement()) { p.execute(sql); } }
    public static void main(String[] args) throws Exception {
        // Không cho bài test đụng vào dữ liệu saved của người dùng.
        System.setProperty("db.url", "jdbc:h2:mem:workflow_check;DB_CLOSE_DELAY=-1");
        Db.start();
        try {
            BookingService booking = new BookingService();
            RescheduleService reschedule = new RescheduleService();
            StatusService status = new StatusService();
            BillingService billing = new BillingService();
            ManagementService management = new ManagementService();
            CustomerService customers = new CustomerService();
            LocalDate day = LocalDate.now().plusDays(1);
            reject("Chưa đăng nhập không được đặt lịch", () -> booking.create(2, List.of(1), 1, day.atTime(10, 0)));
            for (int i=0; i<5; i++) reject("Sai mật khẩu lần " + (i+1), () -> Auth.login("admin", "wrong"));
            reject("Đúng mật khẩu vẫn bị khóa", () -> Auth.login("admin", "123456"));
            check(value("SELECT failed_attempts FROM user_account WHERE username='admin'") == 5, "Bộ đếm sai lưu trong DB");
            execute("UPDATE user_account SET locked_until=DATEADD('MINUTE',-1,CURRENT_TIMESTAMP) WHERE username='admin'");
            check(Auth.login("admin", "123456").equals("ADMIN"), "Hết 15 phút được đăng nhập lại");
            check(value("SELECT failed_attempts FROM user_account WHERE username='admin'") == 0, "Đăng nhập thành công xóa bộ đếm");
            check(value("SELECT COUNT(*) FROM user_account WHERE password_hash LIKE 'pbkdf2$%'") == 4, "Mật khẩu mẫu dùng PBKDF2");
            reject("Không tạo lịch rỗng", () -> booking.create(2, List.of(), 1, day.atTime(10,0)));
            reject("Không chọn dịch vụ trùng", () -> booking.create(2, List.of(1,1), 1, day.atTime(10,0)));
            reject("Không trả khung giờ thời lượng 0", () -> booking.findSlots(1, day, 0, 0));
            reject("Không đặt sai chuyên môn", () -> booking.create(2, List.of(1), 3, day.atTime(10,0)));
            reject("Không đặt ngoài ca", () -> booking.create(2, List.of(4), 3, day.atTime(15,0)));
            int first = booking.create(2, List.of(1), 1, day.atTime(10,0));
            reject("Không đặt trùng lịch", () -> booking.create(1, List.of(2), 1, day.atTime(10,0)));
            int adjacent = booking.create(1, List.of(2), 1, day.atTime(11,30));
            check(adjacent > first, "Đặt liền kề được chấp nhận");
            reject("Không dời vào giờ đã bận", () -> reschedule.reschedule(first, day.atTime(11,30), false));
            reschedule.reschedule(first, day.atTime(14,0), false);
            check(booking.findSlots(1, day, 90, 0).contains(day.atTime(10,0)), "Dời lịch giải phóng giờ cũ");
            reject("Hủy phải có lý do", () -> reschedule.cancel(adjacent, "", false));
            reschedule.cancel(adjacent, "Khách bận", false);
            check(booking.findSlots(1, day, 60, 0).contains(day.atTime(11,30)), "Hủy lịch giải phóng khung giờ");
            reject("Không hủy hai lần", () -> reschedule.cancel(adjacent, "Lần hai", false));
            reject("Không nhảy từ CONFIRMED sang COMPLETED", () -> status.advance(first, "CONFIRMED", "COMPLETED"));
            reject("Không thanh toán trước hoàn thành", () -> billing.pay(first, 0, "CASH"));
            status.advance(first, "CONFIRMED", "CHECKED_IN");
            Auth.login("ktv_lan", "123456");
            check(management.ownSchedule().size() == 2, "Kỹ thuật viên xem lịch phân công, gồm lịch đã hủy");
            reject("Kỹ thuật viên không thanh toán", () -> billing.pay(first, 0, "CASH"));
            reject("Kỹ thuật viên không xem doanh thu", () -> management.revenue(day, day));
            reject("Kỹ thuật viên không xem toàn bộ khách", () -> customers.list());
            reject("Kỹ thuật viên không check-in", () -> status.advance(first, "CONFIRMED", "CHECKED_IN"));
            status.advance(first, "CHECKED_IN", "IN_PROGRESS");
            status.advance(first, "IN_PROGRESS", "COMPLETED");
            check(value("SELECT COUNT(*) FROM appointment WHERE id=" + first + " AND status='COMPLETED'") == 1, "Kỹ thuật viên hoàn thành lịch được giao");
            Auth.login("letan01", "123456");
            reject("Lễ tân không sửa giá", () -> management.updateService(1,"X",1,30,true));
            reject("Phương thức thanh toán lạ", () -> billing.pay(first, 0, "INVALID"));
            reject("Đổi điểm quá số dư", () -> billing.pay(first, 10000, "CASH"));
            check(value("SELECT COUNT(*) FROM invoice") == 0, "Thanh toán lỗi không lưu hóa đơn");
            execute("ALTER TABLE payment ADD CONSTRAINT test_payment_failure CHECK (amount<1)");
            try { billing.pay(first,0,"CASH"); throw new AssertionError("Phải rollback khi ghi payment lỗi"); }
            catch (SQLException expected) { check(value("SELECT COUNT(*) FROM invoice")==0, "Rollback hóa đơn khi ghi thanh toán thất bại"); }
            execute("ALTER TABLE payment DROP CONSTRAINT test_payment_failure");
            billing.pay(first, 0, "CASH");
            check(value("SELECT point_balance FROM customer WHERE id=2") == 130, "Thanh toán 500k tích 50 điểm");
            check(value("SELECT tier_id FROM customer WHERE id=2") == 2, "Khách đạt 10 triệu nâng Gold");
            reject("Không thanh toán hai lần", () -> billing.pay(first, 0, "CASH"));
            check(value("SELECT COUNT(*) FROM invoice") == 1, "Chỉ một hóa đơn được lưu");
            int customer = customers.add("Khách kiểm thử", "0987654321");
            reject("Không trùng điện thoại", () -> customers.add("Trùng", "0987654321"));
            management.updateCustomer(customer, "Khách đổi tên", "0987654322");
            check(management.customers("0987654322").size() == 1, "Tìm hồ sơ đã cập nhật");
            check(management.customers("' OR 1=1 --").isEmpty(), "Tìm kiếm không thực thi SQL từ dữ liệu nhập");
            check(!management.history(2).isEmpty(), "Lịch sử dịch vụ và điểm được lưu");
            Auth.login("admin", "123456");
            check(management.revenue(LocalDate.now(),LocalDate.now()).stream().anyMatch(s -> s.contains("500,000")), "Báo cáo theo ngày xuất hóa đơn");
            reject("Không chấp nhận khoảng ngày ngược", () -> management.revenue(day,day.minusDays(1)));
            int other = booking.create(1, List.of(3), 2, day.atTime(9,0));
            status.advance(other, "CONFIRMED", "CHECKED_IN");
            Auth.login("ktv_lan", "123456");
            reject("Không cập nhật lịch của kỹ thuật viên khác", () -> status.advance(other, "CHECKED_IN", "IN_PROGRESS"));
            Auth.login("admin", "123456");
            int before = (int)value("SELECT remaining_sessions FROM customer_package WHERE id=1");
            int pkg = booking.create(1, List.of(1), 1, day.atTime(8,0), 1, null, 1);
            check(PackageService.available(Db.get(),1) == before-1, "Đặt lịch giữ buổi gói");
            reschedule.cancel(pkg,"Không đi",false);
            check(PackageService.available(Db.get(),1) == before, "Hủy nhả buổi gói");
            int pkg2 = booking.create(1, List.of(1), 1, day.atTime(8,0), 1, null, 1);
            status.complete(pkg2); billing.pay(pkg2,0,"CASH");
            check(value("SELECT remaining_sessions FROM customer_package WHERE id=1") == before-1, "Thanh toán trừ đúng một buổi gói");
            check(value("SELECT total FROM invoice WHERE appointment_id="+pkg2) == 0, "Buổi trả trước không thu tiền lại");
            management.updateService(1,"Massage mới",600000,90,false);
            check(value("SELECT price_at_booking FROM appointment_service WHERE appointment_id="+first) == 500000, "Sửa giá không đổi lịch sử");
            reject("Không đặt dịch vụ ngừng bán", () -> booking.create(1,List.of(1),1,day.atTime(16,0)));
            reject("Không phân ca trùng", () -> management.assignShift(1,day,1));
            management.assignShift(3,day,2);
            check(value("SELECT COUNT(*) FROM work_schedule WHERE staff_id=3 AND work_date=DATE '"+day+"'") == 2, "Phân được ca chiều liền ca sáng");
            Db.reset(); Auth.logout(); Auth.login("ktv_lan", "123456");
            check(management.ownSchedule().isEmpty(), "Reset tạo lại dữ liệu và liên kết tài khoản kỹ thuật viên");
            Auth.logout(); reject("Đăng xuất chặn nghiệp vụ", () -> customers.list());
            // Mô phỏng file H2 cũ: thiếu cột khóa, mật khẩu SHA-256.
            execute("UPDATE user_account SET password_hash='" + Db.hash("123456") + "'");
            execute("ALTER TABLE user_account DROP COLUMN locked_until");
            execute("ALTER TABLE user_account DROP COLUMN is_active");
            execute("ALTER TABLE user_account ALTER COLUMN password_hash VARCHAR(64)");
            Db.get().close(); Db.start();
            check(Auth.login("admin","123456").equals("ADMIN"), "Nâng cấp schema cũ không mất tài khoản");
            check(value("SELECT COUNT(*) FROM user_account WHERE username='admin' AND password_hash LIKE 'pbkdf2$%'")==1, "Tự nâng cấp hash cũ sau đăng nhập thành công");
            System.out.println("PASS " + checks + " kiểm tra nghiệp vụ.");
        } finally { Auth.logout(); Db.get().close(); }
    }
}
