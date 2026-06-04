package cn.zerobot.core.console;

import cn.zerobot.core.plugin.PluginDescriptor;
import cn.zerobot.core.plugin.PluginHandle;
import cn.zerobot.core.plugin.PluginManager;
import org.jline.reader.Candidate;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandConsoleCompletionTest {
    @TempDir
    Path tempDir;

    @Test
    void completesPluginIdAfterPluginReload() throws Exception {
        PluginManager pluginManager = new TestPluginManager(tempDir, List.of(plugin("luckperms")));
        CommandConsole console = new CommandConsole(pluginManager);
        var completer = console.new ConsoleCompleter();
        var line = new DefaultParser().parse("plugin reload ", "plugin reload ".length(), Parser.ParseContext.COMPLETE);
        var candidates = new ArrayList<Candidate>();

        completer.complete(null, line, candidates);

        assertThat(candidates).extracting(Candidate::value).contains("luckperms");
    }

    private PluginHandle plugin(String id) {
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setId(id);
        descriptor.setName("ZeroBot LuckPerms");
        descriptor.setVersion("1.0.0");
        descriptor.setMain("test.Plugin");
        return new PluginHandle(descriptor, tempDir.resolve("plugins/" + id + ".jar"), null, null);
    }

    private static class TestPluginManager extends PluginManager {
        private final Collection<PluginHandle> plugins;

        TestPluginManager(Path tempDir, Collection<PluginHandle> plugins) {
            super(tempDir.resolve("plugins"), tempDir, null);
            this.plugins = plugins;
        }

        @Override
        public Collection<PluginHandle> plugins() {
            return plugins;
        }
    }
}
