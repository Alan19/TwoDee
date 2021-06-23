package logic;

import commander.CommandContext;
import commander.CommandResponse;
import commander.CommandSpec;
import commander.CommandSpecBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StopLogic {
    private static final Logger LOGGER = LogManager.getLogger(StopLogic.class);

    public static CommandSpec getSpec() {
        return CommandSpecBuilder.of("stop")
                .withDescription("Stops the bot!")
                .withUsage("~stop")
                .withHandler(StopLogic::stop)
                .build();
    }

    public static Optional<CommandResponse> stop(CommandContext context) {
        LOGGER.info("TwoDee is shutting down...");
        context.getApi()
                .getThreadPool()
                .getScheduler()
                .schedule(() -> System.exit(0), 5, TimeUnit.SECONDS);
        return Optional.of(CommandResponse.of("TwoDee is shutting down..."));
    }
}
