package dev.dsmnh.horizon.adminmonitor.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import dev.dsmnh.horizon.adminmonitor.HorizonAdminMonitor;

/** 唯一注册的命令：{@code /hadmin reload}。 */
public final class ReloadCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "horizonadminmonitor.admin";
    private static final String RELOAD = "reload";

    private final HorizonAdminMonitor plugin;

    public ReloadCommand(HorizonAdminMonitor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("缺少权限 " + PERMISSION);
            return true;
        }
        if (args.length != 1 || !RELOAD.equalsIgnoreCase(args[0])) {
            sender.sendMessage("用法：/" + label + " " + RELOAD);
            return true;
        }

        plugin.reload();
        sender.sendMessage("[HorizonAdminMonitor] 配置已重新加载");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(PERMISSION) && RELOAD.startsWith(args[0].toLowerCase())) {
            List<String> result = new ArrayList<String>();
            result.add(RELOAD);
            return result;
        }
        return Collections.emptyList();
    }
}
