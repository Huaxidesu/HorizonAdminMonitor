package dev.dsmnh.horizon.adminmonitor.util;

import org.bukkit.ChatColor;

/** 颜色码转换与文本处理。 */
public final class Texts {

    private Texts() {
    }

    /** 把 {@code &} 颜色码转换为 § 颜色码。 */
    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    /** 去掉颜色码，用于写入日志。 */
    public static String plain(String text) {
        return ChatColor.stripColor(color(text));
    }
}
