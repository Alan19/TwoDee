package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StopCommand implements CommandExecutor {
    private static final Logger LOGGER = LogManager.getLogger(StopCommand.class);

    @Command(aliases = {"~stop"}, description = "Stops the bot!", async = true, privateMessages = false, usage = "~stop")
    public void onCommand() {
        LOGGER.info("TwoDee is shutting down...");
        System.exit(1);
    }
}
