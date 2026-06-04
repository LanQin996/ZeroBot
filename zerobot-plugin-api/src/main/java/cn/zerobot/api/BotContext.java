package cn.zerobot.api;

import cn.zerobot.api.event.EventListener;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.GroupFileUploadEvent;
import cn.zerobot.api.event.GroupFileUploadListener;
import cn.zerobot.api.event.GroupMessageEvent;
import cn.zerobot.api.event.GroupMessageListener;
import cn.zerobot.api.event.MetaEvent;
import cn.zerobot.api.event.MetaEventListener;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.api.event.NoticeEvent;
import cn.zerobot.api.event.NoticeListener;
import cn.zerobot.api.message.MessageSegment;
import cn.zerobot.api.command.CommandExecutor;
import cn.zerobot.api.event.PrivateMessageEvent;
import cn.zerobot.api.event.PrivateMessageListener;
import cn.zerobot.api.event.RequestEvent;
import cn.zerobot.api.event.RequestListener;
import com.fasterxml.jackson.databind.JsonNode;
import cn.zerobot.api.permission.PermissionService;
import cn.zerobot.api.permission.PermissionSubject;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface BotContext {
    /**
     * 插件日志对象。
     * <p>
     * 插件里建议统一使用这个 logger，日志会跟随 ZeroBot 一起输出。
     */
    Logger logger();

    /**
     * 调用原始 OneBot 动作。
     * <p>
     * 当 ZeroBot 还没有封装某个动作时，可以用这个方法直接调用 NapCat/OneBot。
     * {@code action} 是动作名，{@code params} 是动作参数，会原样发送给 NapCat。
     */
    CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params);

    /**
     * 调用没有参数的原始 OneBot 动作。
     */
    default CompletableFuture<ActionResponse<JsonNode>> callAction(String action) {
        return callAction(action, Map.of());
    }

    /**
     * 发送私聊消息。
     * <p>
     * {@code message} 可以是纯文本、{@link MessageSegment} 列表，或者 NapCat/OneBot 支持的消息结构。
     */
    CompletableFuture<ActionResponse<JsonNode>> sendPrivateMsg(String userId, Object message);

    /**
     * 发送群消息。
     * <p>
     * {@code message} 可以是纯文本、{@link MessageSegment} 列表，或者 NapCat/OneBot 支持的消息结构。
     */
    CompletableFuture<ActionResponse<JsonNode>> sendGroupMsg(String groupId, Object message);

    /**
     * 回复一条消息事件。
     * <p>
     * 群消息会回复到原群，私聊消息会回复给原发送者。
     */
    CompletableFuture<ActionResponse<JsonNode>> reply(MessageEvent event, Object message);

    /**
     * 权限服务。
     * <p>
     * 插件应使用统一权限节点判断能力，方便后续接入权限组管理插件。
     */
    PermissionService permission();

    /**
     * 注册插件提供的权限服务。
     * <p>
     * 权限管理插件可以用这个入口接管全局权限判断，也可以包装原有服务来保留内置超级管理员能力。
     */
    default EventSubscription registerPermissionService(PermissionService permissionService) {
        throw new UnsupportedOperationException("This ZeroBot runtime does not support permission service registration");
    }

    /**
     * 注册 plugin.yml 中声明的命令执行器。
     * <p>
     * 命令名应匹配 {@code plugin.yml -> commands -> name}。ZeroBot 会统一处理命令前缀、
     * 别名、权限节点和 usage 回复。
     */
    default EventSubscription registerCommand(String name, CommandExecutor executor) {
        throw new UnsupportedOperationException("This ZeroBot runtime does not support command registration");
    }

    /**
     * 判断消息发送者是否拥有指定权限节点。
     */
    default boolean hasPermission(MessageEvent event, String permission) {
        return permission().hasPermission(PermissionSubject.from(event), permission);
    }

    /**
     * 判断消息发送者是否拥有指定权限节点，并指定默认结果。
     * <p>
     * 如果某个命令只是声明权限节点用于后续权限插件管理，但当前默认所有人可用，可以传 {@code true}。
     */
    default boolean hasPermission(MessageEvent event, String permission, boolean defaultAllowed) {
        return permission().hasPermission(PermissionSubject.from(event), permission, defaultAllowed);
    }

    /**
     * 当前插件的配置目录。
     * <p>
     * 默认位置是 {@code config/<插件ID>/}。插件可以把 YAML、JSON 等用户可编辑配置放在这里。
     */
    Path configDir();

    /**
     * 当前插件的数据目录。
     * <p>
     * 默认位置是 {@code data/<插件ID>/}。插件可以把缓存、数据库、运行时数据放在这里。
     */
    Path dataDir();

    /**
     * 读取插件 YAML 配置。
     * <p>
     * 如果文件不存在，ZeroBot 会优先从插件 jar 中拷贝同名资源；资源不存在时，
     * 再使用 {@code configType} 的无参构造创建默认配置并写入文件。
     * 文件路径相对于 {@link #configDir()}。
     */
    <T> T loadConfig(String fileName, Class<T> configType) throws IOException;

    /**
     * 保存插件 jar 中的 {@code config.yml} 到插件配置目录。
     * <p>
     * 和 Bukkit/Spigot 的 {@code saveDefaultConfig()} 类似：目标文件已存在时不会覆盖。
     */
    default void saveDefaultConfig() throws IOException {
        saveResource("config.yml", false);
    }

    /**
     * 保存插件 jar 中的资源文件到插件配置目录。
     * <p>
     * {@code resourcePath} 相对于插件 jar 根目录；目标路径相对于 {@link #configDir()}。
     */
    default void saveResource(String resourcePath, boolean replace) throws IOException {
        throw new UnsupportedOperationException("This ZeroBot runtime does not support plugin resource saving");
    }

    /**
     * 保存插件 YAML 配置。
     * <p>
     * 文件路径相对于 {@link #configDir()}。
     */
    void saveConfig(String fileName, Object config) throws IOException;

    /**
     * 监听 NapCat 推送的所有 OneBot 事件。
     * <p>
     * 这是兜底入口，适合处理框架暂时没有封装的事件。
     * 如果类型化事件没有暴露你需要的字段，可以通过 {@code event.raw()} 读取完整 JSON。
     */
    EventSubscription onEvent(EventListener listener);

    /**
     * 监听所有消息事件，包括私聊消息和群消息。
     */
    EventSubscription onMessage(EventListener listener);

    /**
     * 只监听群消息。
     */
    default EventSubscription onGroupMessage(GroupMessageListener listener) {
        return onMessage(event -> {
            if (event instanceof GroupMessageEvent groupMessageEvent) {
                listener.onGroupMessage(groupMessageEvent);
            }
        });
    }

    /**
     * 只监听私聊消息。
     */
    default EventSubscription onPrivateMessage(PrivateMessageListener listener) {
        return onMessage(event -> {
            if (event instanceof PrivateMessageEvent privateMessageEvent) {
                listener.onPrivateMessage(privateMessageEvent);
            }
        });
    }

    /**
     * 监听 OneBot 通知事件。
     * <p>
     * 常见通知包括消息撤回、戳一戳、群成员变动、群文件上传等。
     */
    default EventSubscription onNotice(NoticeListener listener) {
        return onEvent(event -> {
            if (event instanceof NoticeEvent noticeEvent) {
                listener.onNotice(noticeEvent);
            }
        });
    }

    /**
     * 监听群文件上传事件。
     * <p>
     * 这是 {@code notice_type=group_upload} 的语义化入口。插件如果要处理群文件上传，
     * 建议使用这个方法，而不是同时消费群消息里的 {@code type=file} 消息段。
     */
    default EventSubscription onGroupFileUpload(GroupFileUploadListener listener) {
        return onNotice(event -> {
            if (event instanceof GroupFileUploadEvent uploadEvent) {
                listener.onGroupFileUpload(uploadEvent);
                return;
            }
            if ("group_upload".equals(event.noticeType())) {
                listener.onGroupFileUpload(new GroupFileUploadEvent(event.raw()));
            }
        });
    }

    /**
     * 监听 OneBot 请求事件。
     * <p>
     * 常见请求包括好友申请、加群申请等。
     */
    default EventSubscription onRequest(RequestListener listener) {
        return onEvent(event -> {
            if (event instanceof RequestEvent requestEvent) {
                listener.onRequest(requestEvent);
            }
        });
    }

    /**
     * 监听 OneBot 元事件。
     * <p>
     * 常见元事件包括生命周期事件、心跳事件等。
     */
    default EventSubscription onMetaEvent(MetaEventListener listener) {
        return onEvent(event -> {
            if (event instanceof MetaEvent metaEvent) {
                listener.onMetaEvent(metaEvent);
            }
        });
    }

    /**
     * 向群发送纯文本消息。
     */
    default CompletableFuture<ActionResponse<JsonNode>> sendGroupText(String groupId, String text) {
        return sendGroupMsg(groupId, List.of(MessageSegment.text(text)));
    }

    /**
     * 向群发送图片。
     * <p>
     * {@code file} 可以是 http/https URL、base64:// 数据、file:// URI，或 OneBot/NapCat 支持的图片标识。
     */
    default CompletableFuture<ActionResponse<JsonNode>> sendGroupImage(String groupId, String file) {
        return sendGroupMsg(groupId, List.of(MessageSegment.image(file)));
    }

    /**
     * 向群发送本地图片文件。
     * <p>
     * ZeroBot 会把本地文件读成 base64:// 数据，避免 NapCat 无法访问 ZeroBot 所在机器的本地路径。
     */
    default CompletableFuture<ActionResponse<JsonNode>> sendGroupImage(String groupId, Path file) {
        return sendGroupMsg(groupId, List.of(MessageSegment.image(file)));
    }

    /**
     * 向私聊用户发送纯文本消息。
     */
    default CompletableFuture<ActionResponse<JsonNode>> sendPrivateText(String userId, String text) {
        return sendPrivateMsg(userId, List.of(MessageSegment.text(text)));
    }

    /**
     * 向私聊用户发送图片。
     * <p>
     * {@code file} 可以是 http/https URL、base64:// 数据、file:// URI，或 OneBot/NapCat 支持的图片标识。
     */
    default CompletableFuture<ActionResponse<JsonNode>> sendPrivateImage(String userId, String file) {
        return sendPrivateMsg(userId, List.of(MessageSegment.image(file)));
    }

    /**
     * 向私聊用户发送本地图片文件。
     * <p>
     * ZeroBot 会把本地文件读成 base64:// 数据，避免 NapCat 无法访问 ZeroBot 所在机器的本地路径。
     */
    default CompletableFuture<ActionResponse<JsonNode>> sendPrivateImage(String userId, Path file) {
        return sendPrivateMsg(userId, List.of(MessageSegment.image(file)));
    }

    /**
     * 回复图片到消息来源。
     */
    default CompletableFuture<ActionResponse<JsonNode>> replyImage(MessageEvent event, String file) {
        return reply(event, List.of(MessageSegment.image(file)));
    }

    /**
     * 回复本地图片文件到消息来源。
     * <p>
     * ZeroBot 会把本地文件读成 base64:// 数据，避免 NapCat 无法访问 ZeroBot 所在机器的本地路径。
     */
    default CompletableFuture<ActionResponse<JsonNode>> replyImage(MessageEvent event, Path file) {
        return reply(event, List.of(MessageSegment.image(file)));
    }
}
