package spacare;


/** DEMO 1 – UC-01: đăng nhập, phân quyền, khóa tài khoản. */
public class LoginDemo {
    public static void main(String[] a) throws Exception {
        DemoUtil.init("DEMO 1 – ĐĂNG NHẬP VÀ PHÂN QUYỀN (UC-01)");
        DemoUtil.step("Đăng nhập letan01 / 123456 (đúng)", () -> DemoUtil.say("Vai trò: " + Auth.login("letan01", "123456")));
        DemoUtil.step("Đăng nhập ktv_lan / 123456 (kỹ thuật viên)", () -> DemoUtil.say("Vai trò: " + Auth.login("ktv_lan", "123456") + "  -> không có quyền thanh toán (menu 7 của Main)"));
        DemoUtil.step("Đăng nhập ql_hoa / 123456 (quản lý)", () -> DemoUtil.say("Vai trò: " + Auth.login("ql_hoa", "123456")));
        DemoUtil.step("Đăng nhập admin với mật khẩu sai, lặp lại 5 lần", () -> {
            for (int i = 1; i <= 5; i++) DemoUtil.say("Lần " + i + ": " + (Auth.login("admin", "sai") == null ? "sai mật khẩu" : "?"));
        });
        DemoUtil.step("Đăng nhập admin với mật khẩu ĐÚNG sau khi đã sai 5 lần", () -> DemoUtil.say("Vai trò: " + Auth.login("admin", "123456")));
        DemoUtil.step("Đăng nhập tài khoản không tồn tại", () -> DemoUtil.say("Kết quả: " + (Auth.login("khong_ton_tai", "123456") == null ? "null (từ chối)" : "?")));
    }
}
