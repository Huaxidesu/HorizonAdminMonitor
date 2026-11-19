package dev.dsmnh.horizon.adminmonitor.rule;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import dev.dsmnh.horizon.adminmonitor.util.GameNames;
import dev.dsmnh.horizon.adminmonitor.util.ItemNames;
import dev.dsmnh.horizon.adminmonitor.watch.CommandCall;

/** 内置规则表与别名索引。 */
public final class RuleTable {

    private final Map<String, CommandRule> byKey = new LinkedHashMap<String, CommandRule>();
    private final Map<String, CommandRule> byAlias = new LinkedHashMap<String, CommandRule>();

    private RuleTable() {
    }

    public static RuleTable create() {
        RuleTable table = new RuleTable();

        table.add(new CommandRule("give", new String[] {"give"},
                "&f%actor% &7给予了 &b%target% &f%item% &7×&e%amount%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("target", call.arg(0));
                        out.put("item", ItemNames.resolve(call.arg(1)));
                        out.put("amount", call.args().length > 2 ? call.arg(2) : "1");
                    }
                }));

        table.add(new CommandRule("item", new String[] {"i", "item", "more"},
                "&f%actor% &7取出了 &f%item% &7×&e%amount%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("target", call.actor());
                        out.put("item", ItemNames.resolve(call.arg(0)));
                        out.put("amount", call.args().length > 1 ? call.arg(1) : "1");
                    }
                }));

        table.add(new CommandRule("gamemode", new String[] {"gamemode", "gm"},
                "&f%actor% &7将 &b%target% &7的游戏模式改为 &e%mode%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("mode", GameNames.mode(call.arg(0)));
                        out.put("target", call.args().length > 1 ? call.arg(1) : call.actor());
                    }
                }));

        table.add(new CommandRule("time", new String[] {"time"},
                "&f%actor% &7将时间%time-action% &e%time%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        String first = call.arg(0).toLowerCase(Locale.ROOT);
                        boolean add = "add".equals(first);
                        if ((add || "set".equals(first)) && call.args().length > 1) {
                            out.put("time", GameNames.time(call.arg(1)));
                        } else {
                            out.put("time", GameNames.time(call.arg(0)));
                        }
                        out.put("time-action", add ? "推进" : "设为");
                    }
                }));

        table.add(new CommandRule("weather", new String[] {"weather"},
                "&f%actor% &7将天气改为 &e%weather%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("weather", GameNames.weather(call.arg(0)));
                    }
                }));

        table.add(new CommandRule("effect", new String[] {"effect"},
                "&f%actor% &7对 &b%target% &7施加了效果 &e%effect%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("target", call.arg(0));
                        out.put("effect", GameNames.effect(call.arg(1)));
                        out.put("duration", call.arg(2));
                    }
                }));

        table.add(new CommandRule("enchant", new String[] {"enchant"},
                "&f%actor% &7为 &b%target% &7附魔 &e%enchant% &7等级 &e%level%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("target", call.arg(0));
                        out.put("enchant", call.arg(1));
                        out.put("level", call.args().length > 2 ? call.arg(2) : "1");
                    }
                }));

        table.add(new CommandRule("xp", new String[] {"xp", "exp"},
                "&f%actor% &7给予 &b%target% &e%amount% &7点经验",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("amount", call.arg(0));
                        out.put("target", call.args().length > 1 ? call.arg(1) : call.actor());
                    }
                }));

        table.add(new CommandRule("summon", new String[] {"summon", "spawnmob"},
                "&f%actor% &7召唤了 &e%mob%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("mob", call.arg(0));
                    }
                }));

        table.add(new CommandRule("tp", new String[] {"tp", "tppos", "tphere", "tpall"},
                "&f%actor% &7执行了传送 &b%target%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        String[] args = call.args();
                        String target;
                        if (args.length >= 4) {
                            target = args[0] + " 至坐标 " + args[1] + " " + args[2] + " " + args[3];
                        } else if (args.length == 3) {
                            target = "坐标 " + args[0] + " " + args[1] + " " + args[2];
                        } else if (args.length == 2) {
                            target = args[0] + " → " + args[1];
                        } else {
                            target = call.arg(0);
                        }
                        out.put("target", target);
                    }
                }));

        table.add(new CommandRule("difficulty", new String[] {"difficulty", "diff"},
                "&f%actor% &7将难度改为 &e%difficulty%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("difficulty", GameNames.difficulty(call.arg(0)));
                    }
                }));

        table.add(new CommandRule("gamerule", new String[] {"gamerule"},
                "&f%actor% &7修改了规则 &e%arg1% &7= &e%arg2%", null));

        table.add(new CommandRule("op", new String[] {"op", "deop"},
                "&f%actor% &7变更了管理员身份：&b%target%",
                new CommandRule.Parser() {
                    @Override
                    public void parse(CommandCall call, Map<String, String> out) {
                        out.put("target", call.arg(0));
                    }
                }));

        table.add(new CommandRule("essentials", new String[] {"heal", "feed", "fly", "god", "repair"},
                "&f%actor% &7执行了 &e/%label%", null));

        table.add(new CommandRule("generic", new String[0],
                "&f%actor% &7执行了 &e/%label% %args%", null));

        return table;
    }

    public CommandRule byLabel(String label) {
        return byAlias.get(label.toLowerCase(Locale.ROOT));
    }

    public CommandRule byKey(String key) {
        return byKey.get(key);
    }

    public CommandRule generic() {
        return byKey.get("generic");
    }

    public Collection<CommandRule> all() {
        return Collections.unmodifiableCollection(byKey.values());
    }

    private void add(CommandRule rule) {
        byKey.put(rule.key(), rule);
        for (String alias : rule.aliases()) {
            byAlias.put(alias.toLowerCase(Locale.ROOT), rule);
        }
    }
}
