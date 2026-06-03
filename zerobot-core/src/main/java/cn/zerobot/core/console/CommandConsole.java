package cn.zerobot.core.console;

import cn.zerobot.core.plugin.PluginHandle;
import cn.zerobot.core.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandConsole implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(CommandConsole.class);

    private final PluginManager pluginManager;
    private final CountDownLatch stopLatch = new CountDownLatch(1);
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread thread;

    public CommandConsole(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::loop, "zerobot-console");
        thread.start();
    }

    public void awaitStop() throws InterruptedException {
        stopLatch.await();
    }

    @Override
    public void close() {
        running.set(false);
        stopLatch.countDown();
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        printBanner();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running.get()) {
                String line = reader.readLine();
                if (line == null) {
                    close();
                    return;
                }
                handle(line.trim());
            }
        } catch (Exception e) {
            if (running.get()) {
                log.warn("Console stopped unexpectedly", e);
            }
            close();
        }
    }

    private void handle(String line) {
        if (line.isBlank()) {
            return;
        }
        String[] tokens = line.split("\\s+");
        String command = tokens[0].toLowerCase();
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);

        try {
            switch (command) {
                case "help", "?" -> printHelp();
                case "plugin", "plugins" -> handlePlugin(args);
                case "load" -> loadPlugin(requireArg(command, args, "<jar>"));
                case "unload" -> unloadPlugin(requireArg(command, args, "<id>"));
                case "reload" -> reloadPlugin(requireArg(command, args, "<id>"));
                case "reload-all" -> reloadAllPlugins();
                case "stop", "exit", "quit" -> close();
                default -> {
                    log.info("未知命令：{}", command);
                    log.info("输入 help 查看可用命令。");
                }
            }
        } catch (Exception e) {
            log.warn("命令执行失败：{}", line, e);
        }
    }

    private void handlePlugin(String[] args) throws Exception {
        if (args.length == 0) {
            listPlugins();
            return;
        }

        String subcommand = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (subcommand) {
            case "list", "ls" -> listPlugins();
            case "load" -> loadPlugin(requireArg("plugin load", rest, "<jar>"));
            case "unload" -> unloadPlugin(requireArg("plugin unload", rest, "<id>"));
            case "reload" -> reloadPlugin(requireArg("plugin reload", rest, "<id>"));
            case "reload-all" -> reloadAllPlugins();
            default -> {
                log.info("未知插件命令：{}", subcommand);
                printPluginHelp();
            }
        }
    }

    private String requireArg(String command, String[] args, String placeholder) {
        if (args.length == 0 || args[0].isBlank()) {
            throw new IllegalArgumentException("Usage: " + command + " " + placeholder);
        }
        return args[0];
    }

    private void loadPlugin(String jarPath) throws Exception {
        PluginHandle handle = pluginManager.load(Path.of(jarPath));
        log.info("已加载插件：{} {} ({})",
                handle.descriptor().getId(),
                handle.descriptor().getVersion(),
                handle.jarPath());
    }

    private void unloadPlugin(String id) throws Exception {
        pluginManager.unload(id);
        log.info("已卸载插件：{}", id);
    }

    private void reloadPlugin(String id) throws Exception {
        PluginHandle handle = pluginManager.reload(id);
        log.info("已重载插件：{} {} ({})",
                handle.descriptor().getId(),
                handle.descriptor().getVersion(),
                handle.jarPath());
    }

    private void reloadAllPlugins() throws Exception {
        pluginManager.reloadAll();
        log.info("已重载全部插件");
    }

    private void listPlugins() {
        var plugins = pluginManager.plugins();
        if (plugins.isEmpty()) {
            log.info("当前没有已加载插件。");
            return;
        }
        log.info("已加载插件：");
        for (PluginHandle plugin : plugins) {
            log.info("- {} {} | {} | {}",
                    plugin.descriptor().getId(),
                    plugin.descriptor().getVersion(),
                    plugin.descriptor().getName(),
                    plugin.jarPath());
        }
    }

    private void printBanner() {
        log.info("ZeroBot 控制台已就绪，输入 help 查看命令。");
    }

    private void printHelp() {
        log.info("可用命令：");
        log.info("  help                         查看帮助。");
        log.info("  plugin list                  查看已加载插件。");
        log.info("  plugin load <jar>            加载插件 JAR。");
        log.info("  plugin unload <id>           卸载插件。");
        log.info("  plugin reload <id>           重载插件。");
        log.info("  plugin reload-all            重载全部插件。");
        log.info("  stop                         停止 ZeroBot。");
    }

    private void printPluginHelp() {
        log.info("插件命令：");
        log.info("  plugin list");
        log.info("  plugin load <jar>");
        log.info("  plugin unload <id>");
        log.info("  plugin reload <id>");
        log.info("  plugin reload-all");
    }
}
