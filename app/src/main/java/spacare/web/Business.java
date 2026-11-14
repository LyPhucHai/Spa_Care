package spacare.web;

import static spacare.web.Database.*;

import java.sql.*;
import java.time.*;
import java.util.*;

/** Business operations run inside one serialized DB transaction by the HTTP layer. */
public final class Business {

  final Connection c;
  final Map<String, Object> user;

  public Business(Connection c, Map<String, Object> user) {
    this.c = c;
    this.user = user;
  }

  static long num(Object v) {
    return v == null ? 0 : Long.parseLong(v.toString());
  }

  static long id(Map<String, Object> a, String key) {
    return num(a.get(key));
  }

  static int integer(Map<String, Object> a, String key) {
    return Math.toIntExact(id(a, key));
  }

  static String str(Map<String, Object> a, String key) {
    return Objects.toString(a.get(key), "").trim();
  }

  static boolean bool(Map<String, Object> a, String key) {
    return Boolean.parseBoolean(Objects.toString(a.get(key), "true"));
  }

  static String required(Map<String, Object> a, String key, int max) {
    String s = str(a, key);
    if (s.isBlank() || s.length() > max) throw bad(
      "Trường " + key + " cần từ 1 đến " + max + " ký tự"
    );
    return s;
  }

  static IllegalArgumentException bad(String s) {
    return new IllegalArgumentException(s);
  }

  boolean role(String... roles) {
    return Arrays.asList(roles).contains(str(user, "role"));
  }

  void require(String... roles) {
    if (!role(roles)) throw new Forbidden(
      "Bạn không có quyền thực hiện chức năng này"
    );
  }

  long uid() {
    return id(user, "id");
  }

  long ownCustomer() throws SQLException {
    return id(one(c, "SELECT id FROM customer WHERE user_id=?", uid()), "id");
  }

  long ownStaff() throws SQLException {
    return id(
      one(c, "SELECT id FROM staff WHERE user_id=? AND is_active=TRUE", uid()),
      "id"
    );
  }

  void customerAccess(long customer) throws SQLException {
    if (role("CUSTOMER")) {
      if (ownCustomer() != customer) throw new Forbidden(
        "Chỉ được xem dữ liệu của bạn"
      );
    } else require("ADMIN", "MANAGER", "RECEPTIONIST");
  }

  public static class Forbidden extends RuntimeException {

    Forbidden(String m) {
      super(m);
    }
  }

  static List<Long> numbers(Map<String, Object> a, String key) {
    Object v = a.get(key);
    if (!(v instanceof List<?> list)) return List.of();
    return list.stream().map(Business::num).distinct().toList();
  }

  static LocalDate date(Map<String, Object> a, String key) {
    return LocalDate.parse(required(a, key, 10));
  }

  static LocalDateTime timestamp(Map<String, Object> a, String key) {
    return LocalDateTime.parse(required(a, key, 30));
  }

  static long bounded(long v, long min, long max, String label) {
    if (v < min || v > max) throw bad(
      label + " phải trong khoảng " + min + "–" + max
    );
    return v;
  }

  static String phone(Map<String, Object> a) {
    String p = required(a, "phone", 15);
    if (!p.matches("0\\d{9}")) throw bad(
      "Điện thoại gồm 10 số, bắt đầu bằng 0"
    );
    return p;
  }

  static String method(Map<String, Object> a) {
    String m = str(a, "method");
    if (!Set.of("CASH", "CARD", "TRANSFER", "QR").contains(m)) throw bad(
      "Hình thức thanh toán không hợp lệ"
    );
    return m;
  }

  void audit(String action, long ref) throws SQLException {
    execute(
      c,
      "INSERT INTO audit_log(user_id,action,detail) VALUES (?,?,?)",
      uid(),
      action,
      "id=" + ref
    );
  }

  long balance(long customer) throws SQLException {
    return num(
      one(
        c,
        "SELECT COALESCE(SUM(remaining_points),0) n FROM loyalty_entry WHERE customer_id=? AND expires_at>CURRENT_TIMESTAMP",
        customer
      ).get("n")
    );
  }

  long spending(long customer) throws SQLException {
    return num(
      one(
        c,
        "SELECT COALESCE(SUM(total),0) n FROM invoice WHERE customer_id=? AND status='PAID' AND created_at>=?",
        customer,
        LocalDateTime.now().minusMonths(12)
      ).get("n")
    );
  }

  Map<String, Object> tier(long customer) throws SQLException {
    return one(
      c,
      "SELECT * FROM tier WHERE min_spending<=? ORDER BY min_spending DESC LIMIT 1",
      spending(customer)
    );
  }

  void loyalty(
    long customer,
    long invoice,
    long total,
    int redeem,
    double multiplier
  ) throws SQLException {
    if (redeem > 0) {
      int left = redeem;
      for (var e : rows(
        c,
        "SELECT id,remaining_points FROM loyalty_entry WHERE customer_id=? AND remaining_points>0 AND expires_at>CURRENT_TIMESTAMP ORDER BY expires_at,id",
        customer
      )) {
        int used = Math.min(left, integer(e, "remaining_points"));
        execute(
          c,
          "UPDATE loyalty_entry SET remaining_points=remaining_points-? WHERE id=?",
          used,
          id(e, "id")
        );
        left -= used;
        if (left == 0) break;
      }
      if (left > 0) throw bad("Không đủ điểm");
      execute(
        c,
        "INSERT INTO loyalty_entry(customer_id,invoice_id,entry_type,points) VALUES (?,?,'REDEEM',?)",
        customer,
        invoice,
        -redeem
      );
    }
    int earn = (int) Math.floor((total / 10000.0) * multiplier);
    if (earn > 0) execute(
      c,
      "INSERT INTO loyalty_entry(customer_id,invoice_id,entry_type,points,remaining_points,expires_at) VALUES (?,?,'EARN',?,?,?)",
      customer,
      invoice,
      earn,
      earn,
      LocalDateTime.now().plusMonths(12)
    );
  }

  public Object action(Map<String, Object> a) throws Exception {
    String command = required(a, "action", 60);
    return switch (command) {
      case "customer.save" -> saveCustomer(a);
      case "customer.active" -> customerActive(a);
      case "branch.save" -> saveBranch(a);
      case "room.save" -> saveRoom(a);
      case "service.save" -> saveService(a);
      case "staff.save" -> saveStaff(a);
      case "shift.save" -> saveShift(a);
      case "shift.delete" -> deleteShift(a);
      case "attendance.toggle" -> attendance(a);
      case "appointment.create" -> book(a);
      case "appointment.reschedule" -> reschedule(a);
      case "appointment.cancel" -> cancel(a);
      case "appointment.status" -> status(a);
      case "appointment.slots" -> slots(a);
      case "invoice.preview" -> price(a, false);
      case "invoice.pay" -> price(a, true);
      case "package.save" -> savePackage(a);
      case "package.sell" -> sellPackage(a);
      case "combo.save" -> saveCombo(a);
      case "promotion.save" -> savePromotion(a);
      case "tier.save" -> saveTier(a);
      case "product.save" -> saveProduct(a);
      case "stock.adjust" -> stock(a);
      case "recipe.save" -> recipe(a);
      case "review.save" -> review(a);
      case "notification.dispatch" -> dispatch();
      case "account.save" -> saveAccount(a);
      case "password.change" -> password(a);
      default -> throw bad("Chức năng không tồn tại");
    };
  }

  Object saveCustomer(Map<String, Object> a) throws SQLException {
    long cid = id(a, "id");
    if (role("CUSTOMER")) {
      if (cid != ownCustomer()) throw new Forbidden(
        "Chỉ được sửa hồ sơ của bạn"
      );
    } else require("ADMIN", "MANAGER", "RECEPTIONIST");
    String name = required(a, "full_name", 100),
      ph = phone(a);
    String birth = str(a, "birthday");
    if (
      !birth.isBlank() && LocalDate.parse(birth).isAfter(LocalDate.now())
    ) throw bad("Ngày sinh không được ở tương lai");
    Object[] v = {
      name,
      ph,
      str(a, "email"),
      birth.isBlank() ? null : LocalDate.parse(birth),
      str(a, "allergy_note"),
    };
    if (cid == 0) cid = insert(
      c,
      "INSERT INTO customer(full_name,phone,email,birthday,allergy_note) VALUES (?,?,?,?,?)",
      v
    );
    else {
      Object[] all = Arrays.copyOf(v, 6);
      all[5] = cid;
      if (
        execute(
          c,
          "UPDATE customer SET full_name=?,phone=?,email=?,birthday=?,allergy_note=? WHERE id=?",
          all
        ) == 0
      ) throw bad("Không tìm thấy khách");
    }
    execute(
      c,
      "UPDATE app_user SET full_name=? WHERE id=(SELECT user_id FROM customer WHERE id=?)",
      name,
      cid
    );
    audit("customer.save", cid);
    return Map.of("id", cid);
  }

  Object customerActive(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long customer = id(a, "id");
    boolean active = bool(a, "is_active");
    if (
      !active &&
      num(
        one(
          c,
          "SELECT COUNT(*) n FROM appointment WHERE customer_id=? AND status IN ('CONFIRMED','CHECKED_IN','IN_PROGRESS')",
          customer
        ).get("n")
      ) > 0
    ) throw bad("Khách còn lịch chưa hoàn thành");
    if (
      execute(
        c,
        "UPDATE customer SET is_active=? WHERE id=?",
        active,
        customer
      ) == 0
    ) throw bad("Không tìm thấy khách");
    execute(
      c,
      "UPDATE app_user SET is_active=? WHERE id=(SELECT user_id FROM customer WHERE id=?)",
      active,
      customer
    );
    audit("customer.active", customer);
    return Map.of("ok", true);
  }

  Object saveBranch(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long branch = id(a, "id");
    String name = required(a, "name", 100),
      address = required(a, "address", 250);
    if (branch == 0) branch = insert(
      c,
      "INSERT INTO branch(name,address) VALUES (?,?)",
      name,
      address
    );
    else if (
      execute(
        c,
        "UPDATE branch SET name=?,address=? WHERE id=?",
        name,
        address,
        branch
      ) == 0
    ) throw bad("Không có chi nhánh");
    audit("branch.save", branch);
    return Map.of("id", branch);
  }

  Object saveRoom(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long room = id(a, "id"),
      branch = id(a, "branch_id");
    String name = required(a, "name", 100),
      type = required(a, "room_type", 20);
    boolean active = bool(a, "is_active");
    if (room > 0) {
      var old = one(c, "SELECT * FROM room WHERE id=?", room);
      if (
        (!active ||
          branch != id(old, "branch_id") ||
          !type.equals(str(old, "room_type"))) &&
        num(
          one(
            c,
            "SELECT COUNT(*) n FROM appointment_item i JOIN appointment ap ON ap.id=i.appointment_id WHERE i.room_id=? AND ap.status IN ('CONFIRMED','CHECKED_IN','IN_PROGRESS')",
            room
          ).get("n")
        ) > 0
      ) throw bad("Phòng còn lịch chưa hoàn thành");
    }
    if (room == 0) room = insert(
      c,
      "INSERT INTO room(name,branch_id,room_type,is_active) VALUES (?,?,?,?)",
      name,
      branch,
      type,
      active
    );
    else execute(
      c,
      "UPDATE room SET name=?,branch_id=?,room_type=?,is_active=? WHERE id=?",
      name,
      branch,
      type,
      active,
      room
    );
    audit("room.save", room);
    return Map.of("id", room);
  }

  Object saveService(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long sid = id(a, "id");
    String name = required(a, "name", 100),
      category = required(a, "category", 50),
      type = required(a, "room_type", 20);
    bounded(id(a, "price"), 0, 1000000000, "Giá");
    bounded(id(a, "duration_min"), 15, 480, "Thời lượng");
    Object[] v = {
      name,
      category,
      id(a, "price"),
      id(a, "duration_min"),
      type,
      bool(a, "is_active"),
    };
    if (sid == 0) sid = insert(
      c,
      "INSERT INTO service(name,category,price,duration_min,room_type,is_active) VALUES (?,?,?,?,?,?)",
      v
    );
    else {
      var all = Arrays.copyOf(v, 7);
      all[6] = sid;
      if (
        execute(
          c,
          "UPDATE service SET name=?,category=?,price=?,duration_min=?,room_type=?,is_active=? WHERE id=?",
          all
        ) == 0
      ) throw bad("Dịch vụ không tồn tại");
    }
    audit("service.save", sid);
    return Map.of("id", sid);
  }

  Object saveStaff(Map<String, Object> a) throws Exception {
    require("ADMIN", "MANAGER");
    long sid = id(a, "id");
    String name = required(a, "full_name", 100),
      ph = phone(a);
    long branch = id(a, "branch_id"),
      commission = bounded(id(a, "commission_percent"), 0, 100, "Hoa hồng");
    one(c, "SELECT id FROM branch WHERE id=?", branch);
    if (sid > 0) {
      var old = one(c, "SELECT * FROM staff WHERE id=?", sid);
      if (
        !bool(a, "is_active") &&
        num(
          one(
            c,
            "SELECT COUNT(*) n FROM appointment_item i JOIN appointment ap ON ap.id=i.appointment_id WHERE i.staff_id=? AND ap.status IN ('CONFIRMED','CHECKED_IN','IN_PROGRESS')",
            sid
          ).get("n")
        ) > 0
      ) throw bad("Nhân viên còn lịch chưa hoàn thành");
      if (
        id(old, "branch_id") != branch &&
        num(
          one(
            c,
            "SELECT COUNT(*) n FROM appointment_item i JOIN appointment ap ON ap.id=i.appointment_id WHERE i.staff_id=? AND ap.status IN ('CONFIRMED','CHECKED_IN','IN_PROGRESS')",
            sid
          ).get("n")
        ) > 0
      ) throw bad("Không chuyển chi nhánh khi còn lịch hẹn");
    }
    if (sid == 0) {
      Long account = null;
      String username = str(a, "username");
      if (!username.isBlank()) {
        String pw = required(a, "password", 100);
        if (pw.length() < 8) throw bad("Mật khẩu mới tối thiểu 8 ký tự");
        account = insert(
          c,
          "INSERT INTO app_user(username,password_hash,role,full_name) VALUES (?,?,'TECHNICIAN',?)",
          username,
          Security.hash(pw),
          name
        );
      }
      sid = insert(
        c,
        "INSERT INTO staff(user_id,full_name,phone,branch_id,commission_percent,is_active) VALUES (?,?,?,?,?,?)",
        account,
        name,
        ph,
        branch,
        commission,
        bool(a, "is_active")
      );
    } else execute(
      c,
      "UPDATE staff SET full_name=?,phone=?,branch_id=?,commission_percent=?,is_active=? WHERE id=?",
      name,
      ph,
      branch,
      commission,
      bool(a, "is_active"),
      sid
    );
    List<Long> skills = numbers(a, "service_ids");
    if (skills.isEmpty()) throw bad("Chọn ít nhất một chuyên môn");
    execute(c, "DELETE FROM staff_skill WHERE staff_id=?", sid);
    for (long service : skills)
      execute(c, "INSERT INTO staff_skill VALUES (?,?)", sid, service);
    execute(
      c,
      "UPDATE app_user SET full_name=?,is_active=? WHERE id=(SELECT user_id FROM staff WHERE id=?)",
      name,
      bool(a, "is_active"),
      sid
    );
    audit("staff.save", sid);
    return Map.of("id", sid);
  }

  Object saveShift(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long staff = id(a, "staff_id");
    LocalDate day = date(a, "work_date");
    if (day.isBefore(LocalDate.now())) throw bad(
      "Không phân ca cho ngày quá khứ"
    );
    LocalTime start = LocalTime.parse(str(a, "start_time")),
      end = LocalTime.parse(str(a, "end_time"));
    if (!end.isAfter(start)) throw bad("Giờ kết thúc phải sau giờ bắt đầu");
    one(c, "SELECT id FROM staff WHERE id=? AND is_active=TRUE", staff);
    if (
      num(
        one(
          c,
          "SELECT COUNT(*) n FROM work_shift WHERE staff_id=? AND work_date=? AND start_time<? AND end_time>?",
          staff,
          day,
          end,
          start
        ).get("n")
      ) > 0
    ) throw bad("Ca làm bị giao nhau");
    long shift = insert(
      c,
      "INSERT INTO work_shift(staff_id,work_date,start_time,end_time) VALUES (?,?,?,?)",
      staff,
      day,
      start,
      end
    );
    audit("shift.save", shift);
    return Map.of("id", shift);
  }

  Object deleteShift(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long shift = id(a, "id");
    var s = one(c, "SELECT * FROM work_shift WHERE id=?", shift);
    LocalDate day = LocalDate.parse(str(s, "work_date"));
    if (
      num(
        one(
          c,
          "SELECT COUNT(*) n FROM appointment_item i JOIN appointment ap ON ap.id=i.appointment_id WHERE i.staff_id=? AND i.start_at<? AND i.end_at>? AND ap.status NOT IN ('CANCELLED','NO_SHOW')",
          id(s, "staff_id"),
          day.atTime(LocalTime.parse(str(s, "end_time"))),
          day.atTime(LocalTime.parse(str(s, "start_time")))
        ).get("n")
      ) > 0
    ) throw bad("Ca đang có lịch hẹn, hãy dời lịch trước");
    execute(c, "DELETE FROM work_shift WHERE id=?", shift);
    audit("shift.delete", shift);
    return Map.of("ok", true);
  }

  Object attendance(Map<String, Object> a) throws SQLException {
    long staff;
    if (role("TECHNICIAN")) staff = ownStaff();
    else {
      require("ADMIN", "MANAGER");
      staff = id(a, "staff_id");
    }
    one(c, "SELECT id FROM staff WHERE id=? AND is_active=TRUE", staff);
    var today = rows(
      c,
      "SELECT * FROM attendance WHERE staff_id=? AND work_date=CURRENT_DATE",
      staff
    );
    if (today.isEmpty()) {
      execute(
        c,
        "INSERT INTO attendance(staff_id,work_date,check_in) VALUES (?,CURRENT_DATE,CURRENT_TIMESTAMP)",
        staff
      );
    } else {
      if (today.get(0).get("check_out") != null) throw bad(
        "Đã chấm công ra hôm nay"
      );
      execute(
        c,
        "UPDATE attendance SET check_out=CURRENT_TIMESTAMP WHERE id=?",
        id(today.get(0), "id")
      );
    }
    audit("attendance.toggle", staff);
    return Map.of("ok", true);
  }

  record Plan(
    long service,
    long staff,
    long room,
    LocalDateTime start,
    LocalDateTime end,
    long price,
    Long customerPackage
  ) {}

  boolean free(
    long staff,
    long room,
    LocalDateTime start,
    LocalDateTime end,
    long exclude
  ) throws SQLException {
    if (
      num(
        one(
          c,
          "SELECT COUNT(*) n FROM work_shift WHERE staff_id=? AND work_date=? AND start_time<=? AND end_time>=?",
          staff,
          start.toLocalDate(),
          start.toLocalTime(),
          end.toLocalTime()
        ).get("n")
      ) == 0 ||
      !end.toLocalDate().equals(start.toLocalDate())
    ) return false;
    return (
      num(
        one(
          c,
          "SELECT COUNT(*) n FROM appointment_item i JOIN appointment a ON a.id=i.appointment_id WHERE a.status NOT IN ('CANCELLED','NO_SHOW') AND a.id<>? AND (i.staff_id=? OR i.room_id=?) AND i.start_at<? AND i.end_at>?",
          exclude,
          staff,
          room,
          end,
          start
        ).get("n")
      ) == 0
    );
  }

  List<Plan> plan(
    List<Long> services,
    long preferred,
    long branch,
    LocalDateTime start,
    long exclude,
    List<Map<String, Object>> originals
  ) throws SQLException {
    if (services.isEmpty() || services.size() > 10) throw bad(
      "Chọn từ 1 đến 10 dịch vụ"
    );
    var result = new ArrayList<Plan>();
    LocalDateTime cursor = start;
    for (int ix = 0; ix < services.size(); ix++) {
      long service = services.get(ix);
      var sv = one(
        c,
        "SELECT * FROM service WHERE id=?" +
          (originals == null ? " AND is_active=TRUE" : ""),
        service
      );
      long duration =
        originals == null
          ? id(sv, "duration_min")
          : Duration.between(
              LocalDateTime.parse(str(originals.get(ix), "start_at")),
              LocalDateTime.parse(str(originals.get(ix), "end_at"))
            ).toMinutes();
      LocalDateTime end = cursor.plusMinutes(duration);
      Plan chosen = null;
      long requested =
        originals == null ? preferred : id(originals.get(ix), "staff_id");
      var candidates = rows(
        c,
        "SELECT st.id FROM staff st JOIN staff_skill sk ON sk.staff_id=st.id WHERE sk.service_id=? AND st.branch_id=? AND st.is_active=TRUE AND (?=0 OR st.id=?) ORDER BY st.id",
        service,
        branch,
        requested,
        requested
      );
      for (var st : candidates) {
        for (var room : rows(
          c,
          "SELECT id FROM room WHERE branch_id=? AND room_type=? AND is_active=TRUE ORDER BY id",
          branch,
          str(sv, "room_type")
        )) {
          if (free(id(st, "id"), id(room, "id"), cursor, end, exclude)) {
            chosen = new Plan(
              service,
              id(st, "id"),
              id(room, "id"),
              cursor,
              end,
              originals == null
                ? id(sv, "price")
                : id(originals.get(ix), "price_at_booking"),
              originals == null
                ? null
                : originals.get(ix).get("customer_package_id") == null
                  ? null
                  : id(originals.get(ix), "customer_package_id")
            );
            break;
          }
        }
        if (chosen != null) break;
      }
      if (chosen == null) throw bad(
        "Không có nhân viên/phòng phù hợp hoặc bị trùng lịch cho " +
          str(sv, "name") +
          " lúc " +
          cursor.toLocalTime()
      );
      result.add(chosen);
      cursor = end;
    }
    return result;
  }

  List<Long> bookingServices(Map<String, Object> a) throws SQLException {
    long combo = id(a, "combo_id");
    if (combo > 0) {
      one(c, "SELECT id FROM combo WHERE id=? AND is_active=TRUE", combo);
      return rows(
        c,
        "SELECT service_id FROM combo_item WHERE combo_id=? ORDER BY service_id",
        combo
      )
        .stream()
        .map(r -> id(r, "service_id"))
        .toList();
    }
    return numbers(a, "service_ids");
  }

  Object slots(Map<String, Object> a) throws SQLException {
    if (!role("CUSTOMER")) require("ADMIN", "MANAGER", "RECEPTIONIST");
    var services = bookingServices(a);
    long branch = id(a, "branch_id"),
      staff = id(a, "staff_id");
    LocalDate day = date(a, "date");
    var out = new ArrayList<String>();
    for (
      LocalTime t = LocalTime.of(8, 0);
      !t.isAfter(LocalTime.of(19, 30));
      t = t.plusMinutes(30)
    ) {
      LocalDateTime start = day.atTime(t);
      if (!start.isAfter(LocalDateTime.now())) continue;
      try {
        plan(services, staff, branch, start, 0, null);
        out.add(t.toString());
      } catch (IllegalArgumentException ignored) {}
    }
    return Map.of("slots", out);
  }

  void checkCustomerOverlap(
    long customer,
    LocalDateTime start,
    LocalDateTime end,
    long exclude,
    Map<String, Object> input
  ) throws SQLException {
    if (
      !Boolean.TRUE.equals(input.get("confirm_overlap")) &&
      num(
        one(
          c,
          "SELECT COUNT(*) n FROM appointment WHERE customer_id=? AND id<>? AND status NOT IN ('CANCELLED','NO_SHOW') AND start_at<? AND end_at>?",
          customer,
          exclude,
          end,
          start
        ).get("n")
      ) > 0
    ) throw bad(
      "Khách đã có lịch trong khoảng này. Chọn giờ khác hoặc xác nhận đặt thay người đi cùng."
    );
  }

  Object book(Map<String, Object> a) throws SQLException {
    long customer = role("CUSTOMER") ? ownCustomer() : id(a, "customer_id");
    customerAccess(customer);
    one(c, "SELECT id FROM customer WHERE id=? AND is_active=TRUE", customer);
    long branch = id(a, "branch_id");
    one(c, "SELECT id FROM branch WHERE id=?", branch);
    LocalDateTime start = timestamp(a, "start_at");
    if (!start.isAfter(LocalDateTime.now())) throw bad(
      "Giờ hẹn phải ở tương lai"
    );
    if (start.getMinute() % 15 != 0 || start.getSecond() != 0) throw bad(
      "Chọn giờ theo bước 15 phút"
    );
    List<Long> services = bookingServices(a);
    int group = (int) bounded(
      a.containsKey("group_count") ? id(a, "group_count") : 1,
      1,
      10,
      "Số người"
    );
    long cp = id(a, "customer_package_id");
    if (cp > 0 && (group != 1 || id(a, "combo_id") > 0)) throw bad(
      "Liệu trình không dùng cùng combo/nhóm"
    );
    int combo =
      id(a, "combo_id") == 0
        ? 0
        : integer(
            one(
              c,
              "SELECT discount_percent FROM combo WHERE id=?",
              id(a, "combo_id")
            ),
            "discount_percent"
          );
    int groupDiscount = group == 2 ? 50 : group >= 5 ? 30 : group >= 3 ? 20 : 0;
    combo = Math.max(combo, groupDiscount);
    var ids = new ArrayList<Long>();
    for (int guest = 0; guest < group; guest++) {
      var plan = plan(services, id(a, "staff_id"), branch, start, 0, null);
      if (guest == 0) checkCustomerOverlap(
        customer,
        start,
        plan.get(plan.size() - 1).end(),
        0,
        a
      );
      long cpService = 0;
      if (cp > 0) {
        var pkg = one(
          c,
          "SELECT cp.*,p.service_id FROM customer_package cp JOIN treatment_package p ON p.id=cp.package_id WHERE cp.id=? AND cp.customer_id=?",
          cp,
          customer
        );
        cpService = id(pkg, "service_id");
        if (!services.contains(cpService)) throw bad(
          "Gói không bao gồm dịch vụ được chọn"
        );
        if (
          LocalDateTime.parse(str(pkg, "expires_at")).isBefore(start)
        ) throw bad("Gói hết hạn trước ngày hẹn");
        if (availablePackage(cp) <= 0) throw bad("Gói không còn buổi trống");
      }
      long appt = insert(
        c,
        "INSERT INTO appointment(customer_id,branch_id,start_at,end_at,note,combo_percent,created_by) VALUES (?,?,?,?,?,?,?)",
        customer,
        branch,
        start,
        plan.get(plan.size() - 1).end(),
        str(a, "note") +
          (group > 1 ? " · Nhóm " + group + " / khách " + (guest + 1) : ""),
        combo,
        uid()
      );
      for (Plan item : plan)
        execute(
          c,
          "INSERT INTO appointment_item(appointment_id,service_id,staff_id,room_id,start_at,end_at,price_at_booking,customer_package_id) VALUES (?,?,?,?,?,?,?,?)",
          appt,
          item.service(),
          item.staff(),
          item.room(),
          item.start(),
          item.end(),
          item.price(),
          cp > 0 && item.service() == cpService ? cp : null
        );
      notify(appt, customer, start, "Đặt lịch thành công #" + appt);
      audit("appointment.create", appt);
      ids.add(appt);
    }
    return Map.of("ids", ids);
  }

  long availablePackage(long cp) throws SQLException {
    long reserved = num(
      one(
        c,
        "SELECT COUNT(*) n FROM appointment_item i JOIN appointment a ON a.id=i.appointment_id WHERE i.customer_package_id=? AND a.status NOT IN ('CANCELLED','NO_SHOW') AND NOT EXISTS(SELECT 1 FROM invoice v WHERE v.appointment_id=a.id)",
        cp
      ).get("n")
    );
    return (
      id(
        one(
          c,
          "SELECT remaining_sessions FROM customer_package WHERE id=?",
          cp
        ),
        "remaining_sessions"
      ) - reserved
    );
  }

  void notify(long appt, long customer, LocalDateTime start, String message)
    throws SQLException {
    execute(
      c,
      "UPDATE notification SET status='CANCELLED' WHERE appointment_id=? AND status='PENDING'",
      appt
    );
    execute(
      c,
      "DELETE FROM notification WHERE appointment_id=? AND kind='REMINDER'",
      appt
    );
    execute(
      c,
      "INSERT INTO notification(customer_id,appointment_id,channel,message,status,due_at,kind) VALUES (?,?,'IN_APP',?,'PENDING',?,'REMINDER')",
      customer,
      appt,
      "Nhắc lịch #" + appt + " · " + start,
      start.minusHours(24)
    );
    execute(
      c,
      "INSERT INTO notification(customer_id,appointment_id,channel,message,status,due_at,kind) VALUES (?,?,'IN_APP',?,'SIMULATED',CURRENT_TIMESTAMP,?)",
      customer,
      appt,
      message,
      "EVENT_" + Security.token().substring(0, 12)
    );
  }

  Map<String, Object> appointment(long appt) throws SQLException {
    var a = one(c, "SELECT * FROM appointment WHERE id=?", appt);
    if (role("CUSTOMER")) {
      if (id(a, "customer_id") != ownCustomer()) throw new Forbidden(
        "Lịch không thuộc bạn"
      );
    } else if (role("TECHNICIAN")) {
      if (
        num(
          one(
            c,
            "SELECT COUNT(*) n FROM appointment_item WHERE appointment_id=? AND staff_id=?",
            appt,
            ownStaff()
          ).get("n")
        ) == 0
      ) throw new Forbidden("Lịch không phân công cho bạn");
    } else require("ADMIN", "MANAGER", "RECEPTIONIST");
    return a;
  }

  void changePolicy(Map<String, Object> appt, Map<String, Object> a) {
    if (!str(appt, "status").equals("CONFIRMED")) throw bad(
      "Chỉ đổi/hủy lịch đã xác nhận"
    );
    if (
      LocalDateTime.parse(str(appt, "start_at")).isBefore(
        LocalDateTime.now().plusHours(2)
      )
    ) {
      if (
        !role("ADMIN", "MANAGER", "RECEPTIONIST") ||
        !Boolean.TRUE.equals(a.get("override"))
      ) throw bad(
        "Đổi/hủy trước ít nhất 2 giờ; lễ tân có thể ghi đè và ghi lý do"
      );
      required(a, "reason", 250);
    }
  }

  Object reschedule(Map<String, Object> a) throws SQLException {
    if (role("TECHNICIAN")) throw new Forbidden("Kỹ thuật viên không dời lịch");
    long appt = id(a, "id");
    var old = appointment(appt);
    changePolicy(old, a);
    LocalDateTime start = timestamp(a, "start_at");
    if (!start.isAfter(LocalDateTime.now())) throw bad(
      "Giờ mới phải ở tương lai"
    );
    var items = rows(
      c,
      "SELECT * FROM appointment_item WHERE appointment_id=? ORDER BY start_at,id",
      appt
    );
    List<Long> services = items
      .stream()
      .map(i -> id(i, "service_id"))
      .toList();
    var planned = plan(services, 0, id(old, "branch_id"), start, appt, items);
    checkCustomerOverlap(
      id(old, "customer_id"),
      start,
      planned.get(planned.size() - 1).end(),
      appt,
      a
    );
    for (int i = 0; i < items.size(); i++) {
      var item = planned.get(i);
      if (item.customerPackage() != null) {
        var cp = one(
          c,
          "SELECT expires_at FROM customer_package WHERE id=?",
          item.customerPackage()
        );
        if (
          LocalDateTime.parse(str(cp, "expires_at")).isBefore(start)
        ) throw bad("Gói hết hạn trước giờ mới");
      }
      execute(
        c,
        "UPDATE appointment_item SET start_at=?,end_at=?,room_id=? WHERE id=?",
        item.start(),
        item.end(),
        item.room(),
        id(items.get(i), "id")
      );
    }
    execute(
      c,
      "UPDATE appointment SET start_at=?,end_at=? WHERE id=?",
      start,
      planned.get(planned.size() - 1).end(),
      appt
    );
    notify(
      appt,
      id(old, "customer_id"),
      start,
      "Lịch #" + appt + " đã đổi sang " + start
    );
    audit("appointment.reschedule", appt);
    return Map.of("id", appt);
  }

  Object cancel(Map<String, Object> a) throws SQLException {
    if (role("TECHNICIAN")) throw new Forbidden("Kỹ thuật viên không hủy lịch");
    long appt = id(a, "id");
    var old = appointment(appt);
    changePolicy(old, a);
    String reason = required(a, "reason", 250);
    execute(
      c,
      "UPDATE appointment SET status='CANCELLED',cancel_reason=? WHERE id=?",
      reason,
      appt
    );
    execute(
      c,
      "UPDATE notification SET status='CANCELLED' WHERE appointment_id=? AND status='PENDING'",
      appt
    );
    execute(
      c,
      "INSERT INTO notification(customer_id,appointment_id,channel,message,status,due_at,kind) VALUES (?,?,'IN_APP',?,'SIMULATED',CURRENT_TIMESTAMP,?)",
      id(old, "customer_id"),
      appt,
      "Đã hủy lịch #" + appt,
      "CANCEL_" + appt
    );
    audit("appointment.cancel", appt);
    return Map.of("ok", true);
  }

  Object status(Map<String, Object> a) throws SQLException {
    long appt = id(a, "id");
    var old = appointment(appt);
    if (role("CUSTOMER")) throw new Forbidden(
      "Khách hàng không cập nhật trạng thái dịch vụ"
    );
    String from = str(old, "status"),
      to = required(a, "status", 20);
    if (
      role("TECHNICIAN") && !Set.of("IN_PROGRESS", "COMPLETED").contains(to)
    ) throw new Forbidden("Lễ tân thực hiện check-in");
    if (
      !(
        (from.equals("CONFIRMED") &&
          Set.of("CHECKED_IN", "NO_SHOW").contains(to)) ||
        (from.equals("CHECKED_IN") && to.equals("IN_PROGRESS")) ||
        (from.equals("IN_PROGRESS") && to.equals("COMPLETED"))
      )
    ) throw bad("Không thể chuyển " + from + " → " + to);
    if (
      to.equals("CHECKED_IN") &&
      !LocalDateTime.parse(str(old, "start_at"))
        .toLocalDate()
        .equals(LocalDate.now())
    ) throw bad("Chỉ check-in lịch của hôm nay");
    if (
      to.equals("NO_SHOW") &&
      LocalDateTime.parse(str(old, "end_at")).isAfter(LocalDateTime.now())
    ) throw bad("Chỉ đánh dấu vắng khi đã hết giờ hẹn");
    if (to.equals("COMPLETED")) {
      for (var used : rows(
        c,
        "SELECT sp.product_id,SUM(sp.quantity) qty FROM appointment_item i JOIN service_product sp ON sp.service_id=i.service_id WHERE i.appointment_id=? GROUP BY sp.product_id",
        appt
      ))
        adjustStock(
          id(used, "product_id"),
          -integer(used, "qty"),
          "Tiêu hao dịch vụ #" + appt,
          appt
        );
    }
    execute(c, "UPDATE appointment SET status=? WHERE id=?", to, appt);
    if (to.equals("NO_SHOW")) execute(
      c,
      "UPDATE notification SET status='CANCELLED' WHERE appointment_id=? AND status='PENDING'",
      appt
    );
    audit("appointment." + to.toLowerCase(Locale.ROOT), appt);
    return Map.of("ok", true);
  }

  @SuppressWarnings("unchecked")
  Object price(Map<String, Object> a, boolean pay) throws SQLException {
    require("ADMIN", "MANAGER", "RECEPTIONIST");
    long appt = id(a, "appointment_id");
    var appointment = appointment(appt);
    if (!str(appointment, "status").equals("COMPLETED")) throw bad(
      "Chỉ thanh toán lịch đã hoàn thành"
    );
    if (
      !rows(c, "SELECT id FROM invoice WHERE appointment_id=?", appt).isEmpty()
    ) throw bad("Lịch đã được thanh toán");
    long customer = id(appointment, "customer_id"),
      subtotal = 0,
      pkgDiscount = 0;
    var lines = rows(
      c,
      "SELECT i.*,sv.name service_name,st.full_name staff_name FROM appointment_item i JOIN service sv ON sv.id=i.service_id JOIN staff st ON st.id=i.staff_id WHERE i.appointment_id=? ORDER BY i.id",
      appt
    );
    for (var line : lines) {
      subtotal += id(line, "price_at_booking");
      if (line.get("customer_package_id") != null) pkgDiscount += id(
        line,
        "price_at_booking"
      );
    }
    var productLines = new ArrayList<Map<String, Object>>();
    Object requested = a.get("products");
    if (requested instanceof List<?> cart) {
      if (cart.size() > 30) throw bad("Tối đa 30 dòng sản phẩm");
      for (Object row : cart) {
        if (!(row instanceof Map)) throw bad("Sản phẩm không hợp lệ");
        var entry = (Map<String, Object>) row;
        long quantity = bounded(id(entry, "quantity"), 1, 1000, "Số lượng");
        var product = one(
          c,
          "SELECT * FROM product WHERE id=? AND is_active=TRUE",
          id(entry, "product_id")
        );
        var l = new LinkedHashMap<String, Object>(product);
        l.put("quantity", quantity);
        productLines.add(l);
        subtotal += id(product, "sale_price") * quantity;
      }
    }
    long base = subtotal - pkgDiscount;
    var tier = tier(customer);
    long combo = (base * id(appointment, "combo_percent")) / 100,
      promo = 0;
    Long promotionId = null;
    String code = str(a, "promotion_code").toUpperCase(Locale.ROOT);
    if (!code.isEmpty()) {
      var p = one(
        c,
        "SELECT * FROM promotion WHERE code=? AND is_active=TRUE",
        code
      );
      LocalDate today = LocalDate.now();
      if (
        today.isBefore(LocalDate.parse(str(p, "start_date"))) ||
        today.isAfter(LocalDate.parse(str(p, "end_date")))
      ) throw bad("Mã khuyến mãi hết hạn hoặc chưa bắt đầu");
      if (id(p, "used_count") >= id(p, "max_uses")) throw bad(
        "Mã đã hết lượt sử dụng"
      );
      if (base < id(p, "min_total")) throw bad(
        "Chưa đạt giá trị hóa đơn tối thiểu"
      );
      promo = (base * id(p, "discount_percent")) / 100;
      promotionId = id(p, "id");
    }
    // Một ưu đãi combo/nhóm hoặc voucher tốt nhất, sau đó giảm theo hạng.
    if (combo >= promo) {
      promo = 0;
      promotionId = null;
    } else combo = 0;
    long tierDiscount =
        ((base - combo - promo) * id(tier, "discount_percent")) / 100,
      after = base - combo - promo - tierDiscount;
    int redeem = integer(a, "redeem_points");
    if (redeem < 0 || redeem % 100 != 0) throw bad(
      "Điểm đổi là bội số của 100"
    );
    if (redeem > balance(customer)) throw bad("Không đủ điểm còn hạn");
    long pointDiscount = redeem * 100L;
    if (pointDiscount > after / 2) throw bad(
      "Điểm chỉ được giảm tối đa 50% hóa đơn sau ưu đãi"
    );
    long total = after - pointDiscount;
    double multiplier = Double.parseDouble(str(tier, "multiplier"));
    var result = new LinkedHashMap<String, Object>();
    result.put("subtotal", subtotal);
    result.put("package_discount", pkgDiscount);
    result.put("combo_discount", combo);
    result.put("promo_discount", promo);
    result.put("tier_discount", tierDiscount);
    result.put("point_discount", pointDiscount);
    result.put("total", total);
    result.put(
      "earned_points",
      (int) Math.floor((total / 10000.0) * multiplier)
    );
    result.put("tier", str(tier, "name"));
    result.put("lines", lines);
    result.put("products", productLines);
    if (!pay) return result;
    String method = method(a);
    long invoice = insert(
      c,
      "INSERT INTO invoice(appointment_id,customer_id,subtotal,package_discount,combo_discount,tier_discount,promo_discount,point_discount,total,cashier_id,description) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
      appt,
      customer,
      subtotal,
      pkgDiscount,
      combo,
      tierDiscount,
      promo,
      pointDiscount,
      total,
      uid(),
      "Dịch vụ lịch #" + appt
    );
    if (total > 0) execute(
      c,
      "INSERT INTO payment(invoice_id,method,amount) VALUES (?,?,?)",
      invoice,
      method,
      total
    );
    for (var line : lines)
      if (line.get("customer_package_id") != null) {
        if (
          execute(
            c,
            "UPDATE customer_package SET remaining_sessions=remaining_sessions-1 WHERE id=? AND remaining_sessions>0",
            id(line, "customer_package_id")
          ) == 0
        ) throw bad("Gói không còn buổi");
      }
    for (var product : productLines) {
      int qty = integer(product, "quantity");
      adjustStock(id(product, "id"), -qty, "Bán lẻ hóa đơn #" + invoice, null);
      execute(
        c,
        "INSERT INTO invoice_product(invoice_id,product_id,quantity,unit_price) VALUES (?,?,?,?)",
        invoice,
        id(product, "id"),
        qty,
        id(product, "sale_price")
      );
    }
    if (promotionId != null) execute(
      c,
      "UPDATE promotion SET used_count=used_count+1 WHERE id=?",
      promotionId
    );
    loyalty(customer, invoice, total, redeem, multiplier);
    var commissionBase = new LinkedHashMap<Long, Long>();
    for (var line : lines)
      if (line.get("customer_package_id") == null) commissionBase.merge(
        id(line, "staff_id"),
        id(line, "price_at_booking"),
        Long::sum
      );
    if (!productLines.isEmpty() && id(a, "retail_staff_id") > 0) {
      long seller = id(a, "retail_staff_id");
      one(c, "SELECT id FROM staff WHERE id=? AND is_active=TRUE", seller);
      long productBase = productLines
        .stream()
        .mapToLong(p -> id(p, "sale_price") * id(p, "quantity"))
        .sum();
      commissionBase.merge(seller, productBase, Long::sum);
    }
    for (var entry : commissionBase.entrySet()) {
      long rate = id(
        one(
          c,
          "SELECT commission_percent FROM staff WHERE id=?",
          entry.getKey()
        ),
        "commission_percent"
      );
      long earned =
        base == 0
          ? 0
          : (long) Math.floor(
              (((entry.getValue() * (double) total) / base) * rate) / 100
            );
      execute(
        c,
        "INSERT INTO commission(staff_id,invoice_id,amount) VALUES (?,?,?)",
        entry.getKey(),
        invoice,
        earned
      );
    }
    audit("invoice.pay", invoice);
    result.put("invoice_id", invoice);
    result.put("balance", balance(customer));
    result.put("new_tier", str(tier(customer), "name"));
    return result;
  }

  Object savePackage(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long id = id(a, "id");
    String name = required(a, "name", 100);
    bounded(id(a, "total_sessions"), 1, 1000, "Số buổi");
    bounded(id(a, "price"), 0, 1000000000, "Giá");
    bounded(id(a, "valid_days"), 1, 3650, "Ngày hiệu lực");
    Object[] v = {
      name,
      id(a, "service_id"),
      id(a, "total_sessions"),
      id(a, "price"),
      id(a, "valid_days"),
      bool(a, "is_active"),
    };
    if (id == 0) id = insert(
      c,
      "INSERT INTO treatment_package(name,service_id,total_sessions,price,valid_days,is_active) VALUES (?,?,?,?,?,?)",
      v
    );
    else {
      if (
        !rows(
          c,
          "SELECT id FROM customer_package WHERE package_id=?",
          id
        ).isEmpty()
      ) throw bad("Gói đã bán: tạo gói mới để giữ nguyên quyền lợi cũ");
      var all = Arrays.copyOf(v, 7);
      all[6] = id;
      if (
        execute(
          c,
          "UPDATE treatment_package SET name=?,service_id=?,total_sessions=?,price=?,valid_days=?,is_active=? WHERE id=?",
          all
        ) == 0
      ) throw bad("Không tìm thấy gói");
    }
    audit("package.save", id);
    return Map.of("id", id);
  }

  Object sellPackage(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER", "RECEPTIONIST");
    long customer = id(a, "customer_id");
    one(c, "SELECT id FROM customer WHERE id=? AND is_active=TRUE", customer);
    var pkg = one(
      c,
      "SELECT * FROM treatment_package WHERE id=? AND is_active=TRUE",
      id(a, "package_id")
    );
    String method = method(a);
    double mult = Double.parseDouble(str(tier(customer), "multiplier"));
    long price = id(pkg, "price");
    long invoice = insert(
      c,
      "INSERT INTO invoice(customer_id,subtotal,total,cashier_id,description) VALUES (?,?,?,?,?)",
      customer,
      price,
      price,
      uid(),
      "Mua gói: " + str(pkg, "name")
    );
    if (price > 0) execute(
      c,
      "INSERT INTO payment(invoice_id,method,amount) VALUES (?,?,?)",
      invoice,
      method,
      price
    );
    long cp = insert(
      c,
      "INSERT INTO customer_package(customer_id,package_id,remaining_sessions,expires_at) VALUES (?,?,?,?)",
      customer,
      id(pkg, "id"),
      id(pkg, "total_sessions"),
      LocalDateTime.now().plusDays(id(pkg, "valid_days"))
    );
    loyalty(customer, invoice, price, 0, mult);
    audit("package.sell", cp);
    return Map.of("id", cp, "invoice_id", invoice);
  }

  Object saveCombo(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long id = id(a, "id");
    String name = required(a, "name", 100);
    long percent = bounded(id(a, "discount_percent"), 0, 80, "Mức giảm");
    List<Long> services = numbers(a, "service_ids");
    if (services.size() < 2) throw bad("Combo cần ít nhất 2 dịch vụ");
    if (id == 0) id = insert(
      c,
      "INSERT INTO combo(name,discount_percent,is_active) VALUES (?,?,?)",
      name,
      percent,
      bool(a, "is_active")
    );
    else {
      if (
        execute(
          c,
          "UPDATE combo SET name=?,discount_percent=?,is_active=? WHERE id=?",
          name,
          percent,
          bool(a, "is_active"),
          id
        ) == 0
      ) throw bad("Combo không tồn tại");
      execute(c, "DELETE FROM combo_item WHERE combo_id=?", id);
    }
    for (long service : services)
      execute(c, "INSERT INTO combo_item VALUES (?,?)", id, service);
    audit("combo.save", id);
    return Map.of("id", id);
  }

  Object savePromotion(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long id = id(a, "id");
    String code = required(a, "code", 40).toUpperCase(Locale.ROOT);
    if (!code.matches("[A-Z0-9_-]{3,40}")) throw bad(
      "Mã gồm 3–40 ký tự A–Z, số, _ hoặc -"
    );
    long percent = bounded(id(a, "discount_percent"), 1, 80, "Mức giảm"),
      max = bounded(id(a, "max_uses"), 1, 1000000, "Số lượt"),
      minimum = bounded(id(a, "min_total"), 0, 1000000000, "Tối thiểu");
    LocalDate start = date(a, "start_date"),
      end = date(a, "end_date");
    if (end.isBefore(start)) throw bad("Khoảng ngày khuyến mãi không hợp lệ");
    Object[] v = {
      code,
      percent,
      start,
      end,
      max,
      minimum,
      bool(a, "is_active"),
    };
    if (id == 0) id = insert(
      c,
      "INSERT INTO promotion(code,discount_percent,start_date,end_date,max_uses,min_total,is_active) VALUES (?,?,?,?,?,?,?)",
      v
    );
    else {
      long used = id(
        one(c, "SELECT used_count FROM promotion WHERE id=?", id),
        "used_count"
      );
      if (max < used) throw bad("Số lượt tối đa phải >= số lượt đã dùng");
      var all = Arrays.copyOf(v, 8);
      all[7] = id;
      execute(
        c,
        "UPDATE promotion SET code=?,discount_percent=?,start_date=?,end_date=?,max_uses=?,min_total=?,is_active=? WHERE id=?",
        all
      );
    }
    audit("promotion.save", id);
    return Map.of("id", id);
  }

  Object saveTier(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long id = id(a, "id");
    String name = required(a, "name", 25);
    long minimum = bounded(id(a, "min_spending"), 0, 1000000000, "Ngưỡng hạng"),
      discount = bounded(id(a, "discount_percent"), 0, 50, "Mức giảm");
    double mult = Double.parseDouble(str(a, "multiplier"));
    if (!Double.isFinite(mult) || mult < 1 || mult > 10) throw bad(
      "Hệ số điểm từ 1 đến 10"
    );
    if (id == 1 && minimum != 0) throw bad("Hạng cơ bản phải có ngưỡng 0");
    if (id == 0) id = insert(
      c,
      "INSERT INTO tier(name,min_spending,discount_percent,multiplier) VALUES (?,?,?,?)",
      name,
      minimum,
      discount,
      mult
    );
    else if (
      execute(
        c,
        "UPDATE tier SET name=?,min_spending=?,discount_percent=?,multiplier=? WHERE id=?",
        name,
        minimum,
        discount,
        mult,
        id
      ) == 0
    ) throw bad("Không tìm thấy hạng");
    audit("tier.save", id);
    return Map.of("id", id);
  }

  Object saveProduct(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long id = id(a, "id");
    Object[] v = {
      required(a, "name", 100),
      required(a, "sku", 40),
      required(a, "unit", 20),
      bounded(id(a, "sale_price"), 0, 1000000000, "Giá"),
      bounded(id(a, "reorder_level"), 0, 1000000, "Mức cảnh báo"),
      bool(a, "is_active"),
    };
    if (id == 0) id = insert(
      c,
      "INSERT INTO product(name,sku,unit,sale_price,reorder_level,is_active) VALUES (?,?,?,?,?,?)",
      v
    );
    else {
      var all = Arrays.copyOf(v, 7);
      all[6] = id;
      if (
        execute(
          c,
          "UPDATE product SET name=?,sku=?,unit=?,sale_price=?,reorder_level=?,is_active=? WHERE id=?",
          all
        ) == 0
      ) throw bad("Sản phẩm không tồn tại");
    }
    audit("product.save", id);
    return Map.of("id", id);
  }

  void adjustStock(long product, int quantity, String reason, Long appt)
    throws SQLException {
    if (
      execute(
        c,
        "UPDATE product SET stock_qty=stock_qty+? WHERE id=? AND stock_qty+?>=0",
        quantity,
        product,
        quantity
      ) == 0
    ) throw bad("Kho không đủ cho sản phẩm #" + product);
    execute(
      c,
      "INSERT INTO stock_movement(product_id,quantity,reason,appointment_id,created_by) VALUES (?,?,?,?,?)",
      product,
      quantity,
      reason,
      appt,
      uid()
    );
  }

  Object stock(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    int qty = (int) bounded(
      id(a, "quantity"),
      -1000000,
      1000000,
      "Số lượng điều chỉnh"
    );
    if (qty == 0) throw bad("Số lượng phải khác 0");
    adjustStock(id(a, "product_id"), qty, required(a, "reason", 250), null);
    audit("stock.adjust", id(a, "product_id"));
    return Map.of("ok", true);
  }

  Object recipe(Map<String, Object> a) throws SQLException {
    require("ADMIN", "MANAGER");
    long service = id(a, "service_id"),
      product = id(a, "product_id");
    int qty = (int) bounded(id(a, "quantity"), 0, 1000000, "Định mức");
    execute(
      c,
      "DELETE FROM service_product WHERE service_id=? AND product_id=?",
      service,
      product
    );
    if (qty > 0) execute(
      c,
      "INSERT INTO service_product VALUES (?,?,?)",
      service,
      product,
      qty
    );
    audit("recipe.save", service);
    return Map.of("ok", true);
  }

  Object review(Map<String, Object> a) throws SQLException {
    require("CUSTOMER");
    long appt = id(a, "appointment_id");
    var old = appointment(appt);
    if (!str(old, "status").equals("COMPLETED")) throw bad(
      "Chỉ đánh giá sau khi hoàn thành"
    );
    int rating = (int) bounded(id(a, "rating"), 1, 5, "Số sao");
    String comment = str(a, "comment");
    if (comment.length() > 1000) throw bad("Nhận xét tối đa 1000 ký tự");
    long review = insert(
      c,
      "INSERT INTO review(appointment_id,customer_id,rating,comment) VALUES (?,?,?,?)",
      appt,
      ownCustomer(),
      rating,
      comment
    );
    audit("review.save", review);
    return Map.of("id", review);
  }

  Object dispatch() throws SQLException {
    require("ADMIN", "MANAGER", "RECEPTIONIST");
    return Map.of(
      "count",
      execute(
        c,
        "UPDATE notification SET status='SIMULATED',sent_at=CURRENT_TIMESTAMP WHERE status='PENDING' AND due_at<=CURRENT_TIMESTAMP"
      )
    );
  }

  Object saveAccount(Map<String, Object> a) throws Exception {
    require("ADMIN");
    long id = id(a, "id");
    String role = required(a, "role", 25);
    if (
      !Set.of(
        "ADMIN",
        "MANAGER",
        "RECEPTIONIST",
        "TECHNICIAN",
        "CUSTOMER"
      ).contains(role)
    ) throw bad("Vai trò không hợp lệ");
    String name = required(a, "full_name", 100);
    boolean active = bool(a, "is_active");
    if (id == uid() && (!active || !role.equals("ADMIN"))) throw bad(
      "Không thể tự khóa hoặc hạ quyền admin đang đăng nhập"
    );
    if (id == 0) {
      String username = required(a, "username", 50),
        pw = required(a, "password", 100);
      if (
        !username.matches("[A-Za-z0-9_.-]{3,50}") || pw.length() < 8
      ) throw bad("Tài khoản >=3 ký tự, mật khẩu >=8 ký tự");
      if (Set.of("CUSTOMER", "TECHNICIAN").contains(role)) throw bad(
        "Tạo khách qua Đăng ký, kỹ thuật viên qua Nhân viên để có liên kết hồ sơ"
      );
      id = insert(
        c,
        "INSERT INTO app_user(username,password_hash,role,full_name,is_active) VALUES (?,?,?,?,?)",
        username,
        Security.hash(pw),
        role,
        name,
        active
      );
    } else {
      var old = one(c, "SELECT role FROM app_user WHERE id=?", id);
      if (
        !str(old, "role").equals(role) &&
        (Set.of("CUSTOMER", "TECHNICIAN").contains(role) ||
          Set.of("CUSTOMER", "TECHNICIAN").contains(str(old, "role")))
      ) throw bad(
        "Không đổi vai trò của tài khoản gắn hồ sơ khách/kỹ thuật viên"
      );
      execute(
        c,
        "UPDATE app_user SET role=?,full_name=?,is_active=?,failed_attempts=0,locked_until=NULL WHERE id=?",
        role,
        name,
        active,
        id
      );
      String pw = str(a, "password");
      if (!pw.isEmpty()) {
        if (pw.length() < 8) throw bad("Mật khẩu tối thiểu 8 ký tự");
        execute(
          c,
          "UPDATE app_user SET password_hash=? WHERE id=?",
          Security.hash(pw),
          id
        );
      }
    }
    audit("account.save", id);
    return Map.of("id", id);
  }

  Object password(Map<String, Object> a) throws Exception {
    String old = required(a, "old_password", 100),
      next = required(a, "new_password", 100);
    if (next.length() < 8) throw bad("Mật khẩu mới tối thiểu 8 ký tự");
    var account = one(
      c,
      "SELECT password_hash FROM app_user WHERE id=?",
      uid()
    );
    if (!Security.verify(old, str(account, "password_hash"))) throw bad(
      "Mật khẩu hiện tại không đúng"
    );
    execute(
      c,
      "UPDATE app_user SET password_hash=? WHERE id=?",
      Security.hash(next),
      uid()
    );
    audit("password.change", uid());
    return Map.of("ok", true);
  }

  public Object state(Map<String, Object> filter) throws SQLException {
    var out = new LinkedHashMap<String, Object>();
    boolean manage = role("ADMIN", "MANAGER"),
      desk = role("ADMIN", "MANAGER", "RECEPTIONIST"),
      customer = role("CUSTOMER"),
      tech = role("TECHNICIAN");
    long own = customer ? ownCustomer() : tech ? ownStaff() : 0;
    out.put("user", user);
    out.put("branches", rows(c, "SELECT * FROM branch ORDER BY id"));
    out.put(
      "services",
      rows(
        c,
        "SELECT * FROM service" +
          (manage ? "" : " WHERE is_active=TRUE") +
          " ORDER BY id"
      )
    );
    out.put("tiers", rows(c, "SELECT * FROM tier ORDER BY min_spending"));
    out.put(
      "staff",
      rows(
        c,
        "SELECT id,full_name,branch_id,is_active" +
          (manage ? ",phone,commission_percent,user_id" : "") +
          " FROM staff" +
          (manage ? "" : " WHERE is_active=TRUE") +
          " ORDER BY id"
      )
    );
    out.put("skills", rows(c, "SELECT * FROM staff_skill"));
    out.put("rooms", rows(c, "SELECT * FROM room ORDER BY id"));
    String appointments =
      "SELECT a.*,cu.full_name customer_name,cu.phone,br.name branch_name,v.id invoice_id,v.total invoice_total FROM appointment a JOIN customer cu ON cu.id=a.customer_id JOIN branch br ON br.id=a.branch_id LEFT JOIN invoice v ON v.appointment_id=a.id";
    if (customer) appointments += " WHERE a.customer_id=" + own;
    if (tech) appointments +=
      " WHERE EXISTS(SELECT 1 FROM appointment_item i WHERE i.appointment_id=a.id AND i.staff_id=" +
      own +
      ")";
    out.put(
      "appointments",
      rows(c, appointments + " ORDER BY a.start_at DESC LIMIT 1000")
    );
    String itemFilter = customer
      ? " WHERE a.customer_id=" + own
      : tech
        ? " WHERE EXISTS(SELECT 1 FROM appointment_item own WHERE own.appointment_id=a.id AND own.staff_id=" +
          own +
          ")"
        : "";
    out.put(
      "items",
      rows(
        c,
        "SELECT i.*,sv.name service_name,st.full_name staff_name,r.name room_name FROM appointment_item i JOIN appointment a ON a.id=i.appointment_id JOIN service sv ON sv.id=i.service_id JOIN staff st ON st.id=i.staff_id JOIN room r ON r.id=i.room_id" +
          itemFilter +
          " ORDER BY i.id DESC LIMIT 3000"
      )
    );
    var customers = desk
      ? rows(c, "SELECT * FROM customer ORDER BY id DESC LIMIT 1000")
      : customer
        ? rows(c, "SELECT * FROM customer WHERE id=?", own)
        : new ArrayList<Map<String, Object>>();
    for (var cu : customers) {
      long cid = id(cu, "id");
      cu.put("point_balance", balance(cid));
      cu.put("spending_12m", spending(cid));
      cu.put("tier", tier(cid).get("name"));
    }
    out.put("customers", customers);
    out.put(
      "combos",
      rows(
        c,
        "SELECT * FROM combo" +
          (manage ? "" : " WHERE is_active=TRUE") +
          " ORDER BY id"
      )
    );
    out.put("combo_items", rows(c, "SELECT * FROM combo_item"));
    out.put(
      "packages",
      rows(
        c,
        "SELECT p.*,sv.name service_name FROM treatment_package p JOIN service sv ON sv.id=p.service_id" +
          (manage ? "" : " WHERE p.is_active=TRUE") +
          " ORDER BY p.id"
      )
    );
    var customerPackages =
      desk || customer
        ? rows(
            c,
            "SELECT cp.*,p.name,p.service_id,p.total_sessions,cu.full_name customer_name FROM customer_package cp JOIN treatment_package p ON p.id=cp.package_id JOIN customer cu ON cu.id=cp.customer_id" +
              (customer ? " WHERE cp.customer_id=" + own : "") +
              " ORDER BY cp.id DESC"
          )
        : new ArrayList<Map<String, Object>>();
    for (var cp : customerPackages)
      cp.put("available_sessions", availablePackage(id(cp, "id")));
    out.put("customer_packages", customerPackages);
    out.put(
      "invoices",
      desk || customer
        ? rows(
            c,
            "SELECT v.*,cu.full_name customer_name FROM invoice v JOIN customer cu ON cu.id=v.customer_id" +
              (customer ? " WHERE v.customer_id=" + own : "") +
              " ORDER BY v.id DESC LIMIT 1000"
          )
        : List.of()
    );
    out.put(
      "invoice_products",
      desk || customer
        ? rows(
            c,
            "SELECT ip.*,p.name product_name FROM invoice_product ip JOIN product p ON p.id=ip.product_id JOIN invoice v ON v.id=ip.invoice_id" +
              (customer ? " WHERE v.customer_id=" + own : "") +
              " ORDER BY ip.id DESC LIMIT 2000"
          )
        : List.of()
    );
    out.put(
      "loyalty",
      desk || customer
        ? rows(
            c,
            "SELECT e.*,cu.full_name customer_name FROM loyalty_entry e JOIN customer cu ON cu.id=e.customer_id" +
              (customer ? " WHERE e.customer_id=" + own : "") +
              " ORDER BY e.id DESC LIMIT 1000"
          )
        : List.of()
    );
    out.put(
      "products",
      desk || manage ? rows(c, "SELECT * FROM product ORDER BY id") : List.of()
    );
    out.put(
      "recipes",
      manage
        ? rows(
            c,
            "SELECT sp.*,sv.name service_name,p.name product_name,p.unit FROM service_product sp JOIN service sv ON sv.id=sp.service_id JOIN product p ON p.id=sp.product_id"
          )
        : List.of()
    );
    out.put(
      "stock_movements",
      manage
        ? rows(
            c,
            "SELECT m.*,p.name product_name FROM stock_movement m JOIN product p ON p.id=m.product_id ORDER BY m.id DESC LIMIT 300"
          )
        : List.of()
    );
    out.put(
      "promotions",
      desk ? rows(c, "SELECT * FROM promotion ORDER BY id DESC") : List.of()
    );
    out.put(
      "shifts",
      manage || tech
        ? rows(
            c,
            "SELECT w.*,st.full_name staff_name FROM work_shift w JOIN staff st ON st.id=w.staff_id" +
              (tech ? " WHERE w.staff_id=" + own : "") +
              " ORDER BY w.work_date DESC,w.start_time LIMIT 1000"
          )
        : List.of()
    );
    out.put(
      "attendance",
      manage || tech
        ? rows(
            c,
            "SELECT at.*,st.full_name staff_name FROM attendance at JOIN staff st ON st.id=at.staff_id" +
              (tech ? " WHERE at.staff_id=" + own : "") +
              " ORDER BY at.work_date DESC LIMIT 300"
          )
        : List.of()
    );
    out.put(
      "commissions",
      manage || tech
        ? rows(
            c,
            "SELECT co.*,st.full_name staff_name FROM commission co JOIN staff st ON st.id=co.staff_id" +
              (tech ? " WHERE co.staff_id=" + own : "") +
              " ORDER BY co.id DESC LIMIT 300"
          )
        : List.of()
    );
    out.put(
      "reviews",
      rows(
        c,
        "SELECT r.*,cu.full_name customer_name FROM review r JOIN customer cu ON cu.id=r.customer_id" +
          (customer
            ? " WHERE r.customer_id=" + own
            : tech
              ? " WHERE EXISTS(SELECT 1 FROM appointment_item i WHERE i.appointment_id=r.appointment_id AND i.staff_id=" +
                own +
                ")"
              : "") +
          " ORDER BY r.id DESC LIMIT 300"
      )
    );
    out.put(
      "notifications",
      desk || customer
        ? rows(
            c,
            "SELECT n.*,cu.full_name customer_name FROM notification n JOIN customer cu ON cu.id=n.customer_id" +
              (customer ? " WHERE n.customer_id=" + own : "") +
              " ORDER BY n.id DESC LIMIT 300"
          )
        : List.of()
    );
    out.put(
      "accounts",
      role("ADMIN")
        ? rows(
            c,
            "SELECT id,username,role,full_name,is_active,failed_attempts,locked_until FROM app_user ORDER BY id"
          )
        : List.of()
    );
    out.put(
      "audit",
      manage
        ? rows(
            c,
            "SELECT a.*,COALESCE(u.full_name,'Hệ thống') user_name FROM audit_log a LEFT JOIN app_user u ON u.id=a.user_id ORDER BY a.id DESC LIMIT 200"
          )
        : List.of()
    );
    if (manage) {
      LocalDate from = str(filter, "from").isBlank()
          ? LocalDate.now().withDayOfMonth(1)
          : date(filter, "from"),
        to = str(filter, "to").isBlank() ? LocalDate.now() : date(filter, "to");
      if (
        to.isBefore(from) ||
        Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() > 3660
      ) throw bad("Khoảng báo cáo không hợp lệ");
      Object start = from.atStartOfDay(),
        end = to.plusDays(1).atStartOfDay();
      out.put(
        "revenue",
        rows(
          c,
          "SELECT CAST(created_at AS DATE) AS \"day\",COUNT(*) invoices,SUM(total) revenue FROM invoice WHERE status='PAID' AND created_at>=? AND created_at<? GROUP BY CAST(created_at AS DATE) ORDER BY 1",
          start,
          end
        )
      );
      out.put(
        "top_services",
        rows(
          c,
          "SELECT sv.name,SUM(i.price_at_booking) gross,COUNT(*) quantity FROM appointment_item i JOIN service sv ON sv.id=i.service_id JOIN invoice v ON v.appointment_id=i.appointment_id WHERE v.status='PAID' AND v.created_at>=? AND v.created_at<? GROUP BY sv.name ORDER BY quantity DESC",
          start,
          end
        )
      );
      out.put(
        "report_stats",
        one(
          c,
          "SELECT COUNT(*) appointments,COALESCE(SUM(CASE WHEN status='NO_SHOW' THEN 1 ELSE 0 END),0) no_shows,COALESCE(SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END),0) completed FROM appointment WHERE start_at>=? AND start_at<? AND status<>'CANCELLED'",
          start,
          end
        )
      );
      out.put(
        "new_customers",
        one(
          c,
          "SELECT COUNT(*) n FROM customer WHERE created_at>=? AND created_at<?",
          start,
          end
        ).get("n")
      );
      var loyal = new ArrayList<Map<String, Object>>(customers);
      loyal.sort((a, b) ->
        Long.compare(id(b, "spending_12m"), id(a, "spending_12m"))
      );
      out.put("loyal_customers", loyal.stream().limit(10).toList());
      out.put(
        "staff_report",
        rows(
          c,
          "SELECT st.full_name,COUNT(co.id) paid_jobs,COALESCE(SUM(co.amount),0) commission FROM staff st LEFT JOIN commission co ON co.staff_id=st.id AND co.created_at>=? AND co.created_at<? GROUP BY st.id,st.full_name ORDER BY commission DESC",
          start,
          end
        )
      );
    }
    out.put("server_time", LocalDateTime.now().toString());
    return out;
  }
}
