package spacare;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Phiên đăng nhập của ứng dụng console một người dùng. Khóa tài khoản lưu trong H2. */
public final class Auth {
    private static String currentUser, currentRole;
    private static final int ITERATIONS = 210_000;
    private Auth() {}

    public static String passwordHash(String password) throws Exception {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
    }
    private static byte[] derive(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    private static boolean matches(String password, String stored) throws Exception {
        if (!stored.startsWith("pbkdf2$")) return MessageDigest.isEqual(Db.hash(password).getBytes(java.nio.charset.StandardCharsets.UTF_8), stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String[] parts = stored.split("\\$");
        return MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1])), Base64.getDecoder().decode(parts[3]));
    }
    public static String login(String username, String password) throws Exception {
        logout();
        if (username == null || password == null) throw new IllegalStateException("Sai tài khoản hoặc mật khẩu");
        try (PreparedStatement p = Db.get().prepareStatement("SELECT id,password_hash,role,failed_attempts,locked_until,is_active FROM user_account WHERE username=?")) {
            p.setString(1, username.trim());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !r.getBoolean(6)) throw new IllegalStateException("Sai tài khoản hoặc mật khẩu");
                int id = r.getInt(1), attempts = r.getInt(4);
                String stored = r.getString(2), role = r.getString(3);
                Timestamp until = r.getTimestamp(5);
                if (until != null && until.toLocalDateTime().isAfter(LocalDateTime.now())) throw new IllegalStateException("Tài khoản đang khóa 15 phút do nhập sai 5 lần");
                if (until != null) attempts = 0;
                if (!matches(password, stored)) {
                    attempts++;
                    try (PreparedStatement u = Db.get().prepareStatement("UPDATE user_account SET failed_attempts=?,locked_until=? WHERE id=?")) {
                        u.setInt(1, attempts);
                        if (attempts >= 5) u.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().plusMinutes(15))); else u.setNull(2, Types.TIMESTAMP);
                        u.setInt(3, id); u.executeUpdate();
                    }
                    throw new IllegalStateException(attempts >= 5 ? "Tài khoản đã khóa 15 phút" : "Sai tài khoản hoặc mật khẩu");
                }
                try (PreparedStatement u = Db.get().prepareStatement("UPDATE user_account SET failed_attempts=0,locked_until=NULL,password_hash=? WHERE id=?")) {
                    u.setString(1, stored.startsWith("pbkdf2$") ? stored : passwordHash(password)); u.setInt(2, id); u.executeUpdate();
                }
                currentUser = username.trim(); currentRole = role; return role;
            }
        }
    }
    public static void logout() { currentUser = null; currentRole = null; }
    public static String user() { return currentUser; }
    public static String role() { return currentRole; }
    public static boolean allowed(String... roles) { return currentRole != null && Arrays.asList(roles).contains(currentRole); }
    public static void require(String... roles) {
        if (!allowed(roles)) throw new IllegalStateException("Bạn không có quyền thực hiện chức năng này");
    }
    public static void requireLogin() {
        if (currentUser == null) throw new IllegalStateException("Vui lòng đăng nhập");
    }
}
