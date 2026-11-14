const { chromium } = require("playwright");
const { spawn } = require("child_process");
const path = require("path");
const fs = require("fs");
const root = path.resolve(__dirname, "..");
const logs = [];
const testOffset = 8 - new Date().getUTCHours();
const testZone =
  "GMT" +
  (testOffset >= 0 ? "+" : "-") +
  String(Math.abs(testOffset)).padStart(2, "0") +
  ":00";
const server = spawn(
  "java",
  [
    "-Duser.timezone=" + testZone,
    "-cp",
    path.join(root, "target/classes") +
      path.delimiter +
      path.join(root, "lib/h2-2.2.224.jar"),
    "spacare.web.SpaCareServer",
  ],
  {
    cwd: root,
    env: { ...process.env, SPACARE_DB: "mem:browser", PORT: "18090" },
  },
);
server.stderr.on("data", (b) => logs.push(b.toString()));
const ready = new Promise((resolve, reject) => {
  server.stdout.on("data", (b) => {
    if (b.toString().includes("đang chạy")) resolve();
  });
  server.on("exit", (code) =>
    reject(Error("Server exit " + code + logs.join(""))),
  );
  setTimeout(() => reject(Error("Server timeout")), 15000).unref();
});
let browser;
(async () => {
  await ready;
  const bundled = process.env.SPACARE_CHROMIUM_MODULE
    ? (
        await import(
          "file://" + process.env.SPACARE_CHROMIUM_MODULE + "/build/index.js"
        )
      ).default
    : null;
  browser = await chromium.launch({
    headless: true,
    executablePath: bundled
      ? await bundled.executablePath()
      : process.env.CHROMIUM_PATH || (fs.existsSync("/usr/bin/chromium") ? "/usr/bin/chromium" : undefined),
    args: [
      "--no-sandbox",
      ...(bundled ? ["--no-zygote", "--single-process", "--disable-gpu"] : []),
    ],
  });
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1050 },
  });
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("console", (m) => {
    if (
      m.type() === "error" &&
      !m.text().includes("401") &&
      !m.text().includes("favicon")
    )
      errors.push(m.text());
  });
  await page.goto("http://127.0.0.1:18090");
  await page.locator("#auth-form").waitFor();
  await page.screenshot({ path: "/tmp/spacare-login.png", fullPage: true });
  await page.locator("[data-demo=admin]").click();
  await page.locator("#auth-form button[type=submit]").click();
  await page.locator(".sidebar").waitFor();
  const actions = await page.evaluate(async () => {
    const me = await (await fetch("/api/me")).json();
    const req = async (a) => {
      const r = await fetch("/api/action", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": me.csrf,
        },
        body: JSON.stringify(a),
      });
      const v = await r.json();
      if (!r.ok) throw Error(v.error);
      return v;
    };
    const d = new Date();
    d.setDate(d.getDate() + 1);
    const tomorrow = d.toLocaleDateString("en-CA", {
      timeZone: "Asia/Ho_Chi_Minh",
    });
    for (const [cust, service, staff, t] of [
      [1, 1, 1, "09:00"],
      [2, 3, 2, "10:00"],
      [3, 4, 3, "11:00"],
    ])
      await req({
        action: "appointment.create",
        customer_id: cust,
        service_ids: [service],
        staff_id: staff,
        branch_id: 1,
        start_at: tomorrow + "T" + t,
        group_count: 1,
      });
    return true;
  });
  await page.reload();
  await page.locator(".sidebar").waitFor();
  await page.screenshot({ path: "/tmp/spacare-dashboard.png", fullPage: true });
  for (const nav of [
    "appointments",
    "services",
    "customers",
    "staff",
    "packages",
    "invoices",
    "promotions",
    "inventory",
    "reports",
    "notifications",
    "reviews",
    "accounts",
    "settings",
  ]) {
    await page.locator(`.sidebar [data-nav=${nav}]`).click();
    await page.locator("main h1").waitFor();
    if (
      await page
        .locator(".main")
        .textContent()
        .then((t) => t.includes("undefined") || t.includes("NaN"))
    )
      throw Error("Invalid render " + nav);
  }
  await page.locator(".sidebar [data-nav=services]").click();
  await page.getByRole("button", { name: "Thêm dịch vụ", exact: true }).click();
  await page.locator("#f-name").fill("Dịch vụ trình duyệt");
  await page.locator("#f-category").fill("Facial");
  await page.locator("#f-price").fill("350000");
  await page.locator("#f-duration_min").fill("30");
  await page.locator("button[form=modal-form]").click();
  await page.locator(".modal-overlay").waitFor({ state: "detached" });
  await page.getByRole("heading", { name: "Dịch vụ trình duyệt" }).waitFor();
  await page.screenshot({ path: "/tmp/spacare-services.png", fullPage: true });
  // Booking through the UI, including slot lookup.
  await page.locator(".sidebar [data-nav=appointments]").click();
  await page.getByRole("button", { name: "Đặt lịch mới", exact: true }).click();
  await page.locator("#f-customer_id").selectOption("1");
  await page.locator("#f-service_ids").selectOption(["1"]);
  await page.locator("#f-staff_id").selectOption("1");
  await page.getByRole("button", { name: "Tìm giờ trống" }).click();
  await page.locator("[data-slot]").first().waitFor();
  await page.locator("[data-slot]").first().click();
  await page.locator("button[form=modal-form]").click();
  await page.locator(".modal-overlay").waitFor({ state: "detached" });
  await page.locator("tbody tr").first().waitFor();
  await page.screenshot({
    path: "/tmp/spacare-appointments.png",
    fullPage: true,
  });
  await page
    .locator("tbody tr")
    .first()
    .getByRole("button", { name: "Chi tiết" })
    .click();
  await page.getByRole("button", { name: "Dời lịch", exact: true }).click();
  await page
    .locator("#f-start_at")
    .fill(
      new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10) + "T09:00",
    );
  await page.locator("button[form=modal-form]").click();
  await page.locator(".modal-overlay").waitFor({ state: "detached" });
  // Full service lifecycle and payment through UI controls.
  const appt = await page.evaluate(async () => {
    const me = await (await fetch("/api/me")).json();
    const state = await (await fetch("/api/state")).json();
    const r = await fetch("/api/action", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-CSRF-Token": me.csrf },
      body: JSON.stringify({
        action: "appointment.create",
        customer_id: 1,
        service_ids: [1],
        staff_id: 4,
        branch_id: 2,
        start_at: state.server_time.slice(0, 10) + "T09:00",
      }),
    });
    const v = await r.json();
    if (!r.ok) throw Error(v.error);
    return v.ids[0];
  });
  await page.reload();
  await page.locator(".sidebar").waitFor();
  await page.locator(".sidebar [data-nav=appointments]").click();
  const appointmentRow = () =>
    page.locator("tbody tr").filter({ hasText: "#" + appt + " ·" });
  for (const label of ["Check-in", "Bắt đầu dịch vụ", "Hoàn thành"]) {
    await appointmentRow().getByRole("button", { name: "Chi tiết" }).click();
    await page.getByRole("button", { name: label, exact: true }).click();
    await page.locator("button[form=modal-form]").click();
    await page.locator(".modal-overlay").waitFor({ state: "detached" });
  }
  await appointmentRow().getByRole("button", { name: "Chi tiết" }).click();
  await page.getByRole("button", { name: "Thanh toán", exact: true }).click();
  await page.locator("#f-promotion_code").fill("SPACARE10");
  await page
    .getByRole("button", { name: "Thêm sản phẩm", exact: true })
    .click();
  await page.locator(".cart-line select").selectOption("2");
  await page.locator("#f-retail_staff_id").selectOption("4");
  await page.getByRole("button", { name: "Tính hóa đơn", exact: true }).click();
  await page.locator("#payment-preview .total").waitFor();
  if (
    !(await page.locator("#payment-preview .total").textContent()).includes(
      "720.000",
    )
  )
    throw Error("Incorrect UI invoice quote");
  await page.locator("button[form=modal-form]").click();
  await page.locator(".modal-overlay").waitFor({ state: "detached" });
  await appointmentRow().getByRole("button", { name: "Chi tiết" }).click();
  await page.getByRole("button", { name: "Xem hóa đơn", exact: true }).click();
  await page.locator(".print-invoice").waitFor();
  await page.screenshot({ path: "/tmp/spacare-invoice.png", fullPage: true });
  await page.pdf({
    path: "/tmp/spacare-invoice.pdf",
    format: "A4",
    printBackground: true,
  });
  await page.getByRole("button", { name: "Đóng", exact: true }).click();
  // Mobile layout and navigation.
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: "/tmp/spacare-mobile.png", fullPage: true });
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > innerWidth + 1,
  );
  if (overflow) throw Error("Mobile page horizontally overflows");
  await page.getByRole("button", { name: "Mở menu" }).click();
  await page.locator(".sidebar.open").waitFor();
  await page.locator(".sidebar [data-nav=services]").click();
  if (await page.locator(".sidebar.open").count())
    throw Error("Mobile nav did not close");
  await page.setViewportSize({ width: 1440, height: 1050 });
  await page.locator("[data-action=logout]").click();
  await page.locator("#auth-form").waitFor();
  for (const role of ["ql_hoa", "letan01", "ktv_lan", "khach01"]) {
    await page.locator(`[data-demo=${role}]`).click();
    await page.locator("#auth-form button[type=submit]").click();
    await page.locator(".sidebar").waitFor();
    for (const nav of await page
      .locator(".sidebar [data-nav]")
      .evaluateAll((nodes) => nodes.map((n) => n.dataset.nav))) {
      await page.locator(`.sidebar [data-nav=${nav}]`).click();
      await page.locator("main h1").waitFor();
    }
    if (role === "khach01") {
      await page.locator(".sidebar [data-nav=reviews]").click();
      await page
        .getByRole("button", { name: "Viết đánh giá", exact: true })
        .click();
      await page.locator("#f-comment").fill("Trải nghiệm chăm sóc rất tốt.");
      await page.locator("button[form=modal-form]").click();
      await page.locator(".modal-overlay").waitFor({ state: "detached" });
      await page.getByText("Trải nghiệm chăm sóc rất tốt.").waitFor();
    }
    await page.locator("[data-action=logout]").click();
    await page.locator("#auth-form").waitFor();
  }
  if (errors.length) throw Error(errors.join("\n"));
  console.log(
    "PASS browser: all roles/pages, service creation, booking, slots, reschedule, check-in/start/complete, payment/cart/voucher, invoice print, customer review, mobile nav, no JS errors.",
  );
})()
  .catch((e) => {
    console.error(e);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (browser) await browser.close();
    server.kill("SIGTERM");
  });
