package cn.zerobot.core.napcat;

import cn.zerobot.core.config.ZeroBotConfig;
import cn.zerobot.core.event.DefaultEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NapCatClientTest {
    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            server.close();
        } catch (Exception ignored) {
            // MockWebServer can time out waiting for the JDK WebSocket executor to settle.
        }
    }

    @Test
    void treatsOfflineAccountAsSeparateFromRuntimeHealth() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var status = mapper.readTree("""
                {"online":false,"good":true}
                """);

        assertThat(NapCatClient.isRuntimeHealthy(status)).isTrue();
        assertThat(NapCatClient.onlineState(status)).isFalse();
        assertThat(NapCatClient.heartbeatStatusSummary(status)).isEqualTo("online=false, good=true");
    }

    @Test
    void treatsBadRuntimeAsUnhealthy() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var status = mapper.readTree("""
                {"online":true,"good":false}
                """);

        assertThat(NapCatClient.isRuntimeHealthy(status)).isFalse();
        assertThat(NapCatClient.onlineState(status)).isTrue();
    }

    @Test
    void matchesActionResponsesByEchoAndDispatchesEvents() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayBlockingQueue<String> outbound = new ArrayBlockingQueue<>(1);
        AtomicInteger messages = new AtomicInteger();

        server.enqueue(new MockResponse.Builder()
                .webSocketUpgrade(new WebSocketListener() {
                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        outbound.add(text);
                        try {
                            String echo = mapper.readTree(text).get("echo").asText();
                            webSocket.send("""
                                    {"status":"ok","retcode":0,"data":{"message_id":123},"message":"","wording":"","echo":"%s"}
                                    """.formatted(echo));
                            webSocket.send("""
                                    {"post_type":"message","message_type":"private","user_id":1,"raw_message":"hello","message":"hello"}
                                    """);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .build());

        ZeroBotConfig.NapCatConfig config = new ZeroBotConfig.NapCatConfig();
        config.setWsUrl(server.url("/").toString().replace("http://", "ws://"));
        DefaultEventBus eventBus = new DefaultEventBus();
        eventBus.onMessage(event -> messages.incrementAndGet());
        NapCatClient client = new NapCatClient(config, eventBus, mapper);

        try {
            client.connect().get(3, TimeUnit.SECONDS);
            var response = client.callAction("send_msg", Map.of("message", "hello")).get(3, TimeUnit.SECONDS);

            assertThat(response.ok()).isTrue();
            assertThat(response.data().get("message_id").asInt()).isEqualTo(123);
            assertThat(outbound.poll(1, TimeUnit.SECONDS)).contains("\"action\":\"send_msg\"");

            for (int i = 0; i < 20 && messages.get() == 0; i++) {
                Thread.sleep(50);
            }
            assertThat(messages).hasValue(1);
        } finally {
            client.close();
        }
    }
}
