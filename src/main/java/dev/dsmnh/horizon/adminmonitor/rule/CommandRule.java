package dev.dsmnh.horizon.adminmonitor.rule;

import java.util.Map;

import dev.dsmnh.horizon.adminmonitor.watch.CommandCall;

/** 一条播报规则：规则键、别名、默认模板与参数解析器。 */
public final class CommandRule {

    /** 把命令参数解析成规则专用占位符。 */
    public interface Parser {
        void parse(CommandCall call, Map<String, String> placeholders);
    }

    private final String key;
    private final String[] aliases;
    private final String defaultTemplate;
    private final Parser parser;

    public CommandRule(String key, String[] aliases, String defaultTemplate, Parser parser) {
        this.key = key;
        this.aliases = aliases;
        this.defaultTemplate = defaultTemplate;
        this.parser = parser;
    }

    public String key() {
        return key;
    }

    public String[] aliases() {
        return aliases;
    }

    public String defaultTemplate() {
        return defaultTemplate;
    }

    public void parse(CommandCall call, Map<String, String> placeholders) {
        if (parser != null) {
            parser.parse(call, placeholders);
        }
    }
}
