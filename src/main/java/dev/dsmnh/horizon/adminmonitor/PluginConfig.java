package dev.dsmnh.horizon.adminmonitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import dev.dsmnh.horizon.adminmonitor.rule.CommandRule;

/** {@code config.yml} 的只读视图，重载时整体替换。 */
public final class PluginConfig {

    /** 单条规则的开关与模板覆盖。 */
    public static final class RuleSettings {

        private final boolean enabled;
        private final String template;

        RuleSettings(boolean enabled, String template) {
            this.enabled = enabled;
            this.template = template;
        }

        public boolean enabled() {
            return enabled;
        }

        public String template() {
            return template;
        }
    }

    private final boolean enabled;
    private final String prefix;
    private final String consoleName;
    private final boolean watchConsole;
    private final boolean requirePermission;
    private final boolean debug;
    private final Set<String> exclude;
    private final Map<String, RuleSettings> rules;
    private final Set<String> extraCommands;
    private final boolean neiEnabled;
    private final Map<String, String> neiTemplates;

    private PluginConfig(FileConfiguration file) {
        this.enabled = file.getBoolean("enabled", true);
        this.prefix = file.getString("prefix", "&8[&6红扳手之神&8] &r");
        this.consoleName = file.getString("console-name", "控制台");
        this.watchConsole = file.getBoolean("watch-console", true);
        this.requirePermission = file.getBoolean("require-permission", true);
        this.debug = file.getBoolean("debug", false);
        this.exclude = readExclude(file.getStringList("exclude"));
        this.rules = readRules(file.getConfigurationSection("commands"));
        this.extraCommands = readLowercase(file.getStringList("extra-commands"));
        this.neiEnabled = file.getBoolean("nei.enabled", true);
        this.neiTemplates = readNeiTemplates(file);
    }

    public static PluginConfig read(FileConfiguration file) {
        return new PluginConfig(file);
    }

    public boolean enabled() {
        return enabled;
    }

    public String prefix() {
        return prefix;
    }

    public String consoleName() {
        return consoleName;
    }

    public boolean watchConsole() {
        return watchConsole;
    }

    /** 是否要求执行者真的能用该命令，避免普通玩家输入后产生假播报。 */
    public boolean requirePermission() {
        return requirePermission;
    }

    /**
     * 调试模式：执行者本人也接收自己操作的播报，并把收到的命令与 NEI 包写入控制台。
     *
     * <p>关闭时执行者看不到自己的播报，只有其他玩家能看到，符合「让玩家监督管理员」的用法。</p>
     */
    public boolean debug() {
        return debug;
    }

    public Set<String> extraCommands() {
        return extraCommands;
    }

    /** 排除名单条目数，启动日志用。 */
    public int excludedCount() {
        return exclude.size();
    }

    public boolean neiEnabled() {
        return neiEnabled;
    }

    /** 规则是否启用；配置中未出现的规则按启用处理。 */
    public boolean ruleEnabled(String key) {
        RuleSettings settings = rules.get(key);
        return settings == null || settings.enabled();
    }

    /** 取模板：配置里的非空模板优先，否则用内置默认模板。 */
    public String template(CommandRule rule) {
        RuleSettings settings = rules.get(rule.key());
        if (settings != null && settings.template() != null && !settings.template().isEmpty()) {
            return settings.template();
        }
        return rule.defaultTemplate();
    }

    /** 取 NEI 播报模板，返回空串表示该动作不播报。 */
    public String neiTemplate(String action) {
        String template = neiTemplates.get(action);
        return template == null ? "" : template;
    }

    /** 排除名单同时支持玩家名与 UUID，均不区分大小写。 */
    public boolean isExcluded(Player player) {
        if (exclude.isEmpty() || player == null) {
            return false;
        }
        if (exclude.contains(player.getName().toLowerCase(Locale.ROOT))) {
            return true;
        }
        String uuid = player.getUniqueId().toString().toLowerCase(Locale.ROOT);
        return exclude.contains(uuid) || exclude.contains(uuid.replace("-", ""));
    }

    private static Set<String> readExclude(List<String> entries) {
        Set<String> result = new HashSet<String>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                result.add(value);
                result.add(value.replace("-", ""));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<String> readLowercase(List<String> entries) {
        Set<String> result = new HashSet<String>();
        for (String entry : entries) {
            if (entry != null && !entry.trim().isEmpty()) {
                result.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Map<String, RuleSettings> readRules(ConfigurationSection section) {
        Map<String, RuleSettings> result = new HashMap<String, RuleSettings>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                continue;
            }
            result.put(key.toLowerCase(Locale.ROOT),
                    new RuleSettings(child.getBoolean("enabled", true), child.getString("template")));
        }
        return result;
    }

    private static Map<String, String> readNeiTemplates(FileConfiguration file) {
        Map<String, String> result = new HashMap<String, String>();
        ConfigurationSection section = file.getConfigurationSection("nei");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null && !"enabled".equalsIgnoreCase(key)) {
                result.put(key.toLowerCase(Locale.ROOT), value);
            }
        }
        return result;
    }
}
