package cn.zerobot.examples.echo;

import cn.zerobot.api.BotContext;
import cn.zerobot.api.BotPlugin;
import cn.zerobot.api.message.MessageSegment;

import java.util.List;

public class EchoPlugin implements BotPlugin {
    private BotContext context;

    @Override
    public void onLoad(BotContext context) {
        this.context = context;
        context.onGroupMessage(messageEvent -> {
            String raw = messageEvent.rawMessage();
            if (raw == null || !raw.startsWith("/echo ")) {
                return;
            }
            String text = raw.substring("/echo ".length()).trim();
            if (text.isEmpty()) {
                return;
            }
            context.reply(messageEvent, List.of(MessageSegment.text(text)))
                    .exceptionally(error -> {
                        context.logger().warn("Echo reply failed", error);
                        return null;
                    });
        });
        context.onPrivateMessage(messageEvent -> {
            if ("/ping".equals(messageEvent.rawMessage())) {
                context.reply(messageEvent, List.of(MessageSegment.text("pong")));
            }
        });
        context.logger().info("EchoPlugin loaded. Try: /echo hello");
    }

    @Override
    public void onUnload() {
        if (context != null) {
            context.logger().info("EchoPlugin unloaded");
        }
    }
}
