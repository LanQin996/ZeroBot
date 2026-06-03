package cn.zerobot.core.console;

import cn.zerobot.core.plugin.PluginHandle;
import cn.zerobot.core.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
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
        printHelp();
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
        String[] parts = line.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        try {
            switch (command) {
                case "help" -> printHelp();
                case "plugins" -> listPlugins();
                case "load" -> requireArg(command, arg, () -> {
                    PluginHandle handle = pluginManager.load(Path.of(arg));
                    log.info("Loaded plugin {}", handle.descriptor().getId());
                });
                case "unload" -> requireArg(command, arg, () -> pluginManager.unload(arg));
                case "reload" -> requireArg(command, arg, () -> pluginManager.reload(arg));
                case "reload-all" -> pluginManager.reloadAll();
                case "stop", "exit", "quit" -> close();
                default -> log.info("Unknown command: {}", command);
            }
        } catch (Exception e) {
            log.warn("Command failed: {}", line, e);
        }
    }

    private void requireArg(String command, String arg, ThrowingRunnable runnable) throws Exception {
        if (arg.isBlank()) {
            log.info("Usage: {} <arg>", command);
            return;
        }
        runnable.run();
    }

    private void listPlugins() {
        var plugins = pluginManager.plugins();
        if (plugins.isEmpty()) {
            log.info("No plugins loaded");
            return;
        }
        for (PluginHandle plugin : plugins) {
            log.info("{} {} ({})", plugin.descriptor().getId(), plugin.descriptor().getVersion(), plugin.jarPath());
        }
    }

    private void printHelp() {
        log.info("Commands: help, plugins, load <jar>, unload <id>, reload <id>, reload-all, stop");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
