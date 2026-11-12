package spacare;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.*;

/** Tiện ích dùng chung cho menu và demo kịch bản. */
final class DemoUtil {
    interface Step { void run() throws Exception; }

    static void init(String title) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Db.start();
        System.out.println("==================== " + title + " ====================");
        System.out.println("(Ngày mặc định khi đặt lịch: " + LocalDate.now().plusDays(1) + " - tức ngày mai)");
    }
    static LocalDateTime at(int h, int m) { return LocalDate.now().plusDays(1).atTime(h, m); }
    static void step(String desc, Step s) {
        System.out.println("\n> " + desc);
        try { s.run(); } catch (Exception e) { System.out.println("  [BỊ TỪ CHỐI] " + (e.getMessage() == null ? e.toString() : e.getMessage())); }
    }
    static void say(String s) { System.out.println("  " + s); }
}
