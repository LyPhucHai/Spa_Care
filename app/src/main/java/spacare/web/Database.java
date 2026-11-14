package spacare.web;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class Database {

  static String url;

  public static void init() throws Exception {
    String file = System.getenv().getOrDefault(
      "SPACARE_DB",
      "./data/spacare_web"
    );
    if (!file.startsWith("mem:")) {
      Path parent = Path.of(file).toAbsolutePath().getParent();
      Files.createDirectories(parent);
    }
    url =
      "jdbc:h2:" +
      (file.startsWith("mem:") ? file : "file:" + file) +
      ";DB_CLOSE_DELAY=-1";
    try (Connection c = open()) {
      String schema = new String(
        Database.class.getResourceAsStream("/schema.sql").readAllBytes(),
        java.nio.charset.StandardCharsets.UTF_8
      );
      for (String sql : schema.split(";")) if (!sql.isBlank()) execute(c, sql);
      execute(
        c,
        "ALTER TABLE customer ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
      );
      if (
        one(c, "SELECT COUNT(*) n FROM app_user")
          .get("n")
          .toString()
          .equals("0")
      ) seed(c);
    }
  }

  public static Connection open() throws SQLException {
    return DriverManager.getConnection(url, "sa", "");
  }

  public static int execute(Connection c, String sql, Object... args)
    throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, args);
      return p.executeUpdate();
    }
  }

  public static long insert(Connection c, String sql, Object... args)
    throws SQLException {
    try (
      PreparedStatement p = c.prepareStatement(
        sql,
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      bind(p, args);
      p.executeUpdate();
      try (ResultSet r = p.getGeneratedKeys()) {
        r.next();
        return r.getLong(1);
      }
    }
  }

  static void bind(PreparedStatement p, Object[] args) throws SQLException {
    for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
  }

  public static List<Map<String, Object>> rows(
    Connection c,
    String sql,
    Object... args
  ) throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, args);
      try (ResultSet r = p.executeQuery()) {
        var out = new ArrayList<Map<String, Object>>();
        var md = r.getMetaData();
        while (r.next()) {
          var row = new LinkedHashMap<String, Object>();
          for (int i = 1; i <= md.getColumnCount(); i++) {
            Object v = r.getObject(i);
            row.put(
              md.getColumnLabel(i).toLowerCase(Locale.ROOT),
              v instanceof java.sql.Timestamp t
                ? t.toLocalDateTime().toString()
                : v instanceof java.sql.Date d
                  ? d.toLocalDate().toString()
                  : v
            );
          }
          out.add(row);
        }
        return out;
      }
    }
  }

  public static Map<String, Object> one(
    Connection c,
    String sql,
    Object... args
  ) throws SQLException {
    var r = rows(c, sql, args);
    if (r.isEmpty()) throw new IllegalArgumentException(
      "Không tìm thấy dữ liệu"
    );
    return r.get(0);
  }

  static void seed(Connection c) throws Exception {
    c.setAutoCommit(false);
    try {
      execute(
        c,
        "INSERT INTO branch(name,address) VALUES ('SpaCare Quận 1','123 Nguyễn Trãi, TP.HCM'),('SpaCare Thủ Đức','45 Võ Văn Ngân, TP.HCM')"
      );
      execute(
        c,
        "INSERT INTO tier(name,min_spending,discount_percent,multiplier) VALUES ('SILVER',0,0,1),('GOLD',10000000,5,1.2),('PLATINUM',30000000,10,1.5)"
      );
      String[] names = { "admin", "ql_hoa", "letan01", "ktv_lan", "khach01" };
      String[] roles = {
        "ADMIN",
        "MANAGER",
        "RECEPTIONIST",
        "TECHNICIAN",
        "CUSTOMER",
      };
      for (int i = 0; i < names.length; i++) execute(
        c,
        "INSERT INTO app_user(username,password_hash,role,full_name) VALUES (?,?,?,?)",
        names[i],
        Security.hash("123456"),
        roles[i],
        new String[] {
          "Quản trị viên",
          "Quản lý Hoa",
          "Lễ tân Mai",
          "Kỹ thuật viên Lan",
          "Nguyễn Thu Hà",
        }[i]
      );
      execute(
        c,
        "INSERT INTO customer(user_id,full_name,phone,email,birthday,allergy_note) VALUES (5,'Nguyễn Thu Hà','0903000123','thuha@example.com','1998-03-08','Dị ứng hương liệu'),(NULL,'Trần Mai','0912000456','mai@example.com','2000-10-20',''),(NULL,'Lê Linh','0987000789','linh@example.com','1996-06-01','')"
      );
      execute(
        c,
        "INSERT INTO service(name,category,price,duration_min,room_type) VALUES ('Massage thư giãn','Body',500000,90,'SPA'),('Gội đầu dưỡng sinh','Hair',150000,60,'HAIR'),('Chăm sóc da cơ bản','Facial',400000,60,'SPA'),('Cắt tóc tạo kiểu','Hair',200000,45,'HAIR'),('Sơn gel chăm sóc móng','Nail',250000,60,'NAIL')"
      );
      execute(
        c,
        "INSERT INTO staff(user_id,full_name,phone,branch_id,commission_percent) VALUES (4,'Lan','0901000001',1,10),(NULL,'Hà','0901000002',1,12),(NULL,'Minh','0901000003',1,10),(NULL,'An','0901000004',2,10)"
      );
      execute(
        c,
        "INSERT INTO staff_skill VALUES (1,1),(1,2),(1,3),(2,1),(2,3),(2,5),(3,2),(3,4),(4,1),(4,3)"
      );
      execute(
        c,
        "INSERT INTO room(name,branch_id,room_type) VALUES ('Spa 01',1,'SPA'),('Spa 02',1,'SPA'),('Ghế tóc 01',1,'HAIR'),('Bàn nail 01',1,'NAIL'),('Spa Thủ Đức',2,'SPA')"
      );
      for (int d = 0; d < 21; d++) for (
        int staff = 1;
        staff <= 4;
        staff++
      ) execute(
        c,
        "INSERT INTO work_shift(staff_id,work_date,start_time,end_time) VALUES (?,?,?,?)",
        staff,
        LocalDate.now().plusDays(d),
        "08:00",
        "20:00"
      );
      execute(
        c,
        "INSERT INTO combo(name,discount_percent) VALUES ('Thư giãn toàn thân',30),('Da sáng móng xinh',20)"
      );
      execute(c, "INSERT INTO combo_item VALUES (1,1),(1,3),(2,3),(2,5)");
      execute(
        c,
        "INSERT INTO treatment_package(name,service_id,total_sessions,price,valid_days) VALUES ('Massage 10 buổi',1,10,3500000,180),('Chăm sóc da 5 buổi',3,5,1600000,90)"
      );
      execute(
        c,
        "INSERT INTO product(name,sku,unit,sale_price,stock_qty,reorder_level) VALUES ('Tinh dầu thư giãn','OIL-01','ml',10000,500,100),('Serum dưỡng da','SER-01','chai',300000,25,5),('Dầu gội thảo mộc','SHA-01','ml',2000,800,200)"
      );
      execute(
        c,
        "INSERT INTO service_product VALUES (1,1,10),(2,3,20),(3,2,1)"
      );
      execute(
        c,
        "INSERT INTO promotion(code,discount_percent,start_date,end_date,max_uses,min_total) VALUES ('SPACARE10',10,?,?,100,200000)",
        LocalDate.now().minusDays(1),
        LocalDate.now().plusMonths(3)
      );
      c.commit();
    } catch (Exception e) {
      c.rollback();
      throw e;
    } finally {
      c.setAutoCommit(true);
    }
  }
}
