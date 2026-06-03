package cn.zerobot.app;

import cn.zerobot.core.config.ZeroBotConfig;
import cn.zerobot.core.console.CommandConsole;
import cn.zerobot.core.event.DefaultEventBus;
import cn.zerobot.core.napcat.NapCatClient;
import cn.zerobot.core.plugin.PluginManager;
import cn.zerobot.core.runtime.DefaultBotContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class ZeroBotApplication {
    public static void main(String[] args) throws Exception {
        var log = LoggerFactory.getLogger(ZeroBotApplication.class);
        Path configPath = (args.length > 0 ? Path.of(args[0]) : Path.of("config.yml"))
                .toAbsolutePath()
                .normalize();
        Path baseDir = configPath.getParent();
        ZeroBotConfig config = ZeroBotConfig.load(configPath);
        Path pluginsDir = resolve(baseDir, config.getPluginsDir());

        ObjectMapper mapper = new ObjectMapper();
        DefaultEventBus eventBus = new DefaultEventBus();
        NapCatClient client = new NapCatClient(config.getNapcat(), eventBus, mapper);
        DefaultBotContext context = new DefaultBotContext(log, client, eventBus);
        PluginManager pluginManager = new PluginManager(pluginsDir, context);
        CommandConsole console = new CommandConsole(pluginManager);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            console.close();
            pluginManager.unloadAll();
            client.close();
        }, "zerobot-shutdown"));

        pluginManager.ensurePluginsDir();
        log.info("ZeroBot home: {}", baseDir);
        log.info("Plugin directory: {}", pluginsDir);
        pluginManager.loadAllFromDirectory();
        client.start();
        console.start();
        console.awaitStop();

        pluginManager.unloadAll();
        client.close();
        log.info("ZeroBot stopped");
    }

    private static Path resolve(Path baseDir, String path) throws Exception {
        Path candidate = Path.of(path);
        if (!candidate.isAbsolute()) {
            candidate = baseDir.resolve(candidate);
        }
        Files.createDirectories(candidate);
        return candidate.toAbsolutePath().normalize();
    }
}
