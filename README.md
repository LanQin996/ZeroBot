# ZeroBot

ZeroBot 是一个 Java 21 机器人框架，用来对接 NapCat / OneBot 11。使用方式类似一个轻量控制台框架：启动框架本体，连接 NapCat，把插件 JAR 放进 `plugins/`。

## 构建

```powershell
.\gradlew.bat build
```

可直接运行的框架 JAR：

```text
build\libs\ZeroBot-0.1.0.jar
```

应用发行包：

```text
zerobot-app\build\distributions\zerobot-app-0.1.0.zip
```

## 配置

`config.yml`：

```yml
napcat:
  wsUrl: "ws://127.0.0.1:3001/"
  accessToken: ""
  actionTimeoutMs: 10000
  reconnectIntervalMs: 5000
  reconnectFailuresBeforeCooldown: 5
  reconnectCooldownMs: 60000
pluginsDir: "plugins"
```

请确保 NapCat 已开启 OneBot 11 正向 WebSocket，并且地址与 `napcat.wsUrl` 一致。

## 启动

在运行目录执行：

```powershell
java -jar ZeroBot-0.1.0.jar
```

Windows `cmd` 中建议使用启动脚本，它会自动切换 UTF-8：

```cmd
start.bat
```

如果仍想手动启动，先执行：

```cmd
chcp 65001
```

再运行：

```cmd
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar ZeroBot-0.1.0.jar
```

也可以使用 Windows Terminal 或 PowerShell 7。

开发目录中也可以直接运行：

```powershell
.\gradlew.bat :zerobot-app:run
```

ZeroBot 会把当前工作目录作为框架根目录，读取这里的 `config.yml`，并自动加载 `plugins/` 目录下的插件 JAR。

## 控制台命令

推荐命令：

```text
help
plugin list
plugin load <jar>
plugin unload <id>
plugin reload <id>
plugin reload-all
stop
```

## 示例插件

构建示例插件：

```powershell
.\gradlew.bat :examples:echo-plugin:jar
```

插件 JAR：

```text
examples\echo-plugin\build\libs\zerobot-echo-plugin-0.1.0.jar
```

加载插件：

```text
plugin load examples\echo-plugin\build\libs\zerobot-echo-plugin-0.1.0.jar
```

然后在 QQ 里发送：

```text
/echo hello
```

机器人会回复：

```text
hello
```

## 开发插件

插件主类实现 `BotPlugin`：

```java
public class MyPlugin implements BotPlugin {
    @Override
    public void onLoad(BotContext context) {
        context.onMessage(event -> {
            // 处理 OneBot 消息事件。
        });
    }

    @Override
    public void onUnload() {
        // 释放插件资源。
    }
}
```

每个插件 JAR 根目录必须包含 `plugin.yml`：

```yml
id: my-plugin
name: My Plugin
version: 1.0.0
main: your.package.MyPlugin
```

可以用 `examples/echo-plugin` 作为第一个插件模板。
