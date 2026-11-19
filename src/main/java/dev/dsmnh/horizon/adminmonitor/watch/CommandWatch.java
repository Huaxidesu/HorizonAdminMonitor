package dev.dsmnh.horizon.adminmonitor.watch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import dev.dsmnh.horizon.adminmonitor.HorizonAdminMonitor;
import dev.dsmnh.horizon.adminmonitor.PluginConfig;
import dev.dsmnh.horizon.adminmonitor.rule.CommandRule;

/** 命令播报入口：玩家命令与控制台命令。 */
public final class CommandWatch implements Listener {

    private final HorizonAdminMonitor plugin;

    public CommandWatch(HorizonAdminMonitor plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            if (plugin.config().debug()) {
                plugin.getLogger().info("[debug] 命令事件被其它插件取消：" + event.getMessage());
            }
            return;
        }
        handle(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        handleConsole(event.getCommand());
    }

    /** 控制台与 RCON 命令的统一入口。 */
    void handleConsole(String command) {
        if (plugin.config().watchConsole()) {
            handle(null, command);
        }
    }

    private void handle(Player player, String rawLine) {
        PluginConfig config = plugin.config();
        if (config.debug()) {
            plugin.getLogger().info("[debug] 收到命令：" + rawLine
                    + "（" + (player == null ? "控制台" : player.getName() + "/op=" + player.isOp()) + "）");
        }
        if (!config.enabled()) {
            return;
        }

        String line = rawLine == null ? "" : rawLine.trim();
        while (line.startsWith("/")) {
            line = line.substring(1).trim();
        }
        if (line.isEmpty()) {
            return;
        }

        String[] tokens = line.split("\\s+");
        String label = tokens[0].toLowerCase(Locale.ROOT);

        CommandRule rule = plugin.rules().byLabel(label);
        if (rule == null) {
            if (!config.extraCommands().contains(label)) {
                if (config.debug()) {
                    plugin.getLogger().info("[debug] 未匹配任何规则，跳过：" + label);
                }
                return;
            }
            rule = plugin.rules().generic();
        }
        if (!config.ruleEnabled(rule.key())) {
            if (config.debug()) {
                plugin.getLogger().info("[debug] 规则已关闭：" + rule.key());
            }
            return;
        }
        if (player != null && config.isExcluded(player)) {
            if (config.debug()) {
                plugin.getLogger().info("[debug] 执行者在排除名单：" + player.getName());
            }
            return;
        }
        if (player != null && !mayUse(player, label)) {
            if (config.debug()) {
                plugin.getLogger().info("[debug] 权限判定未通过：" + player.getName() + " / " + label);
            }
            return;
        }

        String[] args = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, args, 0, args.length);

        String actor = player == null ? config.consoleName() : player.getName();
        CommandCall call = new CommandCall(player, actor, line, label, args);

        Map<String, String> placeholders = new HashMap<String, String>();
        putBasePlaceholders(placeholders, call);
        try {
            rule.parse(call, placeholders);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("解析命令参数失败：" + line + "（" + e + "）");
        }

        plugin.broadcaster().broadcast(config.template(rule), placeholders, player);
    }

    /**
     * 判断玩家是否真的能执行该命令。
     *
     * <p>命令失败时服务端只回一句无权限提示，但命令文本仍然到达事件，若不判定，
     * 普通玩家输入 {@code /give} 也能触发一条与实际效果不符的播报。</p>
     *
     * <p>Essentials 一类的插件不把权限节点声明在 Bukkit 命令上，{@code testPermissionSilent}
     * 恒为真，因此再按「插件名.命令名」推断一次（如 {@code essentials.gamemode}）。
     * 判定过严时可在配置中关闭 {@code require-permission}。</p>
     */
    private boolean mayUse(Player player, String label) {
        if (!plugin.config().requirePermission() || player.isOp()) {
            return true;
        }
        PluginCommand command = plugin.getServer().getPluginCommand(label);
        if (command == null) {
            // 原版命令没有对应的 Bukkit 命令对象，非 op 执行不会生效
            return false;
        }
        String declared = command.getPermission();
        if (declared != null && !declared.isEmpty()) {
            return player.hasPermission(declared);
        }
        String node = command.getPlugin().getName().toLowerCase(Locale.ROOT)
                + "." + command.getName().toLowerCase(Locale.ROOT);
        return player.hasPermission(node);
    }

    private void putBasePlaceholders(Map<String, String> out, CommandCall call) {        out.put("actor", call.actor());
        out.put("label", call.label());
        out.put("command", call.raw());
        out.put("args", call.arguments());
        for (int i = 0; i < 6; i++) {
            out.put("arg" + (i + 1), call.arg(i));
        }
    }
}
