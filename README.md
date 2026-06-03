<div align="center">

# ZeroBot

面向 NapCat / OneBot 11 的轻量 Java 机器人框架

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.10+-02303A.svg)](https://gradle.org/)
[![OneBot](https://img.shields.io/badge/OneBot-11-blue.svg)](https://github.com/botuniverse/onebot-11)
[![NapCat](https://img.shields.io/badge/NapCat-WebSocket-2ea44f.svg)](https://github.com/NapNeko/NapCatQQ)

**连接 NapCat，加载插件，把机器人能力交给 Java 生态。**

</div>

## 概览

ZeroBot 是一个围绕插件运行时设计的 QQ 机器人框架。

框架本体负责连接 NapCat、接收 OneBot 事件、分发类型化事件、调用 OneBot 动作、管理插件生命周期。业务逻辑由插件提供，插件可以独立开发、独立构建、独立分发。

## 特性

- NapCat / OneBot 11 正向 WebSocket 接入
- 插件 JAR 加载、卸载、重载
- 群消息、私聊消息、通知、请求、元事件的类型化封装
- 保留原始 OneBot JSON，兼容 NapCat 扩展字段
- 插件 API 可发布到 Maven 仓库
- 插件脚手架可独立维护和分发

## 生态

```text
ZeroBot                  框架本体
ZeroBot Plugin API       插件开发 API
ZeroBotPluginTemplate    独立插件脚手架
ZeroBot Plugins          官方或第三方插件
```

插件开发者只需要依赖：

```text
cn.zerobot:zerobot-plugin-api:0.1.0
```

## 模块

```text
zerobot-plugin-api   对外插件 API
zerobot-core         框架核心实现
zerobot-app          启动器与发行包
```

## 状态

ZeroBot 仍处于早期开发阶段，API 会围绕真实插件开发体验持续调整。

当前重点：

- 打磨插件 API
- 完善事件封装
- 整理插件脚手架
- 改进发行包和文档

## 要求

- Java 21+
- NapCat OneBot 11 正向 WebSocket

## 声明

ZeroBot 是插件化机器人框架，不包含 QQ 客户端能力，也不绕过平台限制。
请在遵守相关平台规则、NapCat 使用规则和法律法规的前提下使用。
