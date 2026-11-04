package spacare;

import java.sql.*;
import java.util.*;

/** UC-01: đăng nhập và khóa tài khoản sau 5 lần sai (NFR-02). */
public final class Auth {
    private static final Map<String, Integer> fails = new HashMap<>();

    /** Trả về vai trò nếu đúng, null nếu sai; ném IllegalStateException nếu tài khoản đang bị khóa. */
    public static String login(String user, String password) throws Exception {
        if (fails.getOrDefault(user, 0) >= 5) throw new IllegalStateException("Tài khoản bị khóa do nhập sai 5 lần (NFR-02)");
        try (PreparedStatement p = Db.get().prepareStatement("SELECT role FROM user_account WHERE username=? AND password_hash=?")) {
            p.setString(1, user); p.setString(2, Db.hash(password));
            try (ResultSet r = p.executeQuery()) { if (r.next()) { fails.remove(user); return r.getString(1); } }
        }
        fails.merge(user, 1, Integer::sum);
        return null;
    }
}
