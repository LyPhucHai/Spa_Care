package spacare.web;

import static spacare.web.Business.*;
import static spacare.web.Database.*;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class SpaCareServer {

  record Session(long user, String csrf, Instant expires) {}

  static final Map<String, Session> sessions = new ConcurrentHashMap<>();
  static final Object lock = new Object();
  static String host;

  public static void main(String[] args) throws Exception {
    Database.init();
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    host = System.getenv().getOrDefault("SPACARE_HOST", "127.0.0.1");
    HttpServer server;
    String scheme = "http";
    String store = System.getenv("SPACARE_TLS_KEYSTORE");
    if (store != null && !store.isBlank()) {
      char[] password = System.getenv()
        .getOrDefault("SPACARE_TLS_PASSWORD", "")
        .toCharArray();
      java.security.KeyStore keys = java.security.KeyStore.getInstance(
        "PKCS12"
      );
      try (
        InputStream input = java.nio.file.Files.newInputStream(
          java.nio.file.Path.of(store)
        )
      ) {
        keys.load(input, password);
      }
      javax.net.ssl.KeyManagerFactory km =
        javax.net.ssl.KeyManagerFactory.getInstance(
          javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm()
        );
      km.init(keys, password);
      java.util.Arrays.fill(password, '\0');
      javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance(
        "TLS"
      );
      context.init(km.getKeyManagers(), null, null);
      HttpsServer https = HttpsServer.create(
        new InetSocketAddress(host, port),
        50
      );
      https.setHttpsConfigurator(new HttpsConfigurator(context));
      server = https;
      scheme = "https";
    } else server = HttpServer.create(new InetSocketAddress(host, port), 50);
    server.createContext("/", SpaCareServer::handle);
    server.setExecutor(Executors.newFixedThreadPool(12));
    server.start();
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
      () -> {
        synchronized (lock) {
          try (Connection c = open()) {
            c.setAutoCommit(false);
            execute(
              c,
              "UPDATE notification SET status='SIMULATED',sent_at=CURRENT_TIMESTAMP WHERE status='PENDING' AND due_at<=CURRENT_TIMESTAMP"
            );
            for (var missed : rows(
              c,
              "SELECT id FROM appointment WHERE status='CONFIRMED' AND end_at<?",
              LocalDateTime.now().minusMinutes(15)
            )) {
              long id = id(missed, "id");
              execute(
                c,
                "UPDATE appointment SET status='NO_SHOW' WHERE id=?",
                id
              );
              execute(
                c,
                "UPDATE notification SET status='CANCELLED' WHERE appointment_id=? AND status='PENDING'",
                id
              );
              execute(
                c,
                "INSERT INTO audit_log(action,detail) VALUES ('appointment.auto_no_show',?)",
                "id=" + id
              );
            }
            for (var e : rows(
              c,
              "SELECT id,customer_id,remaining_points FROM loyalty_entry WHERE remaining_points>0 AND expires_at<=CURRENT_TIMESTAMP"
            )) {
              execute(
                c,
                "INSERT INTO loyalty_entry(customer_id,entry_type,points) VALUES (?,'EXPIRE',?)",
                id(e, "customer_id"),
                -id(e, "remaining_points")
              );
              execute(
                c,
                "UPDATE loyalty_entry SET remaining_points=0 WHERE id=?",
                id(e, "id")
              );
            }
            c.commit();
            Backup.daily(c);
            sessions
              .entrySet()
              .removeIf(e -> e.getValue().expires().isBefore(Instant.now()));
          } catch (Exception e) {
            System.err.println("Scheduler: " + e.getMessage());
          }
        }
      },
      2,
      30,
      TimeUnit.SECONDS
    );
    Runtime.getRuntime().addShutdownHook(
      new Thread(() -> {
        scheduler.shutdownNow();
        server.stop(1);
      })
    );
    System.out.println(
      "SpaCare đang chạy: " +
        scheme +
        "://" +
        host +
        ":" +
        port +
        " · JDK " +
        Runtime.version()
    );
  }

  static Map<String, Object> body(HttpExchange x) throws IOException {
    byte[] b = x.getRequestBody().readNBytes(131073);
    if (b.length > 131072) throw bad("Yêu cầu tối đa 128 KB");
    return Json.object(new String(b, StandardCharsets.UTF_8));
  }

  static String cookie(HttpExchange x) {
    String cookies = x.getRequestHeaders().getFirst("Cookie");
    if (cookies != null) for (String part : cookies.split(";")) {
      String[] kv = part.trim().split("=", 2);
      if (kv.length == 2 && kv[0].equals("spacare_session")) return kv[1];
    }
    return "";
  }

  static void json(HttpExchange x, int status, Object result)
    throws IOException {
    byte[] bytes = Json.stringify(result).getBytes(StandardCharsets.UTF_8);
    x.getResponseHeaders().set(
      "Content-Type",
      "application/json; charset=utf-8"
    );
    x.getResponseHeaders().set("Cache-Control", "no-store");
    x.sendResponseHeaders(status, bytes.length);
    x.getResponseBody().write(bytes);
  }

  static Map<String, Object> safeUser(Map<String, Object> u) {
    var out = new LinkedHashMap<String, Object>();
    for (String k : List.of("id", "username", "role", "full_name"))
      out.put(k, u.get(k));
    return out;
  }

  static void handle(HttpExchange x) throws IOException {
    x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    x.getResponseHeaders().set("X-Frame-Options", "DENY");
    x.getResponseHeaders().set("Referrer-Policy", "same-origin");
    try {
      String path = x.getRequestURI().getPath();
      if (!path.startsWith("/api/")) {
        staticFile(x, path);
        return;
      }
      if (!Set.of("GET", "POST").contains(x.getRequestMethod())) {
        json(x, 405, Map.of("error", "Phương thức không hỗ trợ"));
        return;
      }
      String origin = x.getRequestHeaders().getFirst("Origin"),
        requestHost = x.getRequestHeaders().getFirst("Host");
      if (
        origin != null &&
        !origin.equals("http://" + requestHost) &&
        !origin.equals("https://" + requestHost)
      ) throw new Business.Forbidden("Nguồn yêu cầu không được phép");
      synchronized (lock) {
        try (Connection c = open()) {
          if (
            path.equals("/api/login") && x.getRequestMethod().equals("POST")
          ) {
            login(x, c, body(x));
            return;
          }
          if (
            path.equals("/api/register") && x.getRequestMethod().equals("POST")
          ) {
            register(x, c, body(x));
            return;
          }
          Session session = sessions.get(cookie(x));
          if (session == null || session.expires().isBefore(Instant.now())) {
            json(x, 401, Map.of("error", "Vui lòng đăng nhập"));
            return;
          }
          var users = rows(
            c,
            "SELECT * FROM app_user WHERE id=? AND is_active=TRUE",
            session.user()
          );
          if (users.isEmpty()) {
            sessions.remove(cookie(x));
            json(x, 401, Map.of("error", "Tài khoản đã bị vô hiệu hóa"));
            return;
          }
          var user = safeUser(users.get(0));
          if (
            x.getRequestMethod().equals("POST") &&
            !session
              .csrf()
              .equals(x.getRequestHeaders().getFirst("X-CSRF-Token"))
          ) throw new Business.Forbidden(
            "Phiên gửi dữ liệu không hợp lệ, hãy tải lại trang"
          );
          if (
            path.equals("/api/logout") && x.getRequestMethod().equals("POST")
          ) {
            sessions.remove(cookie(x));
            x.getResponseHeaders().add(
              "Set-Cookie",
              "spacare_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0"
            );
            json(x, 200, Map.of("ok", true));
            return;
          }
          if (path.equals("/api/me") && x.getRequestMethod().equals("GET")) {
            json(x, 200, Map.of("user", user, "csrf", session.csrf()));
            return;
          }
          Business business = new Business(c, user);
          if (path.equals("/api/state") && x.getRequestMethod().equals("GET")) {
            json(
              x,
              200,
              business.state(query(x.getRequestURI().getRawQuery()))
            );
            return;
          }
          if (
            path.equals("/api/action") && x.getRequestMethod().equals("POST")
          ) {
            var a = body(x);
            c.setAutoCommit(false);
            try {
              Object result = business.action(a);
              c.commit();
              json(x, 200, result);
            } catch (Exception e) {
              c.rollback();
              throw e;
            }
            return;
          }
          if (
            path.equals("/api/backup") && x.getRequestMethod().equals("GET")
          ) {
            business.require("ADMIN");
            var file = Backup.create(c);
            byte[] bytes = java.nio.file.Files.readAllBytes(file);
            x.getResponseHeaders().set("Content-Type", "application/zip");
            x.getResponseHeaders().set(
              "Content-Disposition",
              "attachment; filename=spacare-backup.zip"
            );
            x.sendResponseHeaders(200, bytes.length);
            x.getResponseBody().write(bytes);
            return;
          }
          if (
            path.equals("/api/export") && x.getRequestMethod().equals("GET")
          ) {
            business.require("ADMIN", "MANAGER");
            var filter = query(x.getRequestURI().getRawQuery());
            @SuppressWarnings("unchecked")
            var state = (Map<String, Object>) business.state(filter);
            @SuppressWarnings("unchecked")
            var revenue = (List<Map<String, Object>>) state.get("revenue");
            StringBuilder csv = new StringBuilder(
              "\ufeffNgày,Số hóa đơn,Doanh thu VND\r\n"
            );
            for (var r : revenue)
              csv
                .append(r.get("day"))
                .append(',')
                .append(r.get("invoices"))
                .append(',')
                .append(r.get("revenue"))
                .append("\r\n");
            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            x.getResponseHeaders().set(
              "Content-Type",
              "text/csv; charset=utf-8"
            );
            x.getResponseHeaders().set(
              "Content-Disposition",
              "attachment; filename=spacare-revenue.csv"
            );
            x.sendResponseHeaders(200, bytes.length);
            x.getResponseBody().write(bytes);
            return;
          }
          json(x, 404, Map.of("error", "Đường dẫn không tồn tại"));
        }
      }
    } catch (Business.Forbidden e) {
      json(x, 403, Map.of("error", e.getMessage()));
    } catch (
      IllegalArgumentException
      | java.time.DateTimeException
      | ArithmeticException e
    ) {
      json(
        x,
        400,
        Map.of(
          "error",
          Objects.toString(e.getMessage(), "Dữ liệu không hợp lệ")
        )
      );
    } catch (SQLException e) {
      String state = e.getSQLState();
      json(
        x,
        409,
        Map.of(
          "error",
          state != null && state.startsWith("23")
            ? "Dữ liệu trùng hoặc vi phạm ràng buộc. Kiểm tra tài khoản, điện thoại, đánh giá, liên kết dữ liệu."
            : "Thao tác dữ liệu không thành công"
        )
      );
      System.err.println("SQL: " + e.getMessage());
    } catch (Exception e) {
      System.err.println(e);
      json(x, 500, Map.of("error", "Máy chủ không thể xử lý yêu cầu"));
    } finally {
      x.close();
    }
  }

  static Map<String, Object> query(String raw) {
    var result = new LinkedHashMap<String, Object>();
    if (raw != null) for (String part : raw.split("&")) {
      String[] kv = part.split("=", 2);
      result.put(
        URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
        kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : ""
      );
    }
    return result;
  }

  static void login(HttpExchange x, Connection c, Map<String, Object> a)
    throws Exception {
    String username = required(a, "username", 50),
      password = required(a, "password", 100);
    var accounts = rows(c, "SELECT * FROM app_user WHERE username=?", username);
    if (accounts.isEmpty()) {
      json(x, 401, Map.of("error", "Sai tài khoản hoặc mật khẩu"));
      return;
    }
    var u = accounts.get(0);
    if (!bool(u, "is_active")) {
      json(x, 401, Map.of("error", "Tài khoản đã bị vô hiệu hóa"));
      return;
    }
    String until = str(u, "locked_until");
    if (
      !until.isEmpty() &&
      LocalDateTime.parse(until).isAfter(LocalDateTime.now())
    ) {
      json(x, 423, Map.of("error", "Tài khoản khóa 15 phút sau 5 lần sai"));
      return;
    }
    if (!Security.verify(password, str(u, "password_hash"))) {
      int attempts = until.isEmpty() ? integer(u, "failed_attempts") + 1 : 1;
      execute(
        c,
        "UPDATE app_user SET failed_attempts=?,locked_until=? WHERE id=?",
        attempts,
        attempts >= 5 ? LocalDateTime.now().plusMinutes(15) : null,
        id(u, "id")
      );
      json(
        x,
        attempts >= 5 ? 423 : 401,
        Map.of(
          "error",
          attempts >= 5
            ? "Tài khoản đã khóa 15 phút"
            : "Sai tài khoản hoặc mật khẩu"
        )
      );
      return;
    }
    execute(
      c,
      "UPDATE app_user SET failed_attempts=0,locked_until=NULL WHERE id=?",
      id(u, "id")
    );
    String token = Security.token(),
      csrf = Security.token();
    sessions.put(
      token,
      new Session(id(u, "id"), csrf, Instant.now().plusSeconds(8 * 3600))
    );
    x.getResponseHeaders().add(
      "Set-Cookie",
      "spacare_session=" +
        token +
        "; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800" +
        (x instanceof HttpsExchange ? "; Secure" : "")
    );
    json(x, 200, Map.of("user", safeUser(u), "csrf", csrf));
  }

  static void register(HttpExchange x, Connection c, Map<String, Object> a)
    throws Exception {
    String username = required(a, "username", 50),
      pw = required(a, "password", 100),
      name = required(a, "full_name", 100),
      ph = phone(a);
    if (!username.matches("[A-Za-z0-9_.-]{3,50}") || pw.length() < 8) throw bad(
      "Tài khoản tối thiểu 3 ký tự, mật khẩu tối thiểu 8 ký tự"
    );
    c.setAutoCommit(false);
    try {
      long u = insert(
        c,
        "INSERT INTO app_user(username,password_hash,role,full_name) VALUES (?,?,'CUSTOMER',?)",
        username,
        Security.hash(pw),
        name
      );
      execute(
        c,
        "INSERT INTO customer(user_id,full_name,phone,email,allergy_note) VALUES (?,?,?,?,?)",
        u,
        name,
        ph,
        str(a, "email"),
        ""
      );
      c.commit();
      json(x, 201, Map.of("ok", true));
    } catch (Exception e) {
      c.rollback();
      throw e;
    }
  }

  static void staticFile(HttpExchange x, String path) throws IOException {
    if (!x.getRequestMethod().equals("GET")) {
      x.sendResponseHeaders(405, -1);
      return;
    }
    String resource = switch (path) {
      case "/", "/index.html" -> "/public/index.html";
      case "/app.js" -> "/public/app.js";
      case "/style.css" -> "/public/style.css";
      default -> null;
    };
    if (resource == null) {
      x.sendResponseHeaders(404, -1);
      return;
    }
    try (InputStream in = SpaCareServer.class.getResourceAsStream(resource)) {
      if (in == null) {
        x.sendResponseHeaders(404, -1);
        return;
      }
      byte[] bytes = in.readAllBytes();
      x.getResponseHeaders().set(
        "Content-Type",
        path.endsWith(".js")
          ? "text/javascript; charset=utf-8"
          : path.endsWith(".css")
            ? "text/css; charset=utf-8"
            : "text/html; charset=utf-8"
      );
      x.getResponseHeaders().set(
        "Content-Security-Policy",
        "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"
      );
      x.sendResponseHeaders(200, bytes.length);
      x.getResponseBody().write(bytes);
    }
  }
}
