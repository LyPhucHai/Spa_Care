"use strict";
const $ = (s, r = document) => r.querySelector(s),
  $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = (v) =>
  String(v ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const money = (n) => Number(n || 0).toLocaleString("vi-VN") + " ₫";
const int = (n) => Number(n || 0).toLocaleString("vi-VN");
const today = () =>
  new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Ho_Chi_Minh" });
const dateStr = (s) =>
  s
    ? new Date(String(s).slice(0, 10) + "T12:00").toLocaleDateString("vi-VN")
    : "—";
const time = (s) => String(s || "").slice(11, 16);
const fmt = (s) => (s ? `${time(s)} · ${dateStr(s)}` : "—");
const roles = {
  ADMIN: "Quản trị viên",
  MANAGER: "Quản lý",
  RECEPTIONIST: "Lễ tân",
  TECHNICIAN: "Kỹ thuật viên",
  CUSTOMER: "Khách hàng",
};
const labels = {
  CONFIRMED: "Đã xác nhận",
  CHECKED_IN: "Đã check-in",
  IN_PROGRESS: "Đang thực hiện",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
  NO_SHOW: "Vắng mặt",
  PAID: "Đã thanh toán",
  PENDING: "Đang chờ",
  SIMULATED: "Đã mô phỏng gửi",
  SILVER: "Silver",
  GOLD: "Gold",
  PLATINUM: "Platinum",
};
const badge = (s) =>
  `<span class="badge ${esc(s)}">${esc(labels[s] || s)}</span>`;
const paths = {
  leaf: "M12 21V10m0 7C3 17 2 11 3 4c7 0 10 5 9 10m0 0c0-8 4-11 10-11 1 7-3 12-10 12",
  dashboard: "M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z",
  calendar: "M4 5h16v16H4zM7 3v4m10-4v4M4 10h16m-12 4h2m4 0h2m-8 4h2",
  users:
    "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2m18 0v-2a4 4 0 0 0-3-4M8.5 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8m8 0a4 4 0 0 1 0 8",
  staff: "M8 4h8v5H8zM4 9h16v12H4zM9 14h6m-3-3v6",
  wallet: "M3 5h16v4H3zm0 4v12h18V9H3m14 4h4v4h-4z",
  box: "m12 3 9 5-9 5-9-5 9-5zm-9 5v10l9 5 9-5V8M12 13v10",
  chart: "M4 3v17h17M8 16v-5m5 5V7m5 9V3",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-12 9h12m-8 4h4",
  logout: "M9 4H3v16h6m5-13 5 5-5 5M8 12h13",
  plus: "M12 5v14M5 12h14",
  search: "M21 21l-5-5M10 3a7 7 0 1 0 0 14 7 7 0 0 0 0-14",
  arrow: "M5 12h14m-5-5 5 5-5 5",
  check: "m4 12 5 5L20 6",
  clock: "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18m0 4v6h4",
  gift: "M3 8h18v4H3zm2 4v9h14v-9M12 8v13m0-13C4 8 5 1 9 3l3 5m0 0c8 0 7-7 3-5l-3 5",
  star: "m12 2 3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1z",
  settings:
    "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8m0-6v3m0 14v3M2 12h3m14 0h3M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2",
  menu: "M3 6h18M3 12h18M3 18h18",
  download: "M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5",
  edit: "m15 4 5 5-11 11-6 1 1-6L15 4zm-2 2 5 5",
  shield: "m12 2 8 4v6c0 6-8 10-8 10S4 18 4 12V6l8-4z",
};
const icon = (n, cls = "") =>
  `<svg class="icon ${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${paths[n] || paths.leaf}"/></svg>`;
const logo = () =>
  `<div class="logo"><span class="logo-mark">${icon("leaf")}</span>SpaCare<span style="color:#b9cd9e;font-size:23px">.</span></div>`;
const empty = (s = "Chưa có dữ liệu. Hãy tạo bản ghi đầu tiên.") =>
  `<div class="empty">${icon("leaf")}<p>${esc(s)}</p></div>`;
const btn = (text, action, id = "", type = "") =>
  `<button class="btn ${type}" data-action="${action}" data-id="${esc(id)}">${esc(text)}</button>`;
const plus = (text, action) =>
  `<button class="btn primary" data-action="${action}">${icon("plus")}${esc(text)}</button>`;
const panel = (title, body, aside = "", sub = "") =>
  `<section class="panel"><div class="panel-head"><div><h2>${title}</h2>${sub ? `<p>${sub}</p>` : ""}</div>${aside}</div>${body}</section>`;
const table = (heads, rows) =>
  rows.length
    ? `<div class="table-wrap"><table><thead><tr>${heads.map((h) => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows.map((r) => `<tr>${r.map((cell) => `<td>${cell}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`
    : empty();
const kpi = (title, value, foot, symbol) =>
  `<div class="kpi"><div class="kpi-top">${title}<span class="symbol">${icon(symbol)}</span></div><div class="kpi-value">${value}</div><div class="kpi-foot">${foot}</div></div>`;
let csrf = "",
  user = null,
  data = {},
  page = "dashboard",
  tab = "",
  search = "",
  appointmentDate = "",
  appointmentStatus = "",
  branchFilter = "",
  reportFrom = "",
  reportTo = "",
  busy = false,
  registerMode = false,
  lastFocus;
const is = (...r) => r.includes(user?.role),
  manager = () => is("ADMIN", "MANAGER"),
  desk = () => is("ADMIN", "MANAGER", "RECEPTIONIST");
const options = (rows, key = "name", value = "id") =>
  rows.map((r) => [r[value], r[key]]);
const serviceOptions = () =>
  data.services
    .filter((r) => r.is_active)
    .map((r) => [r.id, `${r.name} · ${money(r.price)} · ${r.duration_min}p`]);
const customerOptions = () =>
  data.customers
    .filter((r) => r.is_active)
    .map((r) => [r.id, `${r.full_name} · ${r.phone}`]);
const staffOptions = () =>
  data.staff.filter((r) => r.is_active).map((r) => [r.id, r.full_name]);
const productOptions = () =>
  data.products
    .filter((r) => r.is_active)
    .map((r) => [r.id, `${r.name} · còn ${r.stock_qty} ${r.unit}`]);
async function api(path, body) {
  const res = await fetch("/api/" + path, {
    method: body ? "POST" : "GET",
    headers: body
      ? { "Content-Type": "application/json", "X-CSRF-Token": csrf }
      : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const v = await res.json();
  if (!res.ok) {
    if (res.status === 401 && user) {
      user = null;
      renderAuth();
    }
    throw Error(v.error || "Không thực hiện được yêu cầu");
  }
  return v;
}
async function action(name, args = {}) {
  return api("action", { ...args, action: name });
}
function toast(text, error = false) {
  const t = $("#toast");
  t.textContent = text;
  t.className = "show" + (error ? " error" : "");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => (t.className = ""), 5000);
}
function filtered(rows, keys) {
  const q = search.toLocaleLowerCase("vi");
  return rows.filter((r) =>
    keys.some((k) =>
      String(r[k] ?? "")
        .toLocaleLowerCase("vi")
        .includes(q),
    ),
  );
}
async function refresh() {
  const qs = new URLSearchParams();
  if (reportFrom) qs.set("from", reportFrom);
  if (reportTo) qs.set("to", reportTo);
  data = await api("state?" + qs);
  user = data.user;
  render();
}
function pageHead(title, description, actions = "") {
  return `<div class="page-title"><div><div class="eyebrow">SPACARE MANAGEMENT</div><h1>${title}</h1><p class="muted">${description}</p></div><div class="actions">${actions}</div></div>`;
}
const navEntries = () => [
  ["dashboard", "dashboard", "Tổng quan"],
  [
    "appointments",
    "calendar",
    is("CUSTOMER") ? "Lịch hẹn của tôi" : "Lịch hẹn",
  ],
  ["services", "leaf", "Dịch vụ"],
  ...(desk() ? [["customers", "users", "Khách hàng"]] : []),
  ...(manager() || is("TECHNICIAN")
    ? [
        [
          "staff",
          "staff",
          is("TECHNICIAN") ? "Ca làm & thu nhập" : "Nhân viên & ca làm",
        ],
      ]
    : []),
  ...(manager() ? [["facilities", "box", "Chi nhánh & phòng"]] : []),
  ["packages", "gift", "Combo & liệu trình"],
  ...(desk() || is("CUSTOMER")
    ? [
        [
          "invoices",
          "wallet",
          is("CUSTOMER") ? "Hóa đơn & điểm" : "Thanh toán & hóa đơn",
        ],
      ]
    : []),
  ...(manager()
    ? [
        ["promotions", "star", "Ưu đãi & thành viên"],
        ["inventory", "box", "Kho & vật tư"],
        ["reports", "chart", "Báo cáo"],
      ]
    : []),
  ...(desk() || is("CUSTOMER") ? [["notifications", "bell", "Thông báo"]] : []),
  ["reviews", "star", "Đánh giá"],
  ...(is("ADMIN") ? [["accounts", "shield", "Tài khoản"]] : []),
  ["settings", "settings", "Hồ sơ & cài đặt"],
];
function render() {
  if (!user) return renderAuth();
  if (!navEntries().some((n) => n[0] === page)) page = "dashboard";
  const current = navEntries().find((n) => n[0] === page);
  $("#app").innerHTML =
    `<aside class="sidebar">${logo()}<div class="eyebrow">KHÔNG GIAN LÀM VIỆC</div><nav class="nav">${navEntries()
      .map(
        ([id, i, title]) =>
          `<button data-nav="${id}" class="${page === id ? "active" : ""}">${icon(i)}${title}</button>`,
      )
      .join(
        "",
      )}</nav><div class="side-bottom"><div class="userbox"><span class="avatar">${esc(user.full_name.slice(0, 1))}</span><div class="user-info"><strong>${esc(user.full_name)}</strong><span>${roles[user.role]}</span></div><button class="logout" data-action="logout" aria-label="Đăng xuất">${icon("logout")}</button></div></div></aside><div class="workspace"><header class="topbar"><button class="icon-btn mobile-toggle" data-action="mobile" aria-label="Mở menu">${icon("menu")}</button><div class="breadcrumb">Không gian làm việc <span>/</span><strong>${current[2]}</strong></div><div class="top-right"><span class="date">${new Date().toLocaleDateString("vi-VN", { weekday: "long", day: "numeric", month: "long", year: "numeric", timeZone: "Asia/Ho_Chi_Minh" })}</span><button class="icon-btn" data-action="refresh" aria-label="Cập nhật dữ liệu">${icon("clock")}</button><button class="icon-btn" data-nav="notifications" aria-label="Thông báo">${icon("bell")}<i class="notification-dot"></i></button><button class="avatar" data-nav="settings" style="border:0" aria-label="Hồ sơ">${esc(user.full_name.slice(0, 1))}</button></div></header><main class="main">${pages[page]()}</main></div>`;
}
function searchbar(extra = "") {
  return `<div class="toolbar"><input class="search" id="search" aria-label="Tìm kiếm" placeholder="Tìm kiếm…" value="${esc(search)}">${extra}<span class="muted small" style="margin-left:auto">Dữ liệu được lưu tự động</span></div>`;
}
function appointmentTable(list) {
  return table(
    ["Lịch hẹn / Khách hàng", "Thời gian", "Dịch vụ", "Trạng thái", ""],
    list.map((a) => {
      const items = data.items.filter((i) => i.appointment_id === a.id);
      return [
        `<strong>${esc(a.customer_name)}</strong><span class="sub">#${a.id} · ${esc(a.branch_name)}</span>`,
        `<strong>${time(a.start_at)} – ${time(a.end_at)}</strong><span class="sub">${dateStr(a.start_at)}</span>`,
        `${items.map((i) => esc(i.service_name)).join("<br>")}<span class="sub">${[...new Set(items.map((i) => i.staff_name))].map(esc).join(", ")}</span>`,
        badge(a.status) +
          (a.invoice_id ? '<span class="sub">Đã thanh toán</span>' : ""),
        btn("Chi tiết", "appointment-detail", a.id, "sm"),
      ];
    }),
  );
}
function dashboard() {
  const appointments = data.appointments.filter(
      (a) => a.start_at.slice(0, 10) === today(),
    ),
    completed = appointments.filter((a) => a.status === "COMPLETED").length,
    paid = (data.invoices || []).filter(
      (i) => i.created_at.slice(0, 10) === today(),
    ),
    revenue = paid.reduce((s, i) => s + i.total, 0);
  const customer = is("CUSTOMER"),
    tech = is("TECHNICIAN");
  return (
    pageHead(
      "Chào " +
        esc(is("ADMIN") ? "bạn" : user.full_name.split(" ").at(-1)) +
        ",",
      customer
        ? "Chăm sóc bản thân bắt đầu từ một cuộc hẹn."
        : tech
          ? "Lịch làm việc và những khách hàng đang chờ bạn."
          : "Một ngày mới, thêm những trải nghiệm chăm sóc trọn vẹn.",
      !tech ? plus("Đặt lịch mới", "booking") : "",
    ) +
    `<section class="welcome"><div><div class="eyebrow">CARE FOR PEOPLE. GROW WITH CARE.</div><h2>${customer ? "Dành một khoảng dịu dàng cho chính mình." : "Chăm sóc tốt hơn, vận hành nhẹ nhàng hơn."}</h2><p>${customer ? "Khám phá dịch vụ, chọn khung giờ phù hợp và theo dõi hành trình chăm sóc của bạn." : "Mọi lịch hẹn, khách hàng và trải nghiệm đều được kết nối trong một không gian."}</p></div><svg class="plant" viewBox="0 0 150 120" fill="none" stroke="currentColor"><path d="M75 118V30M75 80C18 85 12 48 20 10c44 6 61 30 55 70ZM75 65c-3-44 24-65 62-62 6 43-17 67-62 62ZM75 108C30 112 19 91 24 69c33 2 50 17 51 39ZM75 97c-4-35 21-48 54-44 1 31-17 47-54 44Z"/></svg></section><div class="kpis">${kpi("Lịch hẹn hôm nay", int(appointments.length), "Đã xác nhận trong ngày", "calendar")}${kpi(customer ? "Điểm khả dụng" : tech ? "Lịch hoàn thành" : "Doanh thu hôm nay", customer ? int(data.customers[0]?.point_balance) : tech ? int(completed) : money(revenue), customer ? "Chỉ tính điểm còn hạn" : tech ? "Dịch vụ của bạn" : "Theo ngày xuất hóa đơn", "wallet")}${kpi(customer ? "Hạng thành viên" : tech ? "Hoa hồng đã ghi nhận" : "Khách hàng", customer ? esc(data.customers[0]?.tier || "SILVER") : tech ? money(data.commissions.reduce((s, r) => s + r.amount, 0)) : int(data.customers.length), customer ? "Dựa trên chi tiêu 12 tháng" : tech ? "Sau khi khách thanh toán" : "Hồ sơ đang được quản lý", "users")}${kpi("Hoàn thành hôm nay", int(completed), `${appointments.filter((a) => a.status === "IN_PROGRESS").length} lịch đang thực hiện`, "check")}</div><div class="grid-2"><div>${panel("Lịch hẹn hôm nay", appointmentTable(appointments.slice().sort((a, b) => a.start_at.localeCompare(b.start_at))), btn("Xem tất cả", "go-appointments", "", "sm"), `${appointments.length} cuộc hẹn · ${dateStr(today())}`)}</div><div>${panel(
      "Nhịp hoạt động",
      `<div class="panel-body"><div class="progress-ring" style="--progress:${appointments.length ? Math.round((completed / appointments.length) * 100) : 0}%">${appointments.length ? Math.round((completed / appointments.length) * 100) : 0}%</div><p class="muted small" style="text-align:center;margin-bottom:24px">Tỷ lệ hoàn thành lịch hôm nay</p>${[
        "CONFIRMED",
        "CHECKED_IN",
        "IN_PROGRESS",
        "COMPLETED",
      ]
        .map((status) => {
          const n = appointments.filter((a) => a.status === status).length;
          return `<div class="bar-row"><div class="bar-label"><span>${labels[status]}</span><span>${n}</span></div><div class="bar-track"><div class="bar-fill" style="width:${appointments.length ? (n / appointments.length) * 100 : 0}%"></div></div></div>`;
        })
        .join("")}</div>`,
    )}${panel(customer ? "Ưu đãi dành cho bạn" : "Thông tin vận hành", `<div class="panel-body"><div class="eyebrow">${customer ? "CHĂM SÓC ĐỀU ĐẶN" : "MỌI THỨ TRONG TẦM TAY"}</div><p style="margin-top:12px">${customer ? "Tích điểm sau mỗi hóa đơn. Đổi 100 điểm để giảm 10.000 ₫, tối đa 50% giá trị sau ưu đãi." : "Lịch chỉ được nhận khi đủ nhân viên và phòng. Kho tự trừ khi hoàn thành dịch vụ; điểm và hoa hồng ghi nhận khi thanh toán."}</p></div>`)}</div></div>`
  );
}
function appointments() {
  let list = filtered(data.appointments, ["customer_name", "phone", "id"]);
  if (appointmentDate)
    list = list.filter((a) => a.start_at.slice(0, 10) === appointmentDate);
  if (appointmentStatus)
    list = list.filter((a) => a.status === appointmentStatus);
  if (branchFilter) list = list.filter((a) => a.branch_id == branchFilter);
  return (
    pageHead(
      is("CUSTOMER") ? "Lịch hẹn của tôi" : "Lịch hẹn",
      "Theo dõi hành trình từ xác nhận đến hoàn thành.",
      !is("TECHNICIAN") ? plus("Đặt lịch mới", "booking") : "",
    ) +
    `<section class="panel">${searchbar(
      `<input type="date" id="appointment-date" aria-label="Lọc ngày" value="${esc(appointmentDate)}"><select id="appointment-status" aria-label="Lọc trạng thái"><option value="">Tất cả trạng thái</option>${Object.keys(
        labels,
      )
        .filter((s) =>
          [
            "CONFIRMED",
            "CHECKED_IN",
            "IN_PROGRESS",
            "COMPLETED",
            "CANCELLED",
            "NO_SHOW",
          ].includes(s),
        )
        .map(
          (s) =>
            `<option value="${s}" ${s === appointmentStatus ? "selected" : ""}>${labels[s]}</option>`,
        )
        .join(
          "",
        )}</select><select id="branch-filter" aria-label="Chi nhánh"><option value="">Tất cả chi nhánh</option>${data.branches.map((b) => `<option value="${b.id}" ${b.id == branchFilter ? "selected" : ""}>${esc(b.name)}</option>`).join("")}</select>`,
    )}${appointmentTable(list)}<div class="panel-foot">${list.length} lịch hẹn · Chọn Chi tiết để thực hiện thao tác</div></section>`
  );
}
function services() {
  let list = filtered(data.services, ["name", "category"]);
  return (
    pageHead(
      "Dịch vụ chăm sóc",
      "Danh mục dịch vụ, thời lượng và giá niêm yết.",
      manager() ? plus("Thêm dịch vụ", "service-new") : "",
    ) +
    `<section class="panel">${searchbar()}</section><div class="cards">${list.map((s) => `<article class="service-card ${s.is_active ? "" : "inactive"}"><div class="service-art ${esc(s.category.toLowerCase())}">${icon("leaf")}</div><div class="service-content"><div class="eyebrow">${esc(s.category)}</div><h3>${esc(s.name)}</h3><div class="price">${money(s.price)}</div><span class="muted small">${s.duration_min} phút · Phòng ${esc(s.room_type)}</span></div><div class="service-bottom"><span class="small muted">${s.is_active ? "Đang phục vụ" : "Đã ngừng bán"}</span>${manager() ? btn("Chỉnh sửa", "service-edit", s.id, "sm") : !is("TECHNICIAN") ? btn("Đặt lịch", "booking-service", s.id, "sm") : " "}</div></article>`).join("") || empty()}</div>`
  );
}
function customers() {
  const list = filtered(data.customers, ["full_name", "phone", "email"]);
  return (
    pageHead(
      "Khách hàng",
      "Hiểu khách hàng, chăm sóc đúng điều họ cần.",
      plus("Thêm khách hàng", "customer-new"),
    ) +
    panel(
      "Hồ sơ khách hàng",
      searchbar() +
        table(
          ["Khách hàng", "Liên hệ", "Hạng / Điểm", "Chi tiêu 12 tháng", ""],
          list.map((c) => [
            `<strong>${esc(c.full_name)}</strong><span class="sub">#${c.id} · ${c.birthday ? dateStr(c.birthday) : "Chưa có ngày sinh"}</span>`,
            `${esc(c.phone)}<span class="sub">${esc(c.email) || "—"}</span>`,
            `${badge(c.tier)} <span class="sub">${int(c.point_balance)} điểm</span>`,
            money(c.spending_12m),
            `<div class="actions">${btn("Hồ sơ", "customer-detail", c.id, "sm")}${btn("Sửa", "customer-edit", c.id, "sm")}</div>`,
          ]),
        ),
    )
  );
}
function tabsHtml(items) {
  return `<div class="tabs">${items.map(([id, title]) => `<button data-tab="${id}" class="${tab === id ? "active" : ""}">${title}</button>`).join("")}</div>`;
}
function staff() {
  if (!["members", "shifts", "attendance", "commissions"].includes(tab))
    tab = manager() ? "members" : "shifts";
  if (!manager() && tab === "members") tab = "shifts";
  let body = "";
  if (tab === "members")
    body = panel(
      "Đội ngũ chăm sóc",
      table(
        ["Nhân viên", "Chi nhánh / Liên hệ", "Chuyên môn", "Hoa hồng", ""],
        data.staff.map((s) => [
          `<strong>${esc(s.full_name)}</strong><span class="sub">${s.is_active ? "Đang làm việc" : "Ngừng hoạt động"}</span>`,
          `${esc(data.branches.find((b) => b.id === s.branch_id)?.name)}<span class="sub">${esc(s.phone)}</span>`,
          data.skills
            .filter((k) => k.staff_id === s.id)
            .map((k) =>
              esc(
                data.services.find((v) => v.id === k.service_id)?.name ||
                  "#" + k.service_id,
              ),
            )
            .join("<br>"),
          `${s.commission_percent}%`,
          btn("Chỉnh sửa", "staff-edit", s.id, "sm"),
        ]),
      ),
    );
  if (tab === "shifts")
    body = panel(
      "Lịch phân ca",
      table(
        ["Nhân viên", "Ngày", "Ca làm", ""],
        data.shifts.map((s) => [
          esc(s.staff_name),
          dateStr(s.work_date),
          `${esc(s.start_time.slice(0, 5))} – ${esc(s.end_time.slice(0, 5))}`,
          manager() ? btn("Xóa ca", "shift-delete", s.id, "sm danger") : "",
        ]),
      ),
    );
  if (tab === "attendance")
    body = panel(
      "Chấm công",
      table(
        ["Nhân viên", "Ngày", "Vào ca", "Ra ca"],
        data.attendance.map((a) => [
          esc(a.staff_name),
          dateStr(a.work_date),
          fmt(a.check_in),
          fmt(a.check_out),
        ]),
      ),
      btn("Chấm công vào / ra", "attendance", "", "sm primary"),
    );
  if (tab === "commissions")
    body = panel(
      "Hoa hồng đã ghi nhận",
      table(
        ["Nhân viên", "Hóa đơn", "Ngày ghi nhận", "Hoa hồng"],
        data.commissions.map((c) => [
          esc(c.staff_name),
          "#" + c.invoice_id,
          fmt(c.created_at),
          money(c.amount),
        ]),
      ),
      `<strong>${money(data.commissions.reduce((s, c) => s + c.amount, 0))}</strong>`,
    );
  return (
    pageHead(
      manager() ? "Nhân viên & ca làm" : "Ca làm & thu nhập",
      "Phân công đúng chuyên môn, theo dõi thời gian và đóng góp.",
      manager()
        ? `${btn("Phân ca", "shift-new")}${plus("Thêm nhân viên", "staff-new")}`
        : "",
    ) +
    tabsHtml([
      ...(manager() ? [["members", "Nhân viên"]] : []),
      ["shifts", "Lịch làm việc"],
      ["attendance", "Chấm công"],
      ["commissions", "Hoa hồng"],
    ]) +
    body
  );
}
function packages() {
  if (!["catalog", "combos", "owned"].includes(tab)) tab = "catalog";
  let body = "";
  if (tab === "catalog")
    body = `<div class="cards">${data.packages.map((p) => `<article class="service-card"><div class="service-content"><div class="eyebrow">LIỆU TRÌNH · ${p.total_sessions} BUỔI</div><h3>${esc(p.name)}</h3><div class="price">${money(p.price)}</div><p class="muted small">${esc(p.service_name)}<br>Hiệu lực ${p.valid_days} ngày kể từ khi mua.</p></div><div class="service-bottom"><span class="small muted">${money(p.price / p.total_sessions)} / buổi</span><div class="actions">${manager() ? btn("Sửa", "package-edit", p.id, "sm") : ""}${desk() ? btn("Bán gói", "package-sell", p.id, "sm primary") : ""}</div></div></article>`).join("")}</div>`;
  if (tab === "combos")
    body = `<div class="cards">${data.combos
      .map(
        (c) =>
          `<article class="service-card"><div class="service-content"><div class="eyebrow">COMBO · TIẾT KIỆM ${c.discount_percent}%</div><h3>${esc(c.name)}</h3><p class="muted small">${data.combo_items
            .filter((i) => i.combo_id === c.id)
            .map((i) =>
              esc(
                data.services.find((s) => s.id === i.service_id)?.name ||
                  "#" + i.service_id,
              ),
            )
            .join(
              "<br>",
            )}</p></div><div class="service-bottom"><span class="small muted">Một lần chăm sóc trọn vẹn</span>${manager() ? btn("Chỉnh sửa", "combo-edit", c.id, "sm") : !is("TECHNICIAN") ? btn("Đặt combo", "booking-combo", c.id, "sm") : ""}</div></article>`,
      )
      .join("")}</div>`;
  if (tab === "owned")
    body = panel(
      "Gói đã mua",
      table(
        ["Khách hàng / Gói", "Số buổi", "Hiệu lực", ""],
        data.customer_packages.map((p) => [
          `<strong>${esc(p.customer_name)}</strong><span class="sub">#${p.id} · ${esc(p.name)}</span>`,
          `${p.available_sessions} khả dụng / ${p.remaining_sessions} còn lại<span class="sub">Tổng ${p.total_sessions} buổi</span>`,
          dateStr(p.expires_at),
          !is("TECHNICIAN") && p.available_sessions > 0
            ? btn("Đặt buổi", "booking-package", p.id, "sm")
            : "",
        ]),
      ),
    );
  return (
    pageHead(
      "Combo & liệu trình",
      "Chăm sóc đều đặn với quyền lợi được theo dõi rõ ràng.",
      manager()
        ? `${btn("Thêm combo", "combo-new")}${plus("Thêm liệu trình", "package-new")}`
        : "",
    ) +
    tabsHtml([
      ["catalog", "Gói liệu trình"],
      ["combos", "Combo dịch vụ"],
      ...(!is("TECHNICIAN") ? [["owned", "Gói của khách"]] : []),
    ]) +
    body
  );
}
function invoices() {
  if (!["invoices", "points"].includes(tab)) tab = "invoices";
  const unpaid = data.appointments.filter(
    (a) => a.status === "COMPLETED" && !a.invoice_id,
  );
  return (
    pageHead(
      is("CUSTOMER") ? "Hóa đơn & điểm" : "Thanh toán & hóa đơn",
      "Hóa đơn minh bạch, tích điểm và ghi nhận quyền lợi tự động.",
    ) +
    (desk() && unpaid.length
      ? `<div class="note">Có ${unpaid.length} lịch đã hoàn thành chờ thanh toán. ${btn("Mở danh sách lịch", "go-appointments", "", "sm")}</div>`
      : "") +
    tabsHtml([
      ["invoices", "Hóa đơn"],
      ["points", "Lịch sử điểm"],
    ]) +
    (tab === "invoices"
      ? panel(
          "Hóa đơn đã thanh toán",
          table(
            ["Hóa đơn", "Khách hàng", "Nội dung", "Tổng tiền", "Ngày", ""],
            filtered(data.invoices, ["customer_name", "description", "id"]).map(
              (v) => [
                "#" + v.id,
                esc(v.customer_name),
                esc(v.description),
                `<strong>${money(v.total)}</strong>`,
                fmt(v.created_at),
                btn("Xem / In", "invoice-detail", v.id, "sm"),
              ],
            ),
          ),
        )
      : panel(
          "Sổ điểm thành viên",
          table(
            ["Khách hàng", "Loại", "Điểm", "Còn hiệu lực", "Ngày ghi nhận"],
            data.loyalty.map((e) => [
              esc(e.customer_name),
              esc(
                { EARN: "Tích điểm", REDEEM: "Đổi điểm", EXPIRE: "Hết hạn" }[
                  e.entry_type
                ] || e.entry_type,
              ),
              `<strong>${e.points > 0 ? "+" : ""}${e.points}</strong>`,
              e.expires_at
                ? `${e.remaining_points} điểm · ${dateStr(e.expires_at)}`
                : "—",
              fmt(e.created_at),
            ]),
          ),
        ))
  );
}
function promotions() {
  return (
    pageHead(
      "Ưu đãi & thành viên",
      "Cấu hình quyền lợi theo hạng và mã giảm giá.",
      `${btn("Thêm hạng", "tier-new")}${plus("Tạo mã ưu đãi", "promotion-new")}`,
    ) +
    `<div class="cards" style="margin-bottom:24px">${data.tiers.map((t) => `<article class="service-card"><div class="service-content"><div class="eyebrow">MEMBERSHIP</div><h3>${badge(t.name)}</h3><div class="price">Giảm ${t.discount_percent}%</div><p class="muted small">Chi tiêu 12 tháng từ ${money(t.min_spending)}<br>Hệ số tích điểm ×${t.multiplier}</p></div><div class="service-bottom"><span class="muted small">Tự xác định từ hóa đơn thực tế</span>${btn("Cấu hình", "tier-edit", t.id, "sm")}</div></article>`).join("")}</div>` +
    panel(
      "Mã khuyến mãi",
      table(
        ["Mã", "Mức giảm", "Thời gian", "Điều kiện", "Lượt dùng", ""],
        data.promotions.map((p) => [
          `<strong>${esc(p.code)}</strong>${!p.is_active ? '<span class="sub">Ngừng áp dụng</span>' : ""}`,
          p.discount_percent + "%",
          `${dateStr(p.start_date)} – ${dateStr(p.end_date)}`,
          money(p.min_total),
          `${p.used_count} / ${p.max_uses}`,
          btn("Sửa", "promotion-edit", p.id, "sm"),
        ]),
      ),
      "",
      "Combo/nhóm và voucher chọn mức có lợi hơn; sau đó giảm theo hạng và đổi điểm.",
    )
  );
}
function inventory() {
  if (!["products", "movements", "recipes"].includes(tab)) tab = "products";
  const low = data.products.filter((p) => p.stock_qty <= p.reorder_level);
  return (
    pageHead(
      "Kho & vật tư",
      "Theo dõi nhập xuất, định mức và cảnh báo tồn kho.",
      `${btn("Nhập / Xuất kho", "stock-adjust")}${plus("Thêm sản phẩm", "product-new")}`,
    ) +
    (low.length
      ? `<div class="note warning">${low.length} sản phẩm chạm mức cảnh báo: ${low.map((p) => esc(p.name)).join(", ")}.</div>`
      : "") +
    tabsHtml([
      ["products", "Danh mục & tồn kho"],
      ["movements", "Lịch sử nhập xuất"],
      ["recipes", "Định mức tiêu hao"],
    ]) +
    (tab === "products"
      ? panel(
          "Danh mục kho",
          searchbar() +
            table(
              ["Sản phẩm", "Mã SKU", "Đơn giá", "Tồn kho", "Cảnh báo", ""],
              filtered(data.products, ["name", "sku"]).map((p) => [
                `<strong>${esc(p.name)}</strong><span class="sub">${esc(p.unit)} · ${p.is_active ? "Đang dùng" : "Ngừng dùng"}</span>`,
                esc(p.sku),
                money(p.sale_price),
                `<strong style="color:${p.stock_qty <= p.reorder_level ? "var(--danger)" : "inherit"}">${int(p.stock_qty)}</strong>`,
                p.reorder_level,
                btn("Sửa", "product-edit", p.id, "sm"),
              ]),
            ),
        )
      : tab === "movements"
        ? panel(
            "Nhập xuất kho",
            table(
              ["Thời gian", "Sản phẩm", "Biến động", "Lý do"],
              data.stock_movements.map((m) => [
                fmt(m.created_at),
                esc(m.product_name),
                `${m.quantity > 0 ? "+" : ""}${m.quantity}`,
                esc(m.reason),
              ]),
            ),
          )
        : panel(
            "Định mức theo dịch vụ",
            table(
              ["Dịch vụ", "Vật tư", "Lượng tiêu hao"],
              data.recipes.map((r) => [
                esc(r.service_name),
                esc(r.product_name),
                `${r.quantity} ${esc(r.unit)}`,
              ]),
            ),
            btn("Thêm / Sửa định mức", "recipe", "", "sm primary"),
          ))
  );
}
function reports() {
  const revenue = data.revenue || [],
    total = revenue.reduce((s, r) => s + r.revenue, 0),
    invoices = revenue.reduce((s, r) => s + r.invoices, 0),
    max = Math.max(...revenue.map((r) => r.revenue), 1);
  return (
    pageHead(
      "Báo cáo vận hành",
      "Toàn hệ thống · Doanh thu theo ngày xuất hóa đơn, gồm tiền bán gói.",
      `<a class="btn" href="/api/export?${new URLSearchParams({ from: reportFrom || today().slice(0, 7) + "-01", to: reportTo || today() })}">${icon("download")}Xuất CSV</a>`,
    ) +
    `<section class="panel"><div class="toolbar"><label>Từ ngày <input id="report-from" type="date" value="${reportFrom || today().slice(0, 7) + "-01"}"></label><label>Đến ngày <input id="report-to" type="date" value="${reportTo || today()}"></label>${btn("Áp dụng", "report-filter", "", "primary")}</div></section><div class="kpis">${kpi("Doanh thu", money(total), "Hóa đơn PAID trong kỳ", "wallet")}${kpi("Hóa đơn", int(invoices), "Gồm dịch vụ và gói liệu trình", "check")}${kpi("Giá trị trung bình", money(invoices ? total / invoices : 0), "Trên một hóa đơn", "chart")}${kpi("Hoa hồng trong kỳ", money((data.staff_report || []).reduce((s, r) => s + r.commission, 0)), "Theo hóa đơn đã thanh toán", "staff")}</div><div class="note">Lịch hẹn trong kỳ: ${data.report_stats?.appointments || 0} · Vắng mặt: ${data.report_stats?.no_shows || 0} (${data.report_stats?.appointments ? Math.round((data.report_stats.no_shows / data.report_stats.appointments) * 100) : 0}%) · Khách hàng mới: ${data.new_customers || 0}</div><div class="grid-2"><div>${panel(
      "Doanh thu theo ngày",
      revenue.length
        ? `<div class="panel-body"><div class="report-chart">${revenue
            .slice(-20)
            .map(
              (r) =>
                `<div class="report-bar" style="height:${Math.max(2, (r.revenue / max) * 140)}px"><span>${int(r.revenue / 1000)}k</span><small>${dateStr(r.day).slice(0, 5)}</small></div>`,
            )
            .join("")}</div></div>`
        : empty("Chưa có hóa đơn trong khoảng ngày này."),
    )}${panel(
      "Chi tiết doanh thu",
      table(
        ["Ngày", "Hóa đơn", "Doanh thu"],
        revenue.map((r) => [dateStr(r.day), r.invoices, money(r.revenue)]),
      ),
    )}</div><div>${panel(
      "Dịch vụ được sử dụng",
      table(
        ["Dịch vụ", "Lượt", "Giá niêm yết"],
        (data.top_services || []).map((r) => [
          esc(r.name),
          r.quantity,
          money(r.gross),
        ]),
      ),
      "",
      "Giá tại thời điểm đặt, trước giảm giá; không cộng vào tổng doanh thu.",
    )}${panel(
      "Đóng góp đội ngũ",
      table(
        ["Nhân viên", "Hóa đơn", "Hoa hồng"],
        (data.staff_report || []).map((r) => [
          esc(r.full_name),
          r.paid_jobs,
          money(r.commission),
        ]),
      ),
    )}</div></div>${panel(
      "Khách hàng thân thiết",
      table(
        ["Khách hàng", "Hạng hiện tại", "Chi tiêu 12 tháng", "Điểm còn hạn"],
        (data.loyal_customers || []).map((c) => [
          esc(c.full_name),
          badge(c.tier),
          money(c.spending_12m),
          int(c.point_balance),
        ]),
      ),
    )}`
  );
}
function notifications() {
  return (
    pageHead(
      "Thông báo & nhắc lịch",
      "Lịch sử xác nhận, thay đổi và nhắc lịch trước 24 giờ.",
      desk()
        ? btn("Xử lý thông báo đến hạn", "notification-dispatch", "", "primary")
        : "",
    ) +
    `<div class="note">Chế độ demo: IN_APP / mô phỏng gửi. Bộ xử lý chạy mỗi 30 giây; trạng thái “Đã mô phỏng gửi” không có nghĩa SMS hoặc email đã được gửi ra ngoài.</div>` +
    panel(
      "Hộp thông báo",
      table(
        ["Khách hàng", "Nội dung", "Hẹn gửi", "Trạng thái"],
        data.notifications.map((n) => [
          esc(n.customer_name),
          `<span style="white-space:normal;max-width:430px;display:block">${esc(n.message)}</span>`,
          fmt(n.due_at),
          badge(n.status),
        ]),
      ),
    )
  );
}
function reviews() {
  return (
    pageHead(
      "Đánh giá trải nghiệm",
      "Lắng nghe để mỗi lần chăm sóc đều tốt hơn.",
      is("CUSTOMER") ? plus("Viết đánh giá", "review-new") : "",
    ) +
    panel(
      "Phản hồi khách hàng",
      table(
        ["Khách hàng", "Lịch hẹn", "Đánh giá", "Nhận xét", "Ngày"],
        data.reviews.map((r) => [
          esc(r.customer_name),
          "#" + r.appointment_id,
          `<span class="stars">${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}</span>`,
          `<span style="white-space:normal;max-width:400px;display:block">${esc(r.comment) || "—"}</span>`,
          fmt(r.created_at),
        ]),
      ),
    )
  );
}
function accounts() {
  return (
    pageHead(
      "Tài khoản & bảo mật",
      "Phân quyền, vô hiệu hóa tài khoản và mở khóa khi cần.",
      plus("Thêm tài khoản", "account-new"),
    ) +
    panel(
      "Người dùng",
      table(
        ["Tài khoản", "Họ tên", "Vai trò", "Trạng thái", ""],
        data.accounts.map((a) => [
          `<strong>${esc(a.username)}</strong>`,
          esc(a.full_name),
          roles[a.role],
          a.is_active
            ? a.locked_until && a.locked_until > data.server_time
              ? badge("CANCELLED") + ' <span class="small">Đang khóa</span>'
              : badge("COMPLETED")
            : "Vô hiệu hóa",
          btn("Sửa / Mở khóa", "account-edit", a.id, "sm"),
        ]),
      ),
    )
  );
}
function settings() {
  const customer = data.customers[0];
  return (
    pageHead(
      "Hồ sơ & cài đặt",
      "Thông tin cá nhân và bảo mật tài khoản.",
      is("ADMIN")
        ? '<a class="btn" href="/api/backup">' +
            icon("download") +
            "Sao lưu dữ liệu</a>"
        : "",
    ) +
    `<div class="grid-equal">${panel("Tài khoản của bạn", `<div class="panel-body"><div class="userbox"><span class="avatar">${esc(user.full_name.slice(0, 1))}</span><div><h3>${esc(user.full_name)}</h3><p class="muted small">${esc(user.username)} · ${roles[user.role]}</p></div></div><div style="margin-top:24px">${is("CUSTOMER") && customer ? `<p>${esc(customer.phone)} · ${esc(customer.email)}</p><p class="muted small" style="margin-bottom:18px">Hạng ${esc(customer.tier)} · ${customer.point_balance} điểm khả dụng</p>${btn("Cập nhật hồ sơ", "customer-edit", customer.id)}` : '<p class="muted">Liên hệ quản trị viên để cập nhật vai trò hoặc hồ sơ nhân viên.</p>'}</div></div>`)}${panel("Bảo mật", `<div class="panel-body"><p class="muted" style="margin-bottom:20px">Mật khẩu được băm PBKDF2. Tài khoản tự khóa 15 phút sau 5 lần đăng nhập sai.</p>${btn("Đổi mật khẩu", "password", "", "primary")}</div>`)}${
      manager()
        ? panel(
            "Nhật ký thao tác",
            table(
              ["Thời gian", "Người dùng", "Thao tác"],
              data.audit
                .slice(0, 30)
                .map((a) => [
                  fmt(a.created_at),
                  esc(a.user_name),
                  esc(a.action) + " · " + esc(a.detail),
                ]),
            ),
          )
        : ""
    }</div>`
  );
}
function facilities() {
  return (
    pageHead(
      "Chi nhánh & phòng",
      "Quản lý không gian theo loại phòng và chi nhánh.",
      `${btn("Thêm chi nhánh", "branch-new")}${plus("Thêm phòng", "room-new")}`,
    ) +
    panel(
      "Chi nhánh",
      table(
        ["Tên", "Địa chỉ", ""],
        data.branches.map((b) => [
          esc(b.name),
          esc(b.address),
          btn("Sửa", "branch-edit", b.id, "sm"),
        ]),
      ),
    ) +
    panel(
      "Phòng & khu vực dịch vụ",
      table(
        ["Phòng", "Chi nhánh", "Loại", "Trạng thái", ""],
        data.rooms.map((r) => [
          esc(r.name),
          esc(data.branches.find((b) => b.id === r.branch_id)?.name),
          esc(r.room_type),
          r.is_active ? "Hoạt động" : "Tạm đóng",
          btn("Sửa", "room-edit", r.id, "sm"),
        ]),
      ),
    )
  );
}
const pages = {
  dashboard,
  appointments,
  services,
  customers,
  staff,
  facilities,
  packages,
  invoices,
  promotions,
  inventory,
  reports,
  notifications,
  reviews,
  accounts,
  settings,
};
function modal(title, body, footer = "", wide = false) {
  lastFocus = document.activeElement;
  $("#modal-root").innerHTML =
    `<div class="modal-overlay"><section class="modal ${wide ? "wide" : ""}" role="dialog" aria-modal="true" aria-label="${esc(title)}"><div class="modal-head"><h2>${esc(title)}</h2><button class="close" data-action="close-modal" aria-label="Đóng">×</button></div><div class="modal-body">${body}</div>${footer ? `<div class="modal-foot">${footer}</div>` : ""}</section></div>`;
  setTimeout(
    () =>
      $("input:not([type=hidden]),select,button", $("#modal-root"))?.focus(),
    0,
  );
}
function closeModal() {
  $("#modal-root").innerHTML = "";
  lastFocus?.focus();
}
function field(f, values = {}) {
  const v = values[f.name] ?? f.value ?? "",
    id = "f-" + f.name;
  let control = "";
  const common = `id="${id}" name="${f.name}" ${f.required === false ? "" : "required"} ${f.disabled ? "disabled" : ""}`;
  if (f.type === "select" || f.type === "multi")
    control = `<select ${common} ${f.type === "multi" ? "multiple" : ""}>${f.placeholder ? `<option value="">${esc(f.placeholder)}</option>` : ""}${(f.options || []).map(([key, label]) => `<option value="${esc(key)}" ${(Array.isArray(v) ? v.map(String).includes(String(key)) : String(v) === String(key)) ? "selected" : ""}>${esc(label)}</option>`).join("")}</select>`;
  else if (f.type === "check")
    control = `<label><input type="checkbox" id="${id}" name="${f.name}" ${v === true || v === "true" ? "checked" : ""}>${esc(f.checkLabel || "Đang hoạt động")}</label>`;
  else if (f.type === "textarea")
    control = `<textarea ${common} rows="3" maxlength="${f.max || 500}">${esc(v)}</textarea>`;
  else
    control = `<input ${common} type="${f.type || "text"}" value="${esc(v)}" ${f.min !== undefined ? `min="${f.min}"` : ""} ${f.max !== undefined ? `max="${f.max}"` : ""} ${f.step ? `step="${f.step}"` : ""} ${f.type === "password" ? 'autocomplete="new-password"' : ""}>`;
  return `<div class="field ${f.full ? "full" : ""}">${f.type !== "check" ? `<label for="${id}">${esc(f.label)}${f.required === false ? ' <span class="muted">(tùy chọn)</span>' : ""}</label>` : ""}${control}${f.hint ? `<small>${esc(f.hint)}</small>` : ""}</div>`;
}
function readForm(form) {
  const out = {};
  for (const el of [...form.elements]) {
    if (!el.name || el.disabled) continue;
    if (el.type === "checkbox") out[el.name] = el.checked;
    else if (el.multiple)
      out[el.name] = [...el.selectedOptions].map((o) => Number(o.value));
    else if (
      el.type === "number" ||
      (el.tagName === "SELECT" &&
        el.dataset.string !== "true" &&
        el.value !== "" &&
        !isNaN(el.value))
    )
      out[el.name] = el.value === "" ? 0 : Number(el.value);
    else out[el.name] = el.value;
  }
  return out;
}
let submitHandler = null;
function formModal(
  title,
  fields,
  command,
  values = {},
  onSubmit = null,
  extra = "",
) {
  submitHandler = async (a) => {
    const result = onSubmit
      ? await onSubmit(a)
      : await action(command, { ...a, id: values.id || 0 });
    await refresh();
    closeModal();
    toast("Đã lưu thay đổi thành công.");
    return result;
  };
  modal(
    title,
    `<form id="modal-form"><div class="form-error" role="alert"></div><div class="form-grid">${fields.map((f) => field(f, values)).join("")}</div>${extra}</form>`,
    `${btn("Hủy", "close-modal")}<button class="btn primary" type="submit" form="modal-form">Lưu thay đổi</button>`,
  );
}
const numField = (name, label, min = 0, max = 1000000000) => ({
  name,
  label,
  type: "number",
  min,
  max,
});
const selectField = (name, label, options, required = true) => ({
  name,
  label,
  type: "select",
  options,
  required,
});
const activeField = {
  name: "is_active",
  type: "check",
  value: true,
  checkLabel: "Đang hoạt động / đang bán",
  required: false,
};
function customerForm(id) {
  const c = data.customers.find((c) => c.id == id) || {};
  formModal(
    id ? "Cập nhật hồ sơ khách hàng" : "Thêm khách hàng",
    [
      { name: "full_name", label: "Họ và tên", max: 100 },
      { name: "phone", label: "Số điện thoại", max: 10 },
      { name: "email", label: "Email", type: "email", required: false },
      { name: "birthday", label: "Ngày sinh", type: "date", required: false },
      {
        name: "allergy_note",
        label: "Dị ứng & lưu ý chăm sóc",
        type: "textarea",
        required: false,
        full: true,
      },
    ],
    "customer.save",
    c,
  );
}
function serviceForm(id) {
  formModal(
    id ? "Chỉnh sửa dịch vụ" : "Thêm dịch vụ",
    [
      { name: "name", label: "Tên dịch vụ", max: 100 },
      { name: "category", label: "Danh mục", value: "Body", max: 50 },
      numField("price", "Giá niêm yết (₫)"),
      numField("duration_min", "Thời lượng (phút)", 15, 480),
      selectField("room_type", "Loại phòng", [
        ["SPA", "SPA · Phòng spa"],
        ["HAIR", "HAIR · Ghế tóc"],
        ["NAIL", "NAIL · Bàn nail"],
      ]),
      activeField,
    ],
    "service.save",
    data.services.find((s) => s.id == id) || { duration_min: 60 },
  );
}
function branchForm(id) {
  formModal(
    id ? "Cập nhật chi nhánh" : "Thêm chi nhánh",
    [
      { name: "name", label: "Tên chi nhánh", full: true },
      { name: "address", label: "Địa chỉ", full: true, max: 250 },
    ],
    "branch.save",
    data.branches.find((b) => b.id == id) || {},
  );
}
function roomForm(id) {
  formModal(
    id ? "Cập nhật phòng" : "Thêm phòng",
    [
      { name: "name", label: "Tên phòng" },
      selectField("branch_id", "Chi nhánh", options(data.branches)),
      selectField("room_type", "Loại phòng", [
        ["SPA", "SPA"],
        ["HAIR", "HAIR"],
        ["NAIL", "NAIL"],
      ]),
      activeField,
    ],
    "room.save",
    data.rooms.find((r) => r.id == id) || {},
  );
}
function staffForm(id) {
  const s = data.staff.find((s) => s.id == id) || {};
  formModal(
    id ? "Cập nhật nhân viên" : "Thêm nhân viên",
    [
      { name: "full_name", label: "Họ tên" },
      { name: "phone", label: "Điện thoại", max: 10 },
      selectField("branch_id", "Chi nhánh", options(data.branches)),
      numField("commission_percent", "Hoa hồng (%)", 0, 100),
      {
        name: "service_ids",
        label: "Chuyên môn",
        type: "multi",
        options: serviceOptions(),
        full: true,
        hint: "Giữ Ctrl/Cmd để chọn nhiều dịch vụ.",
      },
      ...(!id
        ? [
            {
              name: "username",
              label: "Tài khoản kỹ thuật viên",
              required: false,
              hint: "Để trống nếu chưa cần tài khoản đăng nhập.",
            },
            {
              name: "password",
              label: "Mật khẩu (tối thiểu 8 ký tự)",
              type: "password",
              required: false,
            },
          ]
        : []),
      activeField,
    ],
    "staff.save",
    {
      ...s,
      commission_percent: s.commission_percent ?? 10,
      service_ids: data.skills
        .filter((k) => k.staff_id == id)
        .map((k) => k.service_id),
    },
  );
}
function packageForm(id) {
  formModal(
    id ? "Cấu hình liệu trình" : "Thêm gói liệu trình",
    [
      { name: "name", label: "Tên gói", full: true },
      selectField("service_id", "Dịch vụ", serviceOptions()),
      numField("total_sessions", "Số buổi", 1, 1000),
      numField("price", "Giá gói (₫)"),
      numField("valid_days", "Hiệu lực (ngày)", 1, 3650),
      activeField,
    ],
    "package.save",
    data.packages.find((p) => p.id == id) || {
      total_sessions: 5,
      valid_days: 90,
    },
  );
}
function comboForm(id) {
  const c = data.combos.find((c) => c.id == id) || {};
  formModal(
    id ? "Cập nhật combo" : "Thêm combo",
    [
      { name: "name", label: "Tên combo" },
      numField("discount_percent", "Giảm giá (%)", 0, 80),
      {
        name: "service_ids",
        label: "Dịch vụ trong combo",
        type: "multi",
        full: true,
        options: serviceOptions(),
        hint: "Chọn ít nhất 2 dịch vụ; thực hiện nối tiếp trong một lịch.",
      },
      activeField,
    ],
    "combo.save",
    {
      ...c,
      discount_percent: c.discount_percent ?? 20,
      service_ids: data.combo_items
        .filter((i) => i.combo_id == id)
        .map((i) => i.service_id),
    },
  );
}
function tierForm(id) {
  formModal(
    id ? "Cấu hình hạng thành viên" : "Thêm hạng",
    [
      { name: "name", label: "Tên hạng" },
      numField("min_spending", "Chi tiêu 12 tháng tối thiểu (₫)"),
      numField("discount_percent", "Giảm theo hạng (%)", 0, 50),
      { ...numField("multiplier", "Hệ số tích điểm", 1, 10), step: 0.1 },
    ],
    "tier.save",
    data.tiers.find((t) => t.id == id) || {
      multiplier: 1,
      discount_percent: 0,
      min_spending: 0,
    },
  );
}
function promotionForm(id) {
  formModal(
    id ? "Cập nhật mã ưu đãi" : "Tạo mã ưu đãi",
    [
      { name: "code", label: "Mã (A–Z, số, _ hoặc -)" },
      numField("discount_percent", "Giảm giá (%)", 1, 80),
      { name: "start_date", label: "Bắt đầu", type: "date" },
      { name: "end_date", label: "Kết thúc", type: "date" },
      numField("max_uses", "Số lượt tối đa", 1, 1000000),
      numField("min_total", "Giá trị hóa đơn tối thiểu (₫)"),
      activeField,
    ],
    "promotion.save",
    data.promotions.find((p) => p.id == id) || {
      start_date: today(),
      end_date: today(),
      max_uses: 100,
      discount_percent: 10,
      min_total: 0,
    },
  );
}
function productForm(id) {
  formModal(
    id ? "Cập nhật sản phẩm" : "Thêm sản phẩm",
    [
      { name: "name", label: "Tên sản phẩm" },
      { name: "sku", label: "Mã SKU" },
      { name: "unit", label: "Đơn vị", value: "chai" },
      numField("sale_price", "Giá bán mỗi đơn vị (₫)"),
      numField("reorder_level", "Ngưỡng tồn kho thấp", 0, 1000000),
      activeField,
    ],
    "product.save",
    data.products.find((p) => p.id == id) || {
      reorder_level: 5,
      sale_price: 0,
    },
  );
}
function accountForm(id) {
  const u = data.accounts.find((a) => a.id == id) || {};
  formModal(
    id ? "Cập nhật / Mở khóa tài khoản" : "Thêm tài khoản nội bộ",
    [
      ...(!id ? [{ name: "username", label: "Tên đăng nhập" }] : []),
      { name: "full_name", label: "Họ tên" },
      selectField(
        "role",
        "Vai trò",
        Object.entries(roles).filter(
          ([r]) => id || !["CUSTOMER", "TECHNICIAN"].includes(r),
        ),
      ),
      {
        name: "password",
        label: id
          ? "Mật khẩu mới (để trống nếu giữ nguyên)"
          : "Mật khẩu (tối thiểu 8 ký tự)",
        type: "password",
        required: !id,
      },
      activeField,
    ],
    "account.save",
    u,
    null,
    id
      ? '<div class="note">Lưu sẽ mở khóa và đặt lại bộ đếm đăng nhập sai. Vai trò kỹ thuật viên/khách hàng gắn với hồ sơ riêng.</div>'
      : "",
  );
}
function bookingForm(pref = {}) {
  const own = is("CUSTOMER") ? data.customers[0]?.id : undefined;
  const now = new Date();
  now.setDate(now.getDate() + 1);
  const tomorrow = now.toLocaleDateString("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
  });
  const fields = [
    ...(!is("CUSTOMER")
      ? [selectField("customer_id", "Khách hàng", customerOptions())]
      : []),
    selectField("branch_id", "Chi nhánh", options(data.branches)),
    {
      name: "service_ids",
      label: "Dịch vụ (chọn nhiều bằng Ctrl/Cmd)",
      type: "multi",
      options: serviceOptions(),
      required: false,
      full: true,
    },
    selectField("combo_id", "Hoặc chọn combo", [
      [0, "Không dùng combo"],
      ...options(data.combos),
    ]),
    selectField("customer_package_id", "Hoặc dùng buổi liệu trình", [
      [0, "Không dùng liệu trình"],
      ...data.customer_packages
        .filter((p) => p.available_sessions > 0)
        .map((p) => [
          p.id,
          `#${p.id} ${p.customer_name} · ${p.name} · còn ${p.available_sessions}`,
        ]),
    ]),
    selectField("staff_id", "Nhân viên", [
      [0, "Tự chọn nhân viên phù hợp"],
      ...staffOptions(),
    ]),
    numField("group_count", "Số người đi cùng", 1, 10),
    {
      name: "confirm_overlap",
      type: "check",
      required: false,
      full: true,
      checkLabel:
        "Xác nhận đặt thay người đi cùng nếu hồ sơ này đã có lịch trùng giờ",
    },
    { name: "date", label: "Ngày hẹn", type: "date", value: tomorrow },
    { name: "start_time", label: "Giờ bắt đầu", type: "time", value: "09:00" },
    {
      name: "note",
      label: "Lưu ý chăm sóc",
      type: "textarea",
      required: false,
      full: true,
    },
  ];
  submitHandler = async (a) => {
    const result = await action("appointment.create", {
      ...a,
      start_at: a.date + "T" + a.start_time,
      customer_id: own || a.customer_id,
    });
    page = "appointments";
    await refresh();
    closeModal();
    toast(`Đã tạo ${result.ids.length} lịch hẹn: #${result.ids.join(", #")}.`);
  };
  modal(
    "Đặt lịch chăm sóc",
    `<form id="modal-form"><div class="form-error" role="alert"></div><div class="form-grid">${fields.map((f) => field(f, { group_count: 1, branch_id: 1, staff_id: 0, ...pref })).join("")}</div><div class="actions" style="margin-bottom:14px">${btn("Tìm giờ trống", "booking-slots", "", "sm")}</div><div id="slots" class="actions" style="margin-bottom:15px"></div><div class="note">Đi đôi giảm 50%, nhóm 3–4 người giảm 20%, từ 5 người giảm 30%. Mỗi người có lịch và tài nguyên riêng; chọn tự động nhân viên khi đi nhóm. Ưu đãi nhóm/combo được giữ tại thời điểm đặt.</div></form>`,
    `${btn("Hủy", "close-modal")}<button class="btn primary" type="submit" form="modal-form">Xác nhận đặt lịch</button>`,
    true,
  );
}
function appointmentDetail(id) {
  const a = data.appointments.find((a) => a.id == id);
  if (!a) return;
  const items = data.items.filter((i) => i.appointment_id == id);
  const review = data.reviews.find((r) => r.appointment_id == id);
  const buttons = [];
  if (a.status === "CONFIRMED" && !is("TECHNICIAN"))
    buttons.push(
      btn("Dời lịch", "reschedule", id),
      btn("Hủy lịch", "cancel", id, "danger"),
    );
  if (desk() && a.status === "CONFIRMED")
    buttons.push(
      btn("Check-in", "checkin", id, "primary"),
      btn("Vắng mặt", "no-show", id),
    );
  if ((desk() || is("TECHNICIAN")) && a.status === "CHECKED_IN")
    buttons.push(btn("Bắt đầu dịch vụ", "start", id, "primary"));
  if ((desk() || is("TECHNICIAN")) && a.status === "IN_PROGRESS")
    buttons.push(btn("Hoàn thành", "complete", id, "primary"));
  if (desk() && a.status === "COMPLETED" && !a.invoice_id)
    buttons.push(btn("Thanh toán", "pay", id, "primary"));
  if (a.invoice_id)
    buttons.push(btn("Xem hóa đơn", "invoice-detail", a.invoice_id));
  if (is("CUSTOMER") && a.status === "COMPLETED" && !review)
    buttons.push(btn("Đánh giá", "review-appointment", id, "primary"));
  modal(
    "Chi tiết lịch #" + id,
    `<div class="detail-grid"><div><div class="detail-label">Khách hàng</div><div class="detail-value">${esc(a.customer_name)}</div><p class="muted small">${esc(a.phone)}</p></div><div><div class="detail-label">Trạng thái</div>${badge(a.status)}</div><div><div class="detail-label">Thời gian</div>${fmt(a.start_at)} → ${time(a.end_at)}</div><div><div class="detail-label">Chi nhánh</div>${esc(a.branch_name)}</div></div>${table(
      ["Dịch vụ", "Thực hiện", "Nhân viên / Phòng", "Giá tại lúc đặt"],
      items.map((i) => [
        esc(i.service_name),
        `${time(i.start_at)} – ${time(i.end_at)}`,
        `${esc(i.staff_name)}<span class="sub">${esc(i.room_name)}</span>`,
        money(i.price_at_booking) +
          (i.customer_package_id
            ? '<span class="sub">Dùng buổi liệu trình</span>'
            : ""),
      ]),
    )}<p class="muted small" style="margin-top:20px">Ghi chú: ${esc(a.note) || "Không có"}${a.cancel_reason ? "<br>Lý do hủy: " + esc(a.cancel_reason) : ""}</p>`,
    buttons.join(""),
    true,
  );
}
function customerDetail(id) {
  const c = data.customers.find((c) => c.id == id);
  modal(
    "Hồ sơ · " + c.full_name,
    `<div class="detail-grid"><div><div class="detail-label">Liên hệ</div>${esc(c.phone)}<p class="muted small">${esc(c.email)}</p></div><div><div class="detail-label">Thành viên</div>${badge(c.tier)} · ${int(c.point_balance)} điểm</div><div><div class="detail-label">Chi tiêu 12 tháng</div>${money(c.spending_12m)}</div><div><div class="detail-label">Dị ứng / lưu ý</div>${esc(c.allergy_note) || "Không có"}</div></div><h3 style="margin-bottom:15px">Lịch sử dịch vụ</h3>${appointmentTable(data.appointments.filter((a) => a.customer_id == id))}<h3 style="margin:22px 0 15px">Gói liệu trình</h3>${table(
      ["Gói", "Khả dụng", "Hạn dùng"],
      data.customer_packages
        .filter((p) => p.customer_id == id)
        .map((p) => [esc(p.name), p.available_sessions, dateStr(p.expires_at)]),
    )}<h3 style="margin:22px 0 15px">Lịch sử điểm</h3>${table(
      ["Loại", "Điểm", "Ngày"],
      data.loyalty
        .filter((l) => l.customer_id == id)
        .map((l) => [esc(l.entry_type), l.points, fmt(l.created_at)]),
    )}`,
    btn("Chỉnh sửa hồ sơ", "customer-edit", id) +
      (manager()
        ? btn(
            c.is_active ? "Ngừng hoạt động" : "Kích hoạt",
            "customer-active",
            id,
            c.is_active ? "danger" : "primary",
          )
        : ""),
    true,
  );
}
function rescheduleForm(id) {
  const a = data.appointments.find((a) => a.id == id);
  formModal(
    "Dời lịch #" + id,
    [
      {
        name: "start_at",
        label: "Giờ hẹn mới",
        type: "datetime-local",
        value: a.start_at.slice(0, 16),
        full: true,
      },
      ...(desk()
        ? [
            {
              name: "override",
              type: "check",
              checkLabel: "Ghi đè giới hạn báo trước 2 giờ (cần lý do)",
              required: false,
            },
          ]
        : []),
      {
        name: "confirm_overlap",
        type: "check",
        required: false,
        full: true,
        checkLabel:
          "Xác nhận hồ sơ đặt thay người đi cùng khi bị trùng lịch khách",
      },
      {
        name: "reason",
        label: "Lý do thay đổi",
        type: "textarea",
        required: false,
        full: true,
      },
    ],
    "appointment.reschedule",
    { id },
  );
}
function cancelForm(id) {
  formModal(
    "Hủy lịch #" + id,
    [
      {
        name: "reason",
        label: "Lý do hủy",
        type: "textarea",
        max: 250,
        full: true,
      },
      ...(desk()
        ? [
            {
              name: "override",
              type: "check",
              checkLabel: "Ghi đè giới hạn báo trước 2 giờ",
              required: false,
            },
          ]
        : []),
    ],
    "appointment.cancel",
    { id },
  );
}
function paymentForm(id) {
  submitHandler = async (a) => {
    const args = paymentArgs(a, id);
    await action("invoice.pay", args);
    await refresh();
    closeModal();
    toast("Thanh toán thành công. Điểm, gói và hoa hồng đã được cập nhật.");
  };
  modal(
    "Thanh toán lịch #" + id,
    `<form id="modal-form"><div class="form-error" role="alert"></div><div class="form-grid">${field({ name: "promotion_code", label: "Mã khuyến mãi", required: false })}${field({ ...numField("redeem_points", "Điểm muốn đổi", 0, 1000000), step: 100, value: 0 })}${field(
      selectField("method", "Hình thức thu tiền", [
        ["CASH", "Tiền mặt"],
        ["CARD", "Thẻ (ghi nhận demo)"],
        ["TRANSFER", "Chuyển khoản (ghi nhận demo)"],
        ["QR", "QR (ghi nhận demo)"],
      ]),
    )}${field(selectField("retail_staff_id", "Nhân viên bán sản phẩm", [[0, "Không ghi nhận hoa hồng bán lẻ"], ...staffOptions()]))}</div><h3 style="margin-bottom:12px">Sản phẩm bán kèm</h3><div id="cart-lines"></div>${btn("Thêm sản phẩm", "cart-add", "", "sm")}<div style="margin:22px 0">${btn("Tính hóa đơn", "pay-preview", id, "primary sm")}</div><div id="payment-preview" class="panel-body" style="background:#edf2e7"><p class="muted small">Bấm Tính hóa đơn để xem số tiền trước khi xác nhận.</p></div><div class="note" style="margin:18px 0 0">Chỉ ghi nhận thanh toán đã thu. Thẻ / chuyển khoản / QR đang ở chế độ mô phỏng, chưa kết nối ngân hàng.</div></form>`,
    `${btn("Hủy", "close-modal")}<button type="submit" form="modal-form" class="btn primary">Xác nhận đã thu tiền</button>`,
    true,
  );
}
function paymentArgs(a, id) {
  const products = $$(".cart-line")
    .filter((r) => $("select", r).value)
    .map((r) => ({
      product_id: Number($("select", r).value),
      quantity: Number($("input", r).value),
    }));
  return { ...a, appointment_id: Number(id), products };
}
function summary(v) {
  return (
    [
      ["Tạm tính", v.subtotal],
      ["Thanh toán bằng buổi liệu trình", -v.package_discount],
      ["Ưu đãi combo / nhóm", -v.combo_discount],
      ["Mã khuyến mãi", -v.promo_discount],
      ["Giảm theo hạng", -v.tier_discount],
      ["Đổi điểm", -v.point_discount],
    ]
      .map(
        ([name, value]) =>
          `<div class="summary-line"><span>${name}</span><strong>${money(value)}</strong></div>`,
      )
      .join("") +
    `<div class="summary-line total"><span>Tổng thanh toán</span><span>${money(v.total)}</span></div>${v.earned_points !== undefined ? `<p class="muted small">Tích thêm ${v.earned_points} điểm · hạng áp dụng ${esc(v.tier)}</p>` : ""}`
  );
}
function invoiceDetail(id) {
  const v = data.invoices.find((v) => v.id == id);
  if (!v) {
    toast("Không có quyền xem hóa đơn này.", true);
    return;
  }
  const items = data.items.filter((i) => i.appointment_id === v.appointment_id),
    products = (data.invoice_products || []).filter((p) => p.invoice_id == id);
  modal(
    "Hóa đơn #" + id,
    `<article class="print-invoice">${logo()}<div style="display:flex;justify-content:space-between;gap:20px"><div><h1>Hóa đơn #${id}</h1><p>${esc(v.customer_name)}</p><p class="muted small">${fmt(v.created_at)} · ${esc(v.description)}</p></div>${badge(v.status)}</div><div style="margin:25px 0">${table(["Dịch vụ / Sản phẩm", "Số lượng", "Thành tiền"], [...items.map((i) => [esc(i.service_name), 1, money(i.price_at_booking)]), ...products.map((p) => [esc(p.product_name), p.quantity, money(p.quantity * p.unit_price)]), ...(!items.length && !products.length ? [[esc(v.description), 1, money(v.subtotal)]] : [])])}</div>${summary(v)}<p class="muted small" style="margin-top:25px;text-align:center">Cảm ơn bạn đã dành thời gian chăm sóc bản thân cùng SpaCare.<br>Chứng từ minh họa phục vụ học tập · Không phải hóa đơn thuế.</p></article>`,
    btn("In / Lưu PDF", "print", "", "primary"),
    true,
  );
}
function reviewForm(id) {
  const eligible = data.appointments.filter(
    (a) =>
      a.status === "COMPLETED" &&
      !data.reviews.some((r) => r.appointment_id === a.id),
  );
  if (!eligible.length)
    return toast("Chưa có lịch hoàn thành chưa đánh giá.", true);
  formModal(
    "Đánh giá trải nghiệm",
    [
      selectField(
        "appointment_id",
        "Lịch hẹn",
        eligible.map((a) => [a.id, `#${a.id} · ${fmt(a.start_at)}`]),
      ),
      selectField("rating", "Mức độ hài lòng", [
        [5, "★★★★★ · Rất hài lòng"],
        [4, "★★★★ · Hài lòng"],
        [3, "★★★ · Bình thường"],
        [2, "★★ · Chưa hài lòng"],
        [1, "★ · Không hài lòng"],
      ]),
      {
        name: "comment",
        label: "Chia sẻ trải nghiệm",
        type: "textarea",
        max: 1000,
        required: false,
        full: true,
      },
    ],
    "review.save",
    { appointment_id: Number(id) || eligible[0].id, rating: 5 },
  );
}
function confirmModal(title, message, fn) {
  submitHandler = async () => {
    await fn();
    await refresh();
    closeModal();
    toast("Đã thực hiện thành công.");
  };
  modal(
    title,
    `<form id="modal-form"><div class="form-error" role="alert"></div><p>${esc(message)}</p></form>`,
    `${btn("Quay lại", "close-modal")}<button class="btn primary" type="submit" form="modal-form">Xác nhận</button>`,
  );
}
function renderAuth() {
  csrf = "";
  closeModal();
  $("#app").innerHTML =
    `<div class="auth-shell"><section class="auth-story">${logo()}<div class="story-copy"><div class="eyebrow">YOUR SPACE TO CARE</div><h1>Chăm sóc từ<br>những điều<br><em>thật giản đơn.</em></h1><p>Một không gian kết nối con người, dịch vụ và những trải nghiệm chăm sóc trọn vẹn.</p></div><svg class="auth-plant" viewBox="0 0 150 200" fill="none" stroke="currentColor" stroke-width=".8"><path d="M75 200V20M75 75C10 80 0 38 10 0c48 0 70 34 65 75ZM75 75c-4-49 30-74 70-70 8 45-21 77-70 70ZM75 130C10 134 0 95 13 65c47 1 68 26 62 65ZM75 132c0-46 31-72 73-63 5 41-23 68-73 63ZM75 190C19 191 0 153 13 125c43 0 66 24 62 65ZM75 190c0-46 32-70 73-60 5 36-24 65-73 60Z"/></svg><footer>SPACARE · THOUGHTFULLY CONNECTED</footer></section><section class="auth-form-side"><div class="auth-form"><div class="eyebrow">${registerMode ? "BẮT ĐẦU HÀNH TRÌNH" : "CHÀO MỪNG TRỞ LẠI"}</div><h2>${registerMode ? "Tạo tài khoản của bạn" : "Đăng nhập SpaCare"}</h2><p class="intro">${registerMode ? "Đặt lịch và theo dõi quyền lợi chăm sóc của bạn." : "Đăng nhập để tiếp tục ngày chăm sóc của bạn."}</p><form id="auth-form"><div class="form-error" role="alert"></div>${registerMode ? field({ name: "full_name", label: "Họ và tên" }) + field({ name: "phone", label: "Số điện thoại", max: 10 }) + field({ name: "email", label: "Email", type: "email", required: false }) : ""}${field({ name: "username", label: "Tên đăng nhập" })}${field({ name: "password", label: registerMode ? "Mật khẩu (tối thiểu 8 ký tự)" : "Mật khẩu", type: "password" })}<button class="btn primary" type="submit">${registerMode ? "Tạo tài khoản" : "Đăng nhập"} ${icon("arrow")}</button></form><button class="switch" data-action="auth-switch">${registerMode ? "Đã có tài khoản? Đăng nhập" : "Bạn là khách hàng mới? Tạo tài khoản"}</button>${
      !registerMode
        ? `<div class="demo-note"><strong>KHÔNG GIAN DEMO</strong><br>Chọn vai trò để điền tài khoản mẫu. Mật khẩu: 123456<div class="demo-roles">${[
            ["admin", "Quản trị"],
            ["ql_hoa", "Quản lý"],
            ["letan01", "Lễ tân"],
            ["ktv_lan", "Kỹ thuật viên"],
            ["khach01", "Khách hàng"],
          ]
            .map(([id, label]) => `<button data-demo="${id}">${label}</button>`)
            .join("")}</div></div>`
        : ""
    }</div></section></div>`;
}
const handlers = {
  "close-modal": () => closeModal(),
  mobile: () => $(".sidebar")?.classList.toggle("open"),
  refresh: async () => {
    await refresh();
    toast("Đã cập nhật dữ liệu mới nhất.");
  },
  logout: async () => {
    await api("logout", {});
    user = null;
    data = {};
    page = "dashboard";
    renderAuth();
    toast("Đã đăng xuất.");
  },
  "auth-switch": () => {
    registerMode = !registerMode;
    renderAuth();
  },
  "go-appointments": () => {
    page = "appointments";
    search = "";
    tab = "";
    render();
  },
  booking: () => bookingForm(),
  "booking-service": (id) => bookingForm({ service_ids: [Number(id)] }),
  "booking-combo": (id) => bookingForm({ combo_id: Number(id) }),
  "booking-package": (id) => {
    const cp = data.customer_packages.find((p) => p.id == id);
    bookingForm({
      customer_id: cp.customer_id,
      customer_package_id: cp.id,
      service_ids: [cp.service_id],
    });
  },
  "booking-slots": async () => {
    const form = $("#modal-form"),
      a = readForm(form);
    if (!a.date) return toast("Chọn ngày trước.", true);
    const r = await action("appointment.slots", a);
    $("#slots").innerHTML = r.slots.length
      ? r.slots
          .map(
            (s) =>
              `<button class="btn sm" type="button" data-slot="${s}">${s}</button>`,
          )
          .join("")
      : '<p class="muted small">Không có giờ trống phù hợp. Thử đổi ngày, chi nhánh hoặc nhân viên.</p>';
  },
  "appointment-detail": (id) => appointmentDetail(id),
  "customer-detail": (id) => customerDetail(id),
  "customer-new": () => customerForm(),
  "customer-edit": (id) => customerForm(id),
  "service-new": () => serviceForm(),
  "service-edit": (id) => serviceForm(id),
  "staff-new": () => staffForm(),
  "staff-edit": (id) => staffForm(id),
  "package-new": () => packageForm(),
  "package-edit": (id) => packageForm(id),
  "combo-new": () => comboForm(),
  "combo-edit": (id) => comboForm(id),
  "promotion-new": () => promotionForm(),
  "promotion-edit": (id) => promotionForm(id),
  "product-new": () => productForm(),
  "product-edit": (id) => productForm(id),
  "tier-new": () => tierForm(),
  "tier-edit": (id) => tierForm(id),
  "account-new": () => accountForm(),
  "account-edit": (id) => accountForm(id),
  "branch-new": () => branchForm(),
  "branch-edit": (id) => branchForm(id),
  "room-new": () => roomForm(),
  "room-edit": (id) => roomForm(id),
  "customer-active": (id) => {
    const c = data.customers.find((c) => c.id == id);
    confirmModal(
      c.is_active ? "Ngừng hoạt động hồ sơ" : "Kích hoạt hồ sơ",
      c.is_active
        ? "Hồ sơ và lịch sử được giữ lại. Tài khoản khách sẽ tạm ngừng đăng nhập."
        : "Cho phép hồ sơ và tài khoản khách hoạt động trở lại.",
      () =>
        action("customer.active", { id: Number(id), is_active: !c.is_active }),
    );
  },
  "shift-new": () =>
    formModal(
      "Phân ca làm việc",
      [
        selectField("staff_id", "Nhân viên", staffOptions()),
        {
          name: "work_date",
          label: "Ngày làm việc",
          type: "date",
          value: today(),
        },
        {
          name: "start_time",
          label: "Giờ bắt đầu",
          type: "time",
          value: "08:00",
        },
        {
          name: "end_time",
          label: "Giờ kết thúc",
          type: "time",
          value: "17:00",
        },
      ],
      "shift.save",
    ),
  "shift-delete": (id) =>
    confirmModal(
      "Xóa ca làm",
      "Chỉ xóa được ca không có lịch hẹn đang giữ chỗ.",
      () => action("shift.delete", { id: Number(id) }),
    ),
  attendance: () =>
    manager()
      ? formModal(
          "Chấm công vào / ra",
          [selectField("staff_id", "Nhân viên", staffOptions())],
          "attendance.toggle",
        )
      : confirmModal(
          "Chấm công",
          "Ghi nhận giờ vào ca hoặc ra ca hôm nay của bạn?",
          () => action("attendance.toggle", {}),
        ),
  reschedule: (id) => rescheduleForm(id),
  cancel: (id) => cancelForm(id),
  checkin: (id) =>
    confirmModal(
      "Check-in khách hàng",
      "Xác nhận khách đã đến và sẵn sàng thực hiện dịch vụ?",
      () =>
        action("appointment.status", { id: Number(id), status: "CHECKED_IN" }),
    ),
  start: (id) =>
    confirmModal(
      "Bắt đầu dịch vụ",
      "Chuyển lịch sang trạng thái Đang thực hiện?",
      () =>
        action("appointment.status", { id: Number(id), status: "IN_PROGRESS" }),
    ),
  complete: (id) =>
    confirmModal(
      "Hoàn thành dịch vụ",
      "Xác nhận hoàn thành? Hệ thống sẽ trừ vật tư theo định mức.",
      () =>
        action("appointment.status", { id: Number(id), status: "COMPLETED" }),
    ),
  "no-show": (id) =>
    confirmModal(
      "Khách vắng mặt",
      "Chỉ đánh dấu khi đã hết giờ hẹn. Thao tác giải phóng buổi gói đang giữ.",
      () => action("appointment.status", { id: Number(id), status: "NO_SHOW" }),
    ),
  pay: (id) => paymentForm(id),
  "pay-preview": async (id) => {
    const r = await action(
      "invoice.preview",
      paymentArgs(readForm($("#modal-form")), id),
    );
    $("#payment-preview").innerHTML = summary(r);
  },
  "cart-add": () => {
    const line = document.createElement("div");
    line.className = "cart-line";
    line.innerHTML = `<select aria-label="Sản phẩm bán kèm"><option value="">Chọn sản phẩm</option>${productOptions()
      .map(([id, label]) => `<option value="${id}">${esc(label)}</option>`)
      .join(
        "",
      )}</select><input type="number" min="1" max="1000" value="1" aria-label="Số lượng"><button type="button" class="btn sm" data-action="cart-remove" aria-label="Bỏ sản phẩm">×</button>`;
    $("#cart-lines").append(line);
  },
  "cart-remove": (_id, el) => el.closest(".cart-line").remove(),
  "invoice-detail": (id) => invoiceDetail(id),
  print: () => window.print(),
  "package-sell": (id) =>
    formModal(
      "Bán gói liệu trình",
      [
        selectField("customer_id", "Khách hàng", customerOptions()),
        selectField(
          "package_id",
          "Gói liệu trình",
          data.packages
            .filter((p) => p.is_active)
            .map((p) => [p.id, `${p.name} · ${money(p.price)}`]),
        ),
        selectField("method", "Hình thức thu tiền", [
          ["CASH", "Tiền mặt"],
          ["CARD", "Thẻ (demo)"],
          ["TRANSFER", "Chuyển khoản (demo)"],
          ["QR", "QR (demo)"],
        ]),
      ],
      "package.sell",
      { package_id: Number(id) },
    ),
  "stock-adjust": () =>
    formModal(
      "Nhập / Xuất kho",
      [
        selectField("product_id", "Sản phẩm", productOptions()),
        numField("quantity", "Biến động (+ nhập / − xuất)", -1000000, 1000000),
        {
          name: "reason",
          label: "Lý do",
          type: "textarea",
          full: true,
          max: 250,
        },
      ],
      "stock.adjust",
    ),
  recipe: () =>
    formModal(
      "Cập nhật định mức",
      [
        selectField("service_id", "Dịch vụ", serviceOptions()),
        selectField("product_id", "Vật tư", productOptions()),
        {
          ...numField("quantity", "Số lượng mỗi lần thực hiện", 0, 1000000),
          hint: "Nhập 0 để bỏ định mức này.",
        },
      ],
      "recipe.save",
    ),
  "review-new": () => reviewForm(),
  "review-appointment": (id) => reviewForm(id),
  "notification-dispatch": async () => {
    const r = await action("notification.dispatch");
    await refresh();
    toast(`Đã mô phỏng gửi ${r.count} thông báo đến hạn.`);
  },
  "report-filter": async () => {
    reportFrom = $("#report-from").value;
    reportTo = $("#report-to").value;
    await refresh();
  },
  password: () =>
    formModal(
      "Đổi mật khẩu",
      [
        {
          name: "old_password",
          label: "Mật khẩu hiện tại",
          type: "password",
          full: true,
        },
        {
          name: "new_password",
          label: "Mật khẩu mới (tối thiểu 8 ký tự)",
          type: "password",
          full: true,
        },
      ],
      "password.change",
    ),
};
document.addEventListener("click", async (e) => {
  const nav = e.target.closest("[data-nav]");
  if (nav) {
    page = nav.dataset.nav;
    tab = "";
    search = "";
    render();
    return;
  }
  const t = e.target.closest("[data-tab]");
  if (t) {
    tab = t.dataset.tab;
    search = "";
    render();
    return;
  }
  const demo = e.target.closest("[data-demo]");
  if (demo) {
    $("#f-username").value = demo.dataset.demo;
    $("#f-password").value = "123456";
    return;
  }
  const slot = e.target.closest("[data-slot]");
  if (slot) {
    $("#f-start_time").value = slot.dataset.slot;
    $$("[data-slot]").forEach((b) => b.classList.remove("primary"));
    slot.classList.add("primary");
    return;
  }
  const el = e.target.closest("[data-action]");
  if (!el) return;
  if (el.closest("form") && el.tagName === "BUTTON") e.preventDefault();
  const fn = handlers[el.dataset.action];
  if (!fn) return;
  try {
    await fn(el.dataset.id, el);
  } catch (error) {
    const inline = $(".modal .form-error");
    if (inline) inline.textContent = error.message;
    else toast(error.message, true);
  }
});
document.addEventListener("submit", async (e) => {
  if (!["modal-form", "auth-form"].includes(e.target.id)) return;
  e.preventDefault();
  if (busy) return;
  busy = true;
  const form = e.target,
    buttons = $$("button[type=submit]");
  buttons.forEach((b) => (b.disabled = true));
  const error = $(".form-error", form);
  if (error) error.textContent = "";
  try {
    const values = readForm(form);
    if (form.id === "auth-form") {
      if (registerMode) {
        await api("register", values);
        registerMode = false;
        renderAuth();
        toast("Tạo tài khoản thành công. Bạn có thể đăng nhập.");
      } else {
        const r = await api("login", values);
        user = r.user;
        csrf = r.csrf;
        page = "dashboard";
        await refresh();
      }
    } else await submitHandler(values);
  } catch (ex) {
    if (error && document.contains(error)) error.textContent = ex.message;
    else toast(ex.message, true);
  } finally {
    busy = false;
    buttons.forEach((b) => (b.disabled = false));
  }
});
let searchTimer;
document.addEventListener("input", (e) => {
  if (e.target.id === "search") {
    search = e.target.value;
    const pos = e.target.selectionStart;
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      render();
      $("#search")?.focus();
      $("#search")?.setSelectionRange(pos, pos);
    }, 180);
  }
});
document.addEventListener("change", (e) => {
  const ids = {
    "appointment-date": "date",
    "appointment-status": "status",
    "branch-filter": "branch",
  };
  if (ids[e.target.id]) {
    if (e.target.id === "appointment-date") appointmentDate = e.target.value;
    if (e.target.id === "appointment-status")
      appointmentStatus = e.target.value;
    if (e.target.id === "branch-filter") branchFilter = e.target.value;
    render();
  }
  if (
    e.target.id === "f-group_count" &&
    Number(e.target.value) > 1 &&
    $("#f-staff_id")
  )
    $("#f-staff_id").value = "0";
  if (e.target.id === "f-customer_id" && $("#f-customer_package_id")) {
    const cid = Number(e.target.value);
    $("#f-customer_package_id").innerHTML =
      '<option value="0">Không dùng liệu trình</option>' +
      data.customer_packages
        .filter((p) => p.customer_id === cid && p.available_sessions > 0)
        .map(
          (p) =>
            `<option value="${p.id}">#${p.id} ${esc(p.name)} · còn ${p.available_sessions}</option>`,
        )
        .join("");
  }
  if (
    e.target.id === "f-combo_id" &&
    Number(e.target.value) > 0 &&
    $("#f-service_ids")
  )
    [...$("#f-service_ids").options].forEach((o) => (o.selected = false));
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") closeModal();
  if (e.key === "Tab" && $(".modal")) {
    const focusable = $$(
      "button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]",
      $(".modal"),
    );
    const first = focusable[0],
      last = focusable.at(-1);
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last?.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first?.focus();
    }
  }
});
(async () => {
  try {
    const me = await api("me");
    user = me.user;
    csrf = me.csrf;
    await refresh();
  } catch {
    renderAuth();
  }
})();

setInterval(async () => {
  if (
    !user ||
    busy ||
    document.visibilityState !== "visible" ||
    $(".modal") ||
    document.activeElement?.matches("input,select,textarea")
  )
    return;
  const y = window.scrollY;
  try {
    await refresh();
    window.scrollTo(0, y);
  } catch {}
}, 20000);
