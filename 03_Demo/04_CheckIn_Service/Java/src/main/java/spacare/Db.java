package spacare;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;

/** Kết nối CSDL (mặc định H2 in-memory; đổi sang MySQL bằng -Ddb.url=... -Ddb.user=... -Ddb.pass=...). */
public final class Db {
    private static Connection conn;

    public static Connection get() { return conn; }

    public static void start() throws Exception {
        conn = DriverManager.getConnection(System.getProperty("db.url", "jdbc:h2:mem:spacare;DB_CLOSE_DELAY=-1"),
                System.getProperty("db.user", "sa"), System.getProperty("db.pass", ""));
        for (String f : new String[]{"/schema.sql", "/data.sql"}) {
            String sql = new String(Db.class.getResourceAsStream(f).readAllBytes(), StandardCharsets.UTF_8);
            for (String s : sql.split(";\\s*\\n")) if (!s.isBlank()) try (Statement st = conn.createStatement()) { st.execute(s); }
        }
        String[][] users = {{"admin", "ADMIN"}, {"letan01", "RECEPTIONIST"}, {"ktv_lan", "TECHNICIAN"}, {"ql_hoa", "MANAGER"}};
        try (PreparedStatement p = conn.prepareStatement("INSERT INTO user_account(username,password_hash,role) VALUES (?,?,?)")) {
            for (String[] u : users) { p.setString(1, u[0]); p.setString(2, hash("123456")); p.setString(3, u[1]); p.executeUpdate(); }
        }
        // Lịch làm 14 ngày tới: Lan, Hà làm cả ngày; Minh chỉ ca sáng
        try (PreparedStatement p = conn.prepareStatement("INSERT INTO work_schedule(staff_id,work_date,shift_id) VALUES (?,?,?)")) {
            for (int d = 0; d < 14; d++) for (int s = 1; s <= 3; s++) {
                p.setInt(1, s); p.setDate(2, java.sql.Date.valueOf(LocalDate.now().plusDays(d))); p.setInt(3, s == 3 ? 1 : 3); p.executeUpdate();
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
