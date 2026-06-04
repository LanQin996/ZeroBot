package cn.zerobot.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ZeroBotConfig {
    private NapCatConfig napcat = new NapCatConfig();
    private ReplyConfig reply = new ReplyConfig();
    private String pluginsDir = "plugins";
    private List<String> superAdmins = new ArrayList<>();

    public static ZeroBotConfig load(Path path) throws IOException {
        ObjectMapper mapper = yamlMapper();
        if (Files.notExists(path)) {
            ZeroBotConfig config = new ZeroBotConfig();
            Files.writeString(path, defaultConfigTemplate());
            return config;
        }
        return mapper.readValue(path.toFile(), ZeroBotConfig.class);
    }

    private static String defaultConfigTemplate() {
        return """
                # NapCat / OneBot 11 正向 WebSocket 配置。
                napcat:
                  # NapCat 正向 WebSocket 地址。
                  wsUrl: "ws://127.0.0.1:3001/"
                  # NapCat access_token；没有设置就留空。
                  accessToken: ""
                  # OneBot 动作调用超时时间，单位毫秒。
                  actionTimeoutMs: 10000
                  # WebSocket 断开后的普通重连间隔，单位毫秒。
                  reconnectIntervalMs: 5000
                  # 连续失败多少次后进入较长冷却。
                  reconnectFailuresBeforeCooldown: 5
                  # 重连冷却时间，单位毫秒。
                  reconnectCooldownMs: 60000
                  # 超过多久没有收到 NapCat 心跳后告警；设为 0 可关闭。
                  heartbeatTimeoutMs: 45000
                  # 心跳超时检查间隔；设为 0 可关闭。
                  heartbeatCheckIntervalMs: 5000
                  # 主动调用 get_status 的间隔；设为 0 可关闭。
                  activeHeartbeatIntervalMs: 15000

                # 框架统一回复行为，影响 bot.reply(...) 和 command.reply(...)。
                reply:
                  # 是否引用触发回复的原消息。
                  quoteMessage: false
                  # 群聊回复时是否自动 @ 触发者；私聊不会 @。
                  mentionUser: false

                # 插件目录。相对路径会基于 ZeroBot 所在目录。
                pluginsDir: "plugins"

                # 超级管理员 QQ 号列表，拥有内置权限服务的全部权限。
                superAdmins: []
                """;
    }

    private static ObjectMapper yamlMapper() {
        YAMLFactory factory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();
        return new ObjectMapper(factory);
    }

    public NapCatConfig getNapcat() {
        return napcat;
    }

    public void setNapcat(NapCatConfig napcat) {
        this.napcat = napcat;
    }

    public ReplyConfig getReply() {
        return reply;
    }

    public void setReply(ReplyConfig reply) {
        this.reply = reply == null ? new ReplyConfig() : reply;
    }

    public String getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    public List<String> getSuperAdmins() {
        return superAdmins;
    }

    public void setSuperAdmins(List<String> superAdmins) {
        this.superAdmins = superAdmins == null ? new ArrayList<>() : superAdmins;
    }

    public static class NapCatConfig {
        private String wsUrl = "ws://127.0.0.1:3001/";
        private String accessToken = "";
        private long actionTimeoutMs = 10_000;
        private long reconnectIntervalMs = 5_000;
        private int reconnectFailuresBeforeCooldown = 5;
        private long reconnectCooldownMs = 60_000;
        private long heartbeatTimeoutMs = 45_000;
        private long heartbeatCheckIntervalMs = 5_000;
        private long activeHeartbeatIntervalMs = 15_000;

        public String getWsUrl() {
            return wsUrl;
        }

        public void setWsUrl(String wsUrl) {
            this.wsUrl = wsUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public long getActionTimeoutMs() {
            return actionTimeoutMs;
        }

        public void setActionTimeoutMs(long actionTimeoutMs) {
            this.actionTimeoutMs = actionTimeoutMs;
        }

        public long getReconnectIntervalMs() {
            return reconnectIntervalMs;
        }

        public void setReconnectIntervalMs(long reconnectIntervalMs) {
            this.reconnectIntervalMs = reconnectIntervalMs;
        }

        public int getReconnectFailuresBeforeCooldown() {
            return reconnectFailuresBeforeCooldown;
        }

        public void setReconnectFailuresBeforeCooldown(int reconnectFailuresBeforeCooldown) {
            this.reconnectFailuresBeforeCooldown = reconnectFailuresBeforeCooldown;
        }

        public long getReconnectCooldownMs() {
            return reconnectCooldownMs;
        }

        public void setReconnectCooldownMs(long reconnectCooldownMs) {
            this.reconnectCooldownMs = reconnectCooldownMs;
        }

        public long getHeartbeatTimeoutMs() {
            return heartbeatTimeoutMs;
        }

        public void setHeartbeatTimeoutMs(long heartbeatTimeoutMs) {
            this.heartbeatTimeoutMs = heartbeatTimeoutMs;
        }

        public long getHeartbeatCheckIntervalMs() {
            return heartbeatCheckIntervalMs;
        }

        public void setHeartbeatCheckIntervalMs(long heartbeatCheckIntervalMs) {
            this.heartbeatCheckIntervalMs = heartbeatCheckIntervalMs;
        }

        public long getActiveHeartbeatIntervalMs() {
            return activeHeartbeatIntervalMs;
        }

        public void setActiveHeartbeatIntervalMs(long activeHeartbeatIntervalMs) {
            this.activeHeartbeatIntervalMs = activeHeartbeatIntervalMs;
        }
    }

    public static class ReplyConfig {
        private boolean quoteMessage;
        private boolean mentionUser;

        public boolean isQuoteMessage() {
            return quoteMessage;
        }

        public void setQuoteMessage(boolean quoteMessage) {
            this.quoteMessage = quoteMessage;
        }

        public boolean isMentionUser() {
            return mentionUser;
        }

        public void setMentionUser(boolean mentionUser) {
            this.mentionUser = mentionUser;
        }
    }
}
