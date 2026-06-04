package cn.zerobot.api.command;

import cn.zerobot.api.BotContext;
import cn.zerobot.api.event.MessageEvent;

import java.util.List;

/**
 * A command invocation dispatched by ZeroBot.
 */
public record CommandContext(
        BotContext bot,
        MessageEvent event,
        String command,
        String label,
        List<String> args
) {
    public String arg(int index) {
        return index < 0 || index >= args.size() ? null : args.get(index);
    }

    public String joinedArgs(int startIndex) {
        if (startIndex < 0 || startIndex >= args.size()) {
            return "";
        }
        return String.join(" ", args.subList(startIndex, args.size()));
    }

    public void reply(Object message) {
        bot.reply(event, message);
    }
}
