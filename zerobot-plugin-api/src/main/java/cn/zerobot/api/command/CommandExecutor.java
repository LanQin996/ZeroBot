package cn.zerobot.api.command;

/**
 * Handles a command declared in plugin.yml.
 */
@FunctionalInterface
public interface CommandExecutor {
    /**
     * @return true if the command was handled, false to let ZeroBot send the command usage.
     */
    boolean execute(CommandContext context) throws Exception;
}
