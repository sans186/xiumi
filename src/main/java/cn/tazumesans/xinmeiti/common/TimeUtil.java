package cn.tazumesans.xinmeiti.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** SQLite 里时间统一存成 'yyyy-MM-dd HH:mm:ss' 文本，可直接排序、可直接比较 */
public final class TimeUtil {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeUtil() {
    }

    public static String now() {
        return LocalDateTime.now().format(FMT);
    }

    public static String format(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }

    public static LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(s.trim(), FMT);
    }

    /** 距离现在还剩多少毫秒（负数表示已过期） */
    public static long remainMillis(String deadline) {
        LocalDateTime t = parse(deadline);
        if (t == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(LocalDateTime.now(), t).toMillis();
    }

    public static boolean expired(String deadline) {
        return remainMillis(deadline) <= 0;
    }

    /** 本月初 */
    public static String monthStart() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay().format(FMT);
    }

    /** 人性化剩余时间 */
    public static String humanRemain(String deadline) {
        long ms = remainMillis(deadline);
        if (ms == Long.MAX_VALUE) {
            return "无期限";
        }
        if (ms <= 0) {
            return "已超时";
        }
        long min = ms / 60000;
        if (min < 60) {
            return "剩 " + min + " 分钟";
        }
        long hour = min / 60;
        if (hour < 24) {
            return "剩 " + hour + " 小时 " + (min % 60) + " 分";
        }
        long day = hour / 24;
        return "剩 " + day + " 天 " + (hour % 24) + " 小时";
    }
}
