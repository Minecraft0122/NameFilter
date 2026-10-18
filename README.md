# NameFilter

NameFilter 在玩家登录前检查用户名。它使用启动时预编译的正则表达式，在 Paper、Velocity 和 BungeeCord 上拒绝命中内置违禁词/名称规则或字符格式限制的连接，并把处理记录异步写入 `log/yyyy-MM-dd.log`。

## 平台与构建

由于 Paper、Velocity、BungeeCord 的插件 API 互不兼容，工程会生成三个独立插件包；请只把对应平台的包放入对应目录：

```text
namefilter-paper/target/namefilter-paper-1.2.0.jar       -> Paper plugins/
namefilter-velocity/target/namefilter-velocity-1.2.0.jar -> Velocity plugins/
namefilter-bungee/target/namefilter-bungee-1.2.0.jar     -> BungeeCord plugins/
```

使用 Java 21 和 Maven 构建：

```text
mvn clean package
```

三个平台 JAR 已经通过 Shade 内置了共享核心代码，不需要再复制 `namefilter-core` JAR。平台 API 使用 `provided` 依赖，由运行中的服务器提供。Velocity 包含标准的 `velocity-plugin.json`（含主类信息），Paper/Bungee 包含各自的插件描述文件。

## 配置

插件第一次启动时会生成配置文件。配置项如下：

```yaml
# Paper 使用 config.yml；Velocity/BungeeCord 使用同名含义的 config.properties。
mode: message
rejection-message: Your name contains a forbidden word.
enforce-name-pattern: true
```

`mode` 支持：

- `message`：拒绝连接，并显示 `rejection-message`。
- `silent`：拒绝连接，但发送空拒绝原因。

`enforce-name-pattern` 默认开启，要求玩家名字整体匹配 `[a-zA-Z0-9_]*`，也就是只允许 ASCII 英文字母、数字和下划线。这样可以避免某些子服不接受特殊字符时，玩家先通过代理再在子服登录失败。若本服或子服明确支持其他字符，可在配置中设为 `false` 关闭这项限制；违禁词检查仍然继续执行。

### 违禁名单

`forbidden-patterns.txt` 是可自定义的违规名称名单，编辑后执行 `/namefilter reload`（别名 `/nf reload`）即可生效，无需重启。Paper、Velocity 和 BungeeCord 都支持此命令，权限为 `namefilter.admin`；Paper 默认仅 OP 可用，代理端请通过权限系统授予。

插件首次启动时会在插件数据目录生成 `forbidden-patterns.txt`。Paper 通常位于 `plugins/NameFilter/forbidden-patterns.txt`，Velocity/BungeeCord 位于各自插件数据目录下的同名文件。文件以 UTF-8 编码，每行一个 Java 正则表达式片段；空行和以 `#` 开头的行会被忽略。普通名称可以直接写入，例如 `badname`；现有默认规则中的 `|`、`[\W_]*` 等写法用于表达多个变体和可选分隔符。

文件会在插件启动或执行重载时读取并预编译，玩家登录检查不会访问磁盘。清空文件（只保留注释或空行）表示关闭违禁词名单，但字符格式校验仍由 `enforce-name-pattern` 单独控制。若配置或名单文件无效，重载会提示错误并保留上一版运行设置；名单中的正则格式错误会报告文件和行号。

## 日志

每个平台插件的数据目录下都会创建 `log` 子目录，例如：

```text
plugins/NameFilter/log/2026-08-24.log
```

每条记录包含时间、平台、玩家名、远端地址、命中的文本和处理模式。日志写入使用单线程后台队列，插件关闭时会先等待队列刷新。

## 事件入口

- Paper：`PlayerConnectionValidateLoginEvent`，在现代 Paper 的登录校验阶段处理。
- Velocity：`LoginEvent`，在代理允许玩家继续连接前处理。
- BungeeCord：`PreLoginEvent`，在预登录阶段取消连接。

建议先在测试服务器检查内置规则是否符合本服命名政策，再部署到生产代理或子服。
