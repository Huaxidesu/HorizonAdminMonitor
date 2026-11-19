package dev.dsmnh.horizon.adminmonitor.watch;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.RemoteServerCommandEvent;

/**
 * RCON 命令播报。
 *
 * <p>Thermos 系服务端把 RCON 命令派发为 {@code RemoteServerCommandEvent}，它自带独立的
 * HandlerList，注册在父类 {@code ServerCommandEvent} 上的监听器收不到，必须单独注册。
 * 该事件只存在于 Thermos 及其分支，其它服务端上本类不会被加载。</p>
 */
public final class RemoteCommandWatch implements Listener {

    private final CommandWatch delegate;

    public RemoteCommandWatch(CommandWatch delegate) {
        this.delegate = delegate;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        delegate.handleConsole(event.getCommand());
    }
}
