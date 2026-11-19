package dev.dsmnh.horizon.adminmonitor;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import dev.dsmnh.horizon.adminmonitor.broadcast.Broadcaster;
import dev.dsmnh.horizon.adminmonitor.command.ReloadCommand;
import dev.dsmnh.horizon.adminmonitor.nei.NeiWatch;
import dev.dsmnh.horizon.adminmonitor.rule.RuleTable;
import dev.dsmnh.horizon.adminmonitor.watch.CommandWatch;
import dev.dsmnh.horizon.adminmonitor.watch.RemoteCommandWatch;

/**
 * 把管理员的高权限操作转成全体可见的播报。
 *
 * <p>命令播报只读命令行文本，不介入命令执行；NEI 播报在网络层读取自定义通道，
 * 两项功能各自独立开关。缺少 ProtocolLib 时 NEI 模块整体停用，命令播报不受影响。</p>
 */
public final class HorizonAdminMonitor extends JavaPlugin {

    private PluginConfig config;
    private RuleTable rules;
    private Broadcaster broadcaster;

    @Override
    public void onEnable() {
        ensureConfig();
        config = PluginConfig.read(getConfig());
        rules = RuleTable.create();
        broadcaster = new Broadcaster(this);

        CommandWatch commandWatch = new CommandWatch(this);
        getServer().getPluginManager().registerEvents(commandWatch, this);
        registerRemoteCommandWatch(commandWatch);

        PluginCommand command = getCommand("hadmin");
        if (command != null) {
            command.setExecutor(new ReloadCommand(this));
        } else {
            getLogger().warning("plugin.yml 缺少 hadmin 命令定义，重载命令不可用");
        }

        startNeiWatch();

        getLogger().info("已启用：命令播报 " + (config.enabled() ? "开" : "关")
                + "，调试模式 " + (config.debug() ? "开（执行者可见自己的播报）" : "关")
                + "，排除名单 " + config.excludedCount() + " 项");
    }

    /** 重新读取 {@code config.yml}，后续播报立即使用新配置。 */
    public void reload() {
        reloadConfig();
        config = PluginConfig.read(getConfig());
    }

    public PluginConfig config() {
        return config;
    }

    public RuleTable rules() {
        return rules;
    }

    public Broadcaster broadcaster() {
        return broadcaster;
    }

    /**
     * 注册 RCON 命令监听。
     *
     * <p>{@code RemoteServerCommandEvent} 只存在于 Thermos 系服务端，其它服务端加载本类时
     * 会抛 {@code NoClassDefFoundError}，此时跳过注册，控制台命令播报不受影响。</p>
     */
    private void registerRemoteCommandWatch(CommandWatch commandWatch) {
        try {
            getServer().getPluginManager().registerEvents(new RemoteCommandWatch(commandWatch), this);
        } catch (Throwable ignored) {
            getLogger().info("当前服务端没有 RCON 命令事件，RCON 播报停用");
        }
    }

    /**
     * 准备 {@code config.yml}。
     *
     * <p>不使用 {@code saveDefaultConfig()}：Thermos 系服务端的所有插件共用同一个类加载器，
     * {@code getResource("config.yml")} 会取到类路径上第一个同名资源，实测会把别的插件的配置
     * 复制进来。默认配置因此改用独有文件名 {@code config-default.yml} 存放在 jar 内。</p>
     */
    private void ensureConfig() {
        File folder = getDataFolder();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            getLogger().warning("无法创建配置目录：" + folder.getAbsolutePath());
            return;
        }
        File file = new File(folder, "config.yml");
        if (file.isFile()) {
            if (isOwnConfig(file)) {
                return;
            }
            File backup = new File(folder, "config.yml.bak-" + System.currentTimeMillis());
            if (!file.renameTo(backup)) {
                getLogger().warning("config.yml 不是本插件的内容且无法备份，请手工删除后重载");
                return;
            }
            getLogger().warning("config.yml 不是本插件的内容，已备份为 " + backup.getName() + "，重新生成默认配置");
        }
        copyDefaultConfig(file);
    }

    private boolean isOwnConfig(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String firstLine = reader.readLine();
            return firstLine != null && firstLine.contains("HorizonAdminMonitor");
        } catch (IOException e) {
            return false;
        } finally {
            closeQuietly(reader);
        }
    }

    private void copyDefaultConfig(File target) {
        InputStream source = getResource("config-default.yml");
        if (source == null) {
            getLogger().warning("jar 内缺少 config-default.yml，本次运行使用内置默认值");
            return;
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(target);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = source.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "写入默认配置失败", e);
        } finally {
            closeQuietly(source);
            closeQuietly(out);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // 关闭失败不影响配置读取
        }
    }

    private void startNeiWatch() {
        if (!config.neiEnabled()) {
            return;
        }
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().info("未安装 ProtocolLib，NEI 播报停用");
            return;
        }
        try {
            NeiWatch.start(this);
            getLogger().info("NEI 播报已启用");
        } catch (Throwable failure) {
            getLogger().log(Level.WARNING, "NEI 播报启动失败，已停用", failure);
        }
    }
}
