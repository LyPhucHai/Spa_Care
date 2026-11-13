package spacare;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Kết nối CSDL.
 * Mặc định: H2 trong RAM (mất khi thoát, mỗi lần chạy sạch dữ liệu).
 * Lưu ra file: chạy với -Ddb.url=jdbc:h2:file:./data/spacare (schema và dữ liệu mẫu chỉ nạp khi CSDL còn trống).
 */
public final class Db {
    private static final String DEFAULT_URL = "jdbc:h2:mem:spacare;DB_CLOSE_DELAY=-1";
    private static Connection conn;

    private Db() {}

    public static Connection get() { return conn; }

    public static boolean persistent() { return !System.getProperty("db.url", DEFAULT_URL).startsWith("jdbc:h2:mem:"); }

    public static void start() throws Exception {
        Auth.logout();
        conn = DriverManager.getConnection(System.getProperty("db.url", DEFAULT_URL),
                System.getProperty("db.user", "sa"), System.getProperty("db.pass", ""));
        migrateAccounts();
        if (!hasTable("CUSTOMER")) seed();
        linkStaffAccounts();
        ensureSchedule();
    }

    private static void linkStaffAccounts() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("ALTER TABLE staff ADD COLUMN IF NOT EXISTS user_account_id INT REFERENCES user_account(id)");
            s.execute("UPDATE staff SET user_account_id=(SELECT id FROM user_account WHERE username='ktv_lan') WHERE id=1 AND user_account_id IS NULL");
        }
    }

    private static void migrateAccounts() throws SQLException {
        if (!hasTable("USER_ACCOUNT")) return;
        try (Statement s = conn.createStatement()) {
            s.execute("ALTER TABLE user_account ALTER COLUMN password_hash VARCHAR(255)");
            s.execute("ALTER TABLE user_account ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP");
            s.execute("ALTER TABLE user_account ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE NOT NULL");
        }
    }

    /** Xóa toàn bộ và nạp lại dữ liệu mẫu (chỉ dùng cho H2). */
    public static void reset() throws Exception {
        Auth.require("ADMIN");
        try (Statement s = conn.createStatement()) { s.execute("DROP ALL OBJECTS"); }
        seed();
        linkStaffAccounts();
        ensureSchedule();
    }

    private static boolean hasTable(String name) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME)=?")) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1) > 0; }
        }
    }

    private static void runScript(String file) throws Exception {
        String sql = new String(Db.class.getResourceAsStream(file).readAllBytes(), StandardCharsets.UTF_8);
        for (String s : sql.split(";\\s*\\n")) if (!s.isBlank()) try (Statement st = conn.createStatement()) { st.execute(s); }
    }

    private static void seed() throws Exception {
        runScript("/schema.sql");
        runScript("/data.sql");
        String[][] users = {{"admin", "ADMIN"}, {"letan01", "RECEPTIONIST"}, {"ktv_lan", "TECHNICIAN"}, {"ql_hoa", "MANAGER"}};
        try (PreparedStatement p = conn.prepareStatement("INSERT INTO user_account(username,password_hash,role) VALUES (?,?,?)")) {
            for (String[] u : users) { p.setString(1, u[0]); p.setString(2, Auth.passwordHash("123456")); p.setString(3, u[1]); p.executeUpdate(); }
        }
        // Thu H đã mua sẵn Thẻ Massage 10 buổi (gói #2), còn 6 buổi, hạn 60 ngày nữa
        try (PreparedStatement p = conn.prepareStatement("INSERT INTO customer_package(customer_id,package_id,remaining_sessions,expires_at) VALUES (1,2,6,?)")) {
            p.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().plusDays(60)));
            p.executeUpdate();
        }
    }

    /** Lịch làm 14 ngày tới: Lan, Hà làm cả ngày; Minh chỉ ca sáng. Chỉ bổ sung ngày còn thiếu. */
    private static void ensureSchedule() throws SQLException {
        try (PreparedStatement chk = conn.prepareStatement("SELECT COUNT(*) FROM work_schedule WHERE staff_id=? AND work_date=?");
             PreparedStatement ins = conn.prepareStatement("INSERT INTO work_schedule(staff_id,work_date,shift_id) VALUES (?,?,?)")) {
            for (int d = 0; d < 14; d++) for (int s = 1; s <= 3; s++) {
                java.sql.Date day = java.sql.Date.valueOf(LocalDate.now().plusDays(d));
                int shift = s == 3 ? 1 : 3;
                chk.setInt(1, s); chk.setDate(2, day);
                try (ResultSet r = chk.executeQuery()) { r.next(); if (r.getInt(1) > 0) continue; }
                ins.setInt(1, s); ins.setDate(2, day); ins.setInt(3, shift); ins.executeUpdate();
            }
        }
    }

    public static String hash(String pw) throws Exception {
        byte[] b = MessageDigest.getInstance("SHA-256").digest(("spacare:" + pw).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
