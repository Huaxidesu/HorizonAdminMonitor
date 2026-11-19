# HorizonAdminMonitor

Minecraft 1.7.10 服务端的 Bukkit 插件，把管理员的高权限操作转成全体玩家可见的播报。

目标运行环境为 Thermos 及其分支 Crucible，与 [HorizonFixPlugin](../HorizonFixPlugin) 相同。
命令播报只使用 `org.bukkit` 公开接口；物品名解析与 NEI 包读取通过反射调用服务端已有的类，
不修改模组代码。

## 1. 功能

| 来源 | 覆盖的操作 |
|---|---|
| 命令 | `/give`、`/i`、`/gamemode`、`/time`、`/weather`、`/effect`、`/enchant`、`/xp`、`/tp`、`/summon`、`/difficulty`、`/gamerule`、`/op`、`/heal` 等，可在配置中增删 |
| NEI 作弊面板 | 取物品、治疗、改游戏模式、改附魔（需要 ProtocolLib）；改时间与天气由 NEI 自身向全体广播，默认不重复播报 |

播报内容为中文描述，例如：

```
[红扳手之神] 华溪 给予了 张三 钻石 ×64
[红扳手之神] 华溪 将 张三 的游戏模式改为 创造模式
[红扳手之神] 华溪 将时间设为 白天
```

管理员自己仍然看到命令原本的回显，播报是额外增加的。播报对象由 `debug` 决定：
关闭时执行者看不到自己的播报，只有其他玩家能看到，用于让玩家监督管理员；
开启时执行者本人也能看到，便于单人测试。
只有确实能执行该命令的玩家才会触发播报，普通玩家输入 `/give` 不会产生与实际效果不符的提示，
该判定可用 `require-permission` 关闭。

## 2. 排除名单

配置中的 `exclude` 列出不需要播报的管理员，写玩家名或 UUID 均可，不区分大小写：

```yaml
exclude:
  - "ExampleAdmin"
  - "069a79f4-44e9-4726-a5be-fca90e38aaf5"
```

名单内玩家执行的命令与 NEI 操作都不播报，其余行为不变。

## 3. 命令

| 命令 | 权限 | 说明 |
|---|---|---|
| `/hadmin reload` | `horizonadminmonitor.admin`（默认 op） | 重新读取 `config.yml` |

插件只注册这一条命令，不提供查询、调试或名单管理子命令。

## 4. 部署

1. 构建：双击 `build.bat`，产物为 `target/HorizonAdminMonitor-1.0.0.jar`。
2. 安装：把 jar 放入服务端 `plugins/`，重启服务端。
3. 首次启动生成 `plugins/HorizonAdminMonitor/config.yml`，按需修改后执行 `/hadmin reload`。
4. NEI 播报需要服务端安装 ProtocolLib；未安装时插件正常加载，仅在控制台提示一次，命令播报不受影响。

本机测试环境为 `D:\Game\Minecraft\Server\gtnh1\server2`（Crucible 1.7.10，后端 25599）。

## 5. 配置

```yaml
enabled: true                  # 总开关
prefix: "&8[&6红扳手之神&8] &r"  # 播报前缀
console-name: "控制台"          # 控制台执行时的执行者名
watch-console: true            # 控制台与 RCON 执行的命令是否播报
debug: false                   # 关闭时执行者看不到自己的播报；开启后可见并输出调试日志
require-permission: true       # 只播报执行者确实能用的命令

exclude: []

commands:
  give:
    enabled: true
    template: "&f%actor% &7给予了 &b%target% &f%item% &7×&e%amount%"
  # 其余规则同构，见 doc/程序结构.md

extra-commands:                # 追加无专用解析的命令，按通用模板播报
  - "heal"

nei:
  enabled: true
  give: "&f%actor% &7通过 NEI 取出了 &f%item% &7×&e%amount%"
```

模板占位符与规则清单见 `doc/程序结构.md`。未知占位符原样保留，便于发现配置错误。

## 6. 文档

| 文件 | 内容 |
|---|---|
| `doc/程序结构.md` | 模块划分、数据流、配置结构、NEI 包格式 |
| `doc/开发进度.md` | 当前进度与最近改动 |
| `doc/注释与Markdown规范.md` | 语言、注释与提交规范 |
