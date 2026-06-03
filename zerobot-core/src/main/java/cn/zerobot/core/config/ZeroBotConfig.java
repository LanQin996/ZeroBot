package cn.zerobot.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZeroBotConfig {
    private NapCatConfig napcat = new NapCatConfig();
    private String pluginsDir = "plugins";

    public static ZeroBotConfig load(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        if (Files.notExists(path)) {
            ZeroBotConfig config = new ZeroBotConfig();
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), config);
            return config;
        }
        return mapper.readValue(path.toFile(), ZeroBotConfig.class);
    }

    public NapCatConfig getNapcat() {
        return napcat;
    }

    public void setNapcat(NapCatConfig napcat) {
        this.napcat = napcat;
    }

    public String getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    public static class NapCatConfig {
        private String wsUrl = "ws://127.0.0.1:3001/";
        private String accessToken = "";
        private long actionTimeoutMs = 10_000;
        private long reconnectIntervalMs = 3_000;

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
    }
}
