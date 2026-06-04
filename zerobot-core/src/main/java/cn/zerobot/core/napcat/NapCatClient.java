package cn.zerobot.core.napcat;

import cn.zerobot.api.ActionResponse;
import cn.zerobot.core.config.ZeroBotConfig;
import cn.zerobot.core.event.DefaultEventBus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NapCatClient implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(NapCatClient.class);

    private final ZeroBotConfig.NapCatConfig config;
    private final DefaultEventBus eventBus;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final Map<String, PendingAction> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private final AtomicBoolean heartbeatMissingWarned = new AtomicBoolean();
    private final AtomicBoolean activeHeartbeatRunning = new AtomicBoolean();
    private final AtomicInteger reconnectFailures = new AtomicInteger();
    private volatile WebSocket webSocket;
    private volatile long connectedAtMs = -1;
    private volatile long lastHeartbeatAtMs = -1;
    private volatile Boolean lastHeartbeatHealthy;
    private volatile String lastHeartbeatStatus = "";
    private volatile Boolean lastActiveHeartbeatHealthy;
    private volatile String lastActiveHeartbeatStatus = "";
    private volatile Boolean lastBotOnline;

    public NapCatClient(ZeroBotConfig.NapCatConfig config, DefaultEventBus eventBus, ObjectMapper mapper) {
        this.config = config;
        this.eventBus = eventBus;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "zerobot-napcat-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduleHeartbeatMonitor();
        scheduleActiveHeartbeat();
    }

    public CompletableFuture<Void> connect() {
        WebSocket.Builder builder = httpClient.newWebSocketBuilder();
        if (config.getAccessToken() != null && !config.getAccessToken().isBlank()) {
            builder.header("Authorization", "Bearer " + config.getAccessToken());
        }
        log.info("正在连接 NapCat WebSocket：{}", config.getWsUrl());
        return builder.buildAsync(URI.create(config.getWsUrl()), new Listener())
                .thenAccept(ws -> this.webSocket = ws);
    }

    public void start() {
        connect().exceptionally(error -> {
            log.warn("NapCat WebSocket 连接失败：{}。请检查 napcat.wsUrl，并确认 NapCat 已开启 OneBot 11 正向 WebSocket。",
                    rootMessage(error));
            scheduleReconnectAfterFailure();
            return null;
        });
    }

    public CompletableFuture<ActionResponse<JsonNode>> callAction(String action, Map<String, Object> params) {
        Objects.requireNonNull(action, "action");
        WebSocket ws = webSocket;
        if (ws == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("NapCat WebSocket is not connected"));
        }

        String echo = UUID.randomUUID().toString();
        CompletableFuture<ActionResponse<JsonNode>> future = new CompletableFuture<>();
        var timeout = scheduler.schedule(() -> {
            PendingAction removed = pending.remove(echo);
            if (removed != null) {
                removed.future().completeExceptionally(new IOException("Action timed out: " + action));
            }
        }, config.getActionTimeoutMs(), TimeUnit.MILLISECONDS);
        pending.put(echo, new PendingAction(future, timeout));

        try {
            String payload = mapper.writeValueAsString(Map.of(
                    "action", action,
                    "params", params == null ? Map.of() : params,
                    "echo", echo
            ));
            ws.sendText(payload, true).exceptionally(error -> {
                PendingAction removed = pending.remove(echo);
                if (removed != null) {
                    removed.timeout().cancel(false);
                    removed.future().completeExceptionally(error);
                }
                return null;
            });
        } catch (Exception e) {
            PendingAction removed = pending.remove(echo);
            if (removed != null) {
                removed.timeout().cancel(false);
            }
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        WebSocket ws = webSocket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "ZeroBot shutdown");
        }
        pending.forEach((echo, action) -> {
            action.timeout().cancel(false);
            action.future().completeExceptionally(new IOException("NapCat client closed"));
        });
        pending.clear();
        scheduler.shutdownNow();
    }

    private void scheduleReconnectAfterFailure() {
        if (closed.get() || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        int failures = reconnectFailures.incrementAndGet();
        long delayMs = reconnectDelayMs(failures);
        if (isCooldownFailure(failures)) {
            log.warn("NapCat WebSocket 已连续失败 {} 次，冷却 {} 毫秒后再重试。", failures, delayMs);
        }
        scheduler.schedule(() -> {
            reconnectScheduled.set(false);
            if (closed.get()) {
                return;
            }
            connect().exceptionally(error -> {
                log.warn("NapCat WebSocket 重连失败：{}", rootMessage(error));
                scheduleReconnectAfterFailure();
                return null;
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private long reconnectDelayMs(int failures) {
        return isCooldownFailure(failures)
                ? Math.max(1, config.getReconnectCooldownMs())
                : Math.max(1, config.getReconnectIntervalMs());
    }

    private boolean isCooldownFailure(int failures) {
        int threshold = Math.max(1, config.getReconnectFailuresBeforeCooldown());
        return failures % threshold == 0;
    }

    private void scheduleHeartbeatMonitor() {
        long checkIntervalMs = config.getHeartbeatCheckIntervalMs();
        if (checkIntervalMs <= 0) {
            return;
        }
        long normalizedIntervalMs = Math.max(1_000, checkIntervalMs);
        scheduler.scheduleWithFixedDelay(
                this::checkHeartbeatTimeoutSafely,
                normalizedIntervalMs,
                normalizedIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void checkHeartbeatTimeoutSafely() {
        try {
            checkHeartbeatTimeout();
        } catch (Exception e) {
            log.debug("NapCat heartbeat monitor failed", e);
        }
    }

    private void checkHeartbeatTimeout() {
        long timeoutMs = config.getHeartbeatTimeoutMs();
        if (closed.get() || timeoutMs <= 0 || webSocket == null) {
            return;
        }
        long lastSeen = lastHeartbeatAtMs > 0 ? lastHeartbeatAtMs : connectedAtMs;
        if (lastSeen <= 0) {
            return;
        }
        long elapsedMs = System.currentTimeMillis() - lastSeen;
        if (elapsedMs >= timeoutMs && heartbeatMissingWarned.compareAndSet(false, true)) {
            String source = lastHeartbeatAtMs > 0 ? "上一条心跳" : "连接建立";
            log.warn("NapCat WebSocket 仍处于连接状态，但从{}起已经 {} 毫秒没有收到心跳。请检查 NapCat/QQ 是否仍在线。",
                    source, elapsedMs);
        }
    }

    private void scheduleActiveHeartbeat() {
        long intervalMs = config.getActiveHeartbeatIntervalMs();
        if (intervalMs <= 0) {
            return;
        }
        long normalizedIntervalMs = Math.max(1_000, intervalMs);
        scheduler.scheduleWithFixedDelay(
                this::activeHeartbeatSafely,
                normalizedIntervalMs,
                normalizedIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void activeHeartbeatSafely() {
        try {
            activeHeartbeat();
        } catch (Exception e) {
            log.debug("NapCat active heartbeat failed", e);
        }
    }

    private void activeHeartbeat() {
        if (closed.get() || webSocket == null || !activeHeartbeatRunning.compareAndSet(false, true)) {
            return;
        }
        callAction("get_status", Map.of())
                .whenComplete((response, error) -> {
                    try {
                        handleActiveHeartbeatResult(response, error);
                    } finally {
                        activeHeartbeatRunning.set(false);
                    }
                });
    }

    private void handleActiveHeartbeatResult(ActionResponse<JsonNode> response, Throwable error) {
        if (error != null) {
            reportActiveHeartbeatHealth(false, "action=get_status, error=" + rootMessage(error));
            return;
        }
        if (response == null) {
            reportActiveHeartbeatHealth(false, "action=get_status, response=null");
            return;
        }
        if (!response.ok()) {
            reportActiveHeartbeatHealth(false, "action=get_status, status=%s, retcode=%d, message=%s, wording=%s"
                    .formatted(response.status(), response.retcode(), response.message(), response.wording()));
            return;
        }
        JsonNode data = response.data();
        if (data == null || data.isNull() || !data.isObject()) {
            reportActiveHeartbeatHealth(true, "action=get_status, status=ok");
            return;
        }
        boolean healthy = isRuntimeHealthy(data);
        reportActiveHeartbeatHealth(healthy, "action=get_status, " + heartbeatStatusSummary(data));
        reportBotOnlineStatus(data, "主动状态检查");
    }

    private void reportActiveHeartbeatHealth(boolean healthy, String status) {
        Boolean previous = lastActiveHeartbeatHealthy;
        String previousStatus = lastActiveHeartbeatStatus;
        lastActiveHeartbeatHealthy = healthy;
        lastActiveHeartbeatStatus = status;

        if (!healthy && (!Boolean.FALSE.equals(previous) || !status.equals(previousStatus))) {
            log.warn("NapCat 主动心跳异常：{}。ZeroBot 将继续定时检测。", status);
            return;
        }
        if (healthy && Boolean.FALSE.equals(previous)) {
            log.info("NapCat 主动心跳恢复正常：{}", status);
        }
    }

    private void handleMetaEvent(JsonNode node) {
        if ("heartbeat".equals(node.path("meta_event_type").asText(""))) {
            handleHeartbeat(node);
        }
    }

    private void handleHeartbeat(JsonNode node) {
        lastHeartbeatAtMs = System.currentTimeMillis();
        heartbeatMissingWarned.set(false);

        JsonNode status = node.get("status");
        if (status == null || status.isNull() || !status.isObject()) {
            reportHeartbeatHealth(false, "status=missing");
            return;
        }

        boolean healthy = isRuntimeHealthy(status);
        reportHeartbeatHealth(healthy, heartbeatStatusSummary(status));
        reportBotOnlineStatus(status, "心跳");
    }

    static boolean isRuntimeHealthy(JsonNode status) {
        Boolean good = booleanValue(status, "good");
        Boolean appGood = booleanValue(status, "app_good");
        return !Boolean.FALSE.equals(good)
                && !Boolean.FALSE.equals(appGood);
    }

    private void reportHeartbeatHealth(boolean healthy, String status) {
        Boolean previous = lastHeartbeatHealthy;
        String previousStatus = lastHeartbeatStatus;
        lastHeartbeatHealthy = healthy;
        lastHeartbeatStatus = status;

        if (!healthy && (!Boolean.FALSE.equals(previous) || !status.equals(previousStatus))) {
            log.warn("NapCat 心跳状态异常：{}。请检查 NapCat 运行状态。", status);
            return;
        }
        if (healthy && Boolean.FALSE.equals(previous)) {
            log.info("NapCat 心跳恢复正常：{}", status);
        }
    }

    private void reportBotOnlineStatus(JsonNode status, String source) {
        Boolean online = onlineState(status);
        if (online == null) {
            return;
        }

        Boolean previous = lastBotOnline;
        String summary = heartbeatStatusSummary(status);
        lastBotOnline = online;

        if (!online && !Boolean.FALSE.equals(previous)) {
            log.info("NapCat {}显示 QQ 当前离线：{}。ZeroBot 与 NapCat 的连接仍保持。", source, summary);
            return;
        }
        if (online && Boolean.FALSE.equals(previous)) {
            log.info("NapCat {}显示 QQ 已恢复在线：{}", source, summary);
        }
    }

    static Boolean onlineState(JsonNode status) {
        return booleanValue(status, "online");
    }

    static String heartbeatStatusSummary(JsonNode status) {
        StringBuilder builder = new StringBuilder();
        appendStatusField(builder, status, "online");
        appendStatusField(builder, status, "good");
        appendStatusField(builder, status, "app_good");
        appendStatusField(builder, status, "app_enabled");
        appendStatusField(builder, status, "app_initialized");
        return builder.isEmpty() ? status.toString() : builder.toString();
    }

    private static void appendStatusField(StringBuilder builder, JsonNode status, String field) {
        JsonNode value = status.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(field).append('=').append(value.asText());
    }

    private static Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private void resetConnectionState() {
        connectedAtMs = System.currentTimeMillis();
        lastHeartbeatAtMs = -1;
        lastHeartbeatHealthy = null;
        lastHeartbeatStatus = "";
        lastActiveHeartbeatHealthy = null;
        lastActiveHeartbeatStatus = "";
        lastBotOnline = null;
        heartbeatMissingWarned.set(false);
        activeHeartbeatRunning.set(false);
    }

    private void markDisconnected() {
        webSocket = null;
        connectedAtMs = -1;
        lastHeartbeatAtMs = -1;
        lastHeartbeatHealthy = null;
        lastHeartbeatStatus = "";
        lastActiveHeartbeatHealthy = null;
        lastActiveHeartbeatStatus = "";
        lastBotOnline = null;
        heartbeatMissingWarned.set(false);
        activeHeartbeatRunning.set(false);
    }

    private void failPendingActions(String message) {
        IOException error = new IOException(message);
        pending.forEach((echo, action) -> {
            action.timeout().cancel(false);
            action.future().completeExceptionally(error);
        });
        pending.clear();
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("NapCat WebSocket 已连接");
            resetConnectionState();
            reconnectFailures.set(0);
            reconnectScheduled.set(false);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleMessage(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (closed.get()) {
                log.info("NapCat WebSocket 已关闭：{} {}", statusCode, reason);
            } else {
                log.warn("NapCat WebSocket 已断开：{} {}。将自动尝试重连。", statusCode, reason);
            }
            markDisconnected();
            failPendingActions("NapCat WebSocket disconnected");
            scheduleReconnectAfterFailure();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("NapCat WebSocket 异常：{}", rootMessage(error));
            markDisconnected();
            failPendingActions("NapCat WebSocket error: " + rootMessage(error));
            scheduleReconnectAfterFailure();
        }

        private void handleMessage(String message) {
            try {
                JsonNode node = mapper.readTree(message);
                JsonNode echoNode = node.get("echo");
                if (echoNode != null && !echoNode.isNull()) {
                    completeAction(echoNode.asText(), node);
                    return;
                }
                if (node.has("post_type")) {
                    if ("meta_event".equals(text(node, "post_type"))) {
                        handleMetaEvent(node);
                    }
                    eventBus.publish(node);
                } else {
                    log.debug("Ignoring NapCat message without echo/post_type: {}", message);
                }
            } catch (Exception e) {
                log.warn("Failed to handle NapCat message: {}", message, e);
            }
        }

        private void completeAction(String echo, JsonNode node) {
            PendingAction action = pending.remove(echo);
            if (action == null) {
                log.debug("No pending action for echo={}", echo);
                return;
            }
            action.timeout().cancel(false);
            ActionResponse<JsonNode> response = new ActionResponse<>(
                    text(node, "status"),
                    intValue(node, "retcode"),
                    node.get("data"),
                    text(node, "message"),
                    text(node, "wording"),
                    echo
            );
            action.future().complete(response);
        }

        private String text(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? "" : value.asText();
        }

        private int intValue(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? 0 : value.asInt();
        }
    }
}
