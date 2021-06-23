package logic;

import commander.CommandContext;
import commander.CommandSpec;
import commander.CommandSpecBuilder;
import commands.StopCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StopLogic {
    private static final Logger LOGGER = LogManager.getLogger(StopCommand.class);

    public static CommandSpec getSpec() {
        return CommandSpecBuilder.of("stop")
                .withDescription("Stops the bot!")
                .withUsage("~stop")
                .withHandler(StopLogic::stop)
                .build();
    }

    public static void stop(CommandContext context) {
        LOGGER.info("TwoDee is shutting down...");
        System.exit(0);
    }
}
