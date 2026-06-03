# ZeroBot

ZeroBot 是一个类似 Mirai Console 使用方式的 Java 机器人框架：

1. 启动 ZeroBot 框架本体。
2. ZeroBot 连接 NapCat / OneBot 11 正向 WebSocket。
3. 把插件 JAR 放进 `plugins/`。
4. 框架启动时自动加载插件，运行中可用命令热加载、卸载、重载。

## 构建框架

```powershell
.\gradlew.bat build
```

构建后框架发行包在：

```text
zerobot-app\build\distributions\zerobot-app-0.1.0.zip
```

解压后目录大概是：

```text
zerobot-app-0.1.0\
  bin\
    zerobot-app.bat
  lib\
  config.yml
  plugins\
```

## 配置 NapCat

编辑框架根目录里的 `config.yml`：

```yml
napcat:
  wsUrl: "ws://127.0.0.1:3001/"
  accessToken: ""
  actionTimeoutMs: 10000
  reconnectIntervalMs: 3000
pluginsDir: "plugins"
```

确保 NapCat 开启 OneBot 11 正向 WebSocket，并且地址和 `wsUrl` 一致。

## 启动框架

开发目录里可以直接运行：

```powershell
.\gradlew.bat :zerobot-app:run
```

发行包里运行：

```powershell
cd zerobot-app-0.1.0
.\bin\zerobot-app.bat
```

启动时 ZeroBot 会自动扫描并加载 `plugins/` 目录下的插件 JAR。

## 插件命令

框架启动后，控制台可输入：

```text
plugins
load <jar>
unload <id>
reload <id>
reload-all
stop
```

常用方式：

```text
load plugins\my-plugin.jar
reload my-plugin
unload my-plugin
```

## 示例插件

构建示例插件：

```powershell
.\gradlew.bat :examples:echo-plugin:jar
```

示例插件 JAR：

```text
examples\echo-plugin\build\libs\zerobot-echo-plugin-0.1.0.jar
```

把它放进框架的 `plugins/` 目录，或者在控制台输入：

```text
load examples\echo-plugin\build\libs\zerobot-echo-plugin-0.1.0.jar
```

然后在 QQ 私聊或群聊发送：

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
            // 处理 OneBot 消息事件
        });
    }

    @Override
    public void onUnload() {
        // 释放资源
    }
}
```

插件 JAR 根目录必须包含 `plugin.yml`：

```yml
id: my-plugin
name: My Plugin
version: 1.0.0
main: your.package.MyPlugin
```

可以直接复制 `examples/echo-plugin` 作为插件模板。
