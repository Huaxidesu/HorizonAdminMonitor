package dev.dsmnh.horizon.adminmonitor.watch;

import org.bukkit.entity.Player;

/** 一次命令调用：执行者、命令名与参数。 */
public final class CommandCall {

    private final Player player;
    private final String actor;
    private final String raw;
    private final String label;
    private final String[] args;

    public CommandCall(Player player, String actor, String raw, String label, String[] args) {
        this.player = player;
        this.actor = actor;
        this.raw = raw;
        this.label = label;
        this.args = args;
    }

    /** 玩家执行时返回该玩家，控制台执行时返回 {@code null}。 */
    public Player player() {
        return player;
    }

    public String actor() {
        return actor;
    }

    public String raw() {
        return raw;
    }

    public String label() {
        return label;
    }

    public String[] args() {
        return args;
    }

    /** 取第 {@code index} 个参数，越界返回空串。 */
    public String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : "";
    }

    /** 参数按单个空格拼接，无参数时返回空串。 */
    public String arguments() {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
