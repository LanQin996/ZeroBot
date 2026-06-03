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
    private final AtomicInteger reconnectFailures = new AtomicInteger();
    private volatile WebSocket webSocket;

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
            log.info("NapCat WebSocket 已关闭：{} {}", statusCode, reason);
            NapCatClient.this.webSocket = null;
            scheduleReconnectAfterFailure();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("NapCat WebSocket 异常：{}", rootMessage(error));
            NapCatClient.this.webSocket = null;
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
