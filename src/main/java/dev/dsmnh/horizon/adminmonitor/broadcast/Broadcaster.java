package dev.dsmnh.horizon.adminmonitor.broadcast;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import dev.dsmnh.horizon.adminmonitor.HorizonAdminMonitor;
import dev.dsmnh.horizon.adminmonitor.PluginConfig;
import dev.dsmnh.horizon.adminmonitor.util.Texts;

/** 统一发送播报：替换占位符、加前缀、发给在线玩家与控制台。 */
public final class Broadcaster {

    private final HorizonAdminMonitor plugin;

    public Broadcaster(HorizonAdminMonitor plugin) {
        this.plugin = plugin;
    }

    /**
     * 发送一条播报。
     *
     * @param template     含 {@code %占位符%} 与 {@code &} 颜色码的模板
     * @param placeholders 占位符取值，值为 {@code null} 时按空串处理
     * @param actor        执行者，用于跳过本人；控制台执行时为 {@code null}
     */
    public void broadcast(String template, Map<String, String> placeholders, Player actor) {
        PluginConfig config = plugin.config();
        String body = apply(template == null ? "" : template, placeholders);
        String line = Texts.color(config.prefix() + body);
        String plain = ChatColor.stripColor(line);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!config.debug() && actor != null && online.getUniqueId().equals(actor.getUniqueId())) {
                continue;
            }
            online.sendMessage(line);
        }
        Bukkit.getConsoleSender().sendMessage(plain);
    }

    /** 替换已知占位符；模板中未被替换的占位符原样保留。 */
    static String apply(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("%" + entry.getKey() + "%", value);
        }
        return result;
    }
}
