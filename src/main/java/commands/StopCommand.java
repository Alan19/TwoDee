package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;

public class StopCommand implements CommandExecutor {
    @Command(aliases = {"~stop"}, description = "Stops the bot!", async = true, privateMessages = false, usage = "~stop")
    public void onCommand() {
        System.out.println("TwoDee is shutting down...");
        System.exit(1);
    }
}
