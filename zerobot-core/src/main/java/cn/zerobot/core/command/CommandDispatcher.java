package cn.zerobot.core.command;

import cn.zerobot.api.BotContext;
import cn.zerobot.api.command.CommandContext;
import cn.zerobot.api.command.CommandExecutor;
import cn.zerobot.api.event.EventSubscription;
import cn.zerobot.api.event.MessageEvent;
import cn.zerobot.core.plugin.PluginDescriptor;
import cn.zerobot.core.plugin.PluginHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CommandDispatcher {
    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private final BotContext context;
    private final Map<String, RegisteredCommand> commands = new LinkedHashMap<>();

    public CommandDispatcher(BotContext context) {
        this.context = context;
    }

    public EventSubscription start() {
        return context.onMessage(event -> {
            if (event instanceof MessageEvent messageEvent) {
                dispatch(messageEvent);
            }
        });
    }

    public EventSubscription register(PluginHandle plugin, String name, CommandExecutor executor) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(executor, "executor");

        PluginDescriptor.PluginCommand descriptor = findCommand(plugin.descriptor(), name);
        RegisteredCommand command = new RegisteredCommand(plugin, descriptor, executor);
        List<String> labels = commandLabels(descriptor);
        synchronized (commands) {
            for (String label : labels) {
                RegisteredCommand existing = commands.get(label);
                if (existing != null && existing.plugin != plugin) {
                    throw new IllegalStateException("Command already registered: " + label);
                }
            }
            for (String label : labels) {
                commands.put(label, command);
            }
        }
        return () -> unregister(command);
    }

    public void unregisterPlugin(PluginHandle plugin) {
        synchronized (commands) {
            commands.entrySet().removeIf(entry -> entry.getValue().plugin == plugin);
        }
    }

    private void unregister(RegisteredCommand command) {
        synchronized (commands) {
            commands.entrySet().removeIf(entry -> entry.getValue() == command);
        }
    }

    private void dispatch(MessageEvent event) {
        ParsedCommand parsed = parse(event.rawMessage());
        if (parsed == null) {
            return;
        }
        RegisteredCommand command = command(parsed.lookup());
        if (command == null) {
            return;
        }
        String permission = command.descriptor.getPermission();
        if (!permission.isBlank() && !context.hasPermission(event, permission, false)) {
            String noPermission = command.descriptor.getNoPermission();
            context.reply(event, noPermission.isBlank()
                    ? "你没有权限使用这个命令。需要权限节点：" + permission
                    : noPermission);
            return;
        }

        CommandContext commandContext = new CommandContext(
                context,
                event,
                command.descriptor.getName(),
                parsed.label(),
                parsed.args()
        );
        try {
            boolean handled = command.executor.execute(commandContext);
            if (!handled) {
                sendUsage(event, command.descriptor, parsed.label());
            }
        } catch (Exception e) {
            log.warn("Command execution failed: {} from plugin {}", command.descriptor.getName(), command.plugin.descriptor().getId(), e);
            context.reply(event, "命令执行失败，请查看控制台日志。");
        }
    }

    private RegisteredCommand command(String label) {
        synchronized (commands) {
            return commands.get(label);
        }
    }

    private ParsedCommand parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        List<String> tokens = tokenize(raw.trim());
        if (tokens.isEmpty()) {
            return null;
        }
        String label = tokens.get(0);
        String lookup = normalizeLabel(label);
        if (lookup.isBlank()) {
            return null;
        }
        return new ParsedCommand(label, lookup, tokens.subList(1, tokens.size()));
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (quoted) {
                if (ch == quote) {
                    quoted = false;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quoted = true;
                quote = ch;
                continue;
            }
            if (Character.isWhitespace(ch)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private void sendUsage(MessageEvent event, PluginDescriptor.PluginCommand command, String label) {
        String usage = command.getUsage();
        if (usage.isBlank()) {
            usage = "/" + command.getName();
        }
        context.reply(event, usage.replace("/" + command.getName(), label));
    }

    private PluginDescriptor.PluginCommand findCommand(PluginDescriptor descriptor, String name) {
        String normalized = normalizeCommandName(name);
        for (PluginDescriptor.PluginCommand command : descriptor.getCommands()) {
            if (normalizeCommandName(command.getName()).equals(normalized)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Command is not declared in plugin.yml: " + name);
    }

    private List<String> commandLabels(PluginDescriptor.PluginCommand command) {
        List<String> labels = new ArrayList<>();
        addLabel(labels, command.getName());
        for (String alias : command.getAliases()) {
            addLabel(labels, alias);
        }
        return List.copyOf(labels);
    }

    private void addLabel(List<String> labels, String label) {
        String normalized = normalizeCommandName(label);
        if (!normalized.isBlank() && !labels.contains(normalized)) {
            labels.add(normalized);
        }
    }

    private String normalizeLabel(String label) {
        String normalized = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String normalizeCommandName(String name) {
        return normalizeLabel(name);
    }

    private record ParsedCommand(String label, String lookup, List<String> args) {
    }

    private record RegisteredCommand(
            PluginHandle plugin,
            PluginDescriptor.PluginCommand descriptor,
            CommandExecutor executor
    ) {
    }
}
