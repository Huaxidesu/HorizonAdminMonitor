package dev.dsmnh.horizon.adminmonitor.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 命令参数里的游戏模式、时间、天气、难度转中文描述。 */
public final class GameNames {

    private static final Map<String, String> MODES = new HashMap<String, String>();
    private static final Map<String, String> TIMES = new HashMap<String, String>();
    private static final Map<String, String> WEATHERS = new HashMap<String, String>();
    private static final Map<String, String> DIFFICULTIES = new HashMap<String, String>();

    /** 状态效果编号到中文名，下标即编号，0 位留空。 */
    private static final String[] EFFECTS = {
            null, "速度", "缓慢", "急迫", "挖掘疲劳", "力量", "瞬间治疗", "瞬间伤害", "跳跃提升",
            "反胃", "生命恢复", "抗性提升", "抗火", "水下呼吸", "隐身", "失明", "夜视",
            "饥饿", "虚弱", "中毒", "凋零", "生命提升", "伤害吸收", "饱和", "发光", "漂浮",
            "幸运", "霉运"};

    static {
        put(MODES, "生存模式", "0", "survival", "s", "生存");
        put(MODES, "创造模式", "1", "creative", "c", "创造");
        put(MODES, "冒险模式", "2", "adventure", "a", "冒险");
        put(MODES, "旁观模式", "3", "spectator", "sp", "旁观");

        put(TIMES, "白天", "day");
        put(TIMES, "正午", "noon");
        put(TIMES, "夜晚", "night");
        put(TIMES, "午夜", "midnight");
        put(TIMES, "黎明", "dawn");
        put(TIMES, "黄昏", "dusk");

        put(WEATHERS, "晴天", "clear", "sun", "晴");
        put(WEATHERS, "雨天", "rain", "下雨");
        put(WEATHERS, "雷雨", "thunder", "storm", "雷");

        put(DIFFICULTIES, "和平", "0", "peaceful", "p");
        put(DIFFICULTIES, "简单", "1", "easy", "e");
        put(DIFFICULTIES, "普通", "2", "normal", "n");
        put(DIFFICULTIES, "困难", "3", "hard", "h");
    }

    private GameNames() {
    }

    public static String mode(String raw) {
        return translate(MODES, raw);
    }

    public static String time(String raw) {
        String named = TIMES.get(normalize(raw));
        if (named != null) {
            return named;
        }
        return raw == null || raw.isEmpty() ? "" : raw + " tick";
    }

    public static String weather(String raw) {
        return translate(WEATHERS, raw);
    }

    public static String difficulty(String raw) {
        return translate(DIFFICULTIES, raw);
    }

    /** 状态效果支持数字编号，其余写法原样返回。 */
    public static String effect(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!value.matches("\\d+")) {
            return value;
        }
        int id = Integer.parseInt(value);
        return id > 0 && id < EFFECTS.length && EFFECTS[id] != null ? EFFECTS[id] : value;
    }

    /** NEI 的模式编号与 Bukkit 不同：2 为扩展创造背包，3 为冒险。 */
    public static String neiMode(int mode) {
        switch (mode) {
            case 0:
                return "生存模式";
            case 1:
                return "创造模式";
            case 2:
                return "创造模式（扩展背包）";
            case 3:
                return "冒险模式";
            default:
                return String.valueOf(mode);
        }
    }

    /** NEI 的改时间按小时下发，换算成时段描述。 */
    public static String hour(int hour) {
        if (hour < 0 || hour > 23) {
            return hour + " 时";
        }
        if (hour >= 5 && hour < 7) {
            return "黎明（" + hour + " 时）";
        }
        if (hour >= 7 && hour < 12) {
            return "上午（" + hour + " 时）";
        }
        if (hour >= 12 && hour < 14) {
            return "正午（" + hour + " 时）";
        }
        if (hour >= 14 && hour < 18) {
            return "下午（" + hour + " 时）";
        }
        if (hour >= 18 && hour < 20) {
            return "黄昏（" + hour + " 时）";
        }
        return "夜晚（" + hour + " 时）";
    }

    private static String translate(Map<String, String> table, String raw) {
        String named = table.get(normalize(raw));
        return named != null ? named : (raw == null ? "" : raw);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static void put(Map<String, String> table, String value, String... keys) {
        for (String key : keys) {
            table.put(key, value);
        }
    }
}
