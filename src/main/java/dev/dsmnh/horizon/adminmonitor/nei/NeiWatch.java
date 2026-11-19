package dev.dsmnh.horizon.adminmonitor.nei;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import dev.dsmnh.horizon.adminmonitor.HorizonAdminMonitor;
import dev.dsmnh.horizon.adminmonitor.PluginConfig;
import dev.dsmnh.horizon.adminmonitor.util.GameNames;
import dev.dsmnh.horizon.adminmonitor.util.ItemNames;

/**
 * NEI 作弊面板的播报。
 *
 * <p>NEI 的取物、改时间、治疗等操作走自定义通道 {@code NEI}，不经过 Bukkit 命令系统，
 * 因此在网络层读取负载。包监听在网络线程触发，播报调度回主线程执行。</p>
 *
 * <p>NEI 自身已向全体玩家广播改时间与切换天气（{@code nei.chat.time}、{@code nei.chat.rain}），
 * 这两类动作的模板默认为空，避免重复刷屏。</p>
 */
public final class NeiWatch {

    private static final String CHANNEL = "NEI";
    private static final int COMPRESSED_FLAG = 0x80;

    private NeiWatch() {
    }

    public static void start(final HorizonAdminMonitor owner) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(owner, ListenerPriority.MONITOR, PacketType.Play.Client.CUSTOM_PAYLOAD) {

                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        handle(owner, event);
                    }
                });
    }

    private static void handle(HorizonAdminMonitor plugin, PacketEvent event) {
        try {
            doHandle(plugin, event);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("解析 NEI 包失败：" + e);
        }
    }

    private static void doHandle(HorizonAdminMonitor plugin, PacketEvent event) {
        PluginConfig config = plugin.config();
        if (!config.neiEnabled() || event.isCancelled()) {
            return;
        }

        PacketContainer packet = event.getPacket();
        String channel;
        byte[] data;
        try {
            channel = packet.getStrings().read(0);
            data = packet.getByteArrays().read(0);
        } catch (RuntimeException e) {
            return;
        }
        if (!CHANNEL.equals(channel) || data == null || data.length == 0) {
            if (config.debug() && data != null) {
                plugin.getLogger().info("[debug] 自定义包：通道=" + channel + "，长度=" + data.length);
            }
            return;
        }
        if (config.debug()) {
            plugin.getLogger().info("[debug] NEI 包：长度=" + data.length
                    + "，类型=" + (data[0] & 0xFF) + "，玩家=" + event.getPlayer());
        }
        if ((data[0] & COMPRESSED_FLAG) != 0) {
            if (config.debug()) {
                plugin.getLogger().info("[debug] NEI 包为压缩包，跳过");
            }
            return;
        }

        NeiPacket action = NeiPacket.parse(data);
        if (action == null) {
            if (config.debug()) {
                plugin.getLogger().info("[debug] NEI 包未解析，原始字节：" + hex(data));
            }
            return;
        }

        final Player player = event.getPlayer();
        if (player == null || config.isExcluded(player)) {
            return;
        }
        if (!allowed(player, action)) {
            if (config.debug()) {
                plugin.getLogger().info("[debug] NEI 鉴权未通过：" + action.action());
            }
            return;
        }

        final String template = templateFor(config, action);
        if (template.isEmpty()) {
            if (config.debug()) {
                plugin.getLogger().info("[debug] NEI 动作模板为空：" + action.type());
            }
            return;
        }

        final Map<String, String> placeholders = placeholders(player, action);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {

            @Override
            public void run() {
                if (action.type() == NeiPacket.TOGGLE_RAIN) {
                    // 包已在本 tick 生效，这里读到的是切换后的状态
                    placeholders.put("weather", player.getWorld().hasStorm() ? "雨天" : "晴天");
                }
                plugin.broadcaster().broadcast(template, placeholders, player);
            }
        });
    }

    private static String hex(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length && i < 32; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", data[i] & 0xFF));
        }
        return builder.toString();
    }

    private static String templateFor(PluginConfig config, NeiPacket action) {
        switch (action.type()) {
            case NeiPacket.GIVE_ITEM:
                return config.neiTemplate("give");
            case NeiPacket.SET_TIME:
                return config.neiTemplate("time");
            case NeiPacket.HEAL:
                return config.neiTemplate("heal");
            case NeiPacket.TOGGLE_RAIN:
                return config.neiTemplate("weather");
            case NeiPacket.SET_GAME_MODE:
                return config.neiTemplate("gamemode");
            case NeiPacket.MODIFY_ENCHANTMENT:
                return config.neiTemplate("enchant");
            default:
                return "";
        }
    }

    private static Map<String, String> placeholders(Player player, NeiPacket action) {
        Map<String, String> out = new HashMap<String, String>();
        out.put("actor", player.getName());
        switch (action.type()) {
            case NeiPacket.GIVE_ITEM:
                out.put("item", ItemNames.resolve(action.itemId(), action.meta()));
                out.put("amount", String.valueOf(action.count()));
                break;
            case NeiPacket.SET_TIME:
                out.put("time", GameNames.hour(action.value()));
                break;
            case NeiPacket.SET_GAME_MODE:
                out.put("mode", GameNames.neiMode(action.value()));
                break;
            case NeiPacket.MODIFY_ENCHANTMENT:
                out.put("enchant", String.valueOf(action.value()));
                out.put("level", String.valueOf(action.extra()));
                break;
            default:
                break;
        }
        return out;
    }

    /**
     * 确认 NEI 会执行该动作。
     *
     * <p>NEI 在 {@code NEIServerConfig.authenticatePacket} 中按玩家与动作名判定权限，
     * 被拒绝的包不会产生任何效果。反射失败时按允许处理，避免漏报。</p>
     */
    private static boolean allowed(Player player, NeiPacket action) {
        String name = action.action();
        if (name.isEmpty()) {
            return true;
        }
        try {
            Class<?> config = Class.forName("codechicken.nei.NEIServerConfig");
            Method method = config.getMethod("canPlayerPerformAction", String.class, String.class);
            Object result = method.invoke(null, player.getName(), name);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return true;
        }
    }
}
