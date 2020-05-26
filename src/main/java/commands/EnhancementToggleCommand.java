package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EnhancementToggleCommand implements CommandExecutor {

    public static final String ENHANCEMENT = "enhancement";
    public static final String ENHANCEMENT_OVERRIDE = "enhancement_override";
    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";
    public static final String ON = "on";

    @Command(aliases = {"~te"}, description = "Toggles enhancement reactions from appearing for player rolls", async = true, privateMessages = false, usage = "~te on|off|clear|player pings")
    public void onCommand(Message message, MessageAuthor author, TextChannel channel, String[] args, Server server, DiscordApi api) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("resources/bot.properties"));
        if (message.getContent().matches("~te (on|off|clear|.+)")) {
            //Add player(s) to override
            if (!message.getMentionedUsers().isEmpty()) {
                addPlayersToOverride(message, channel, server, api, prop);
            }
            //On/off/clear
            else {
                if (args[0].equals("clear")) {
                    prop.setProperty(ENHANCEMENT_OVERRIDE, "");
                    channel.sendMessage("Enhancement overrides have been cleared. Enhancements are currently " + (prop.getProperty(ENHANCEMENT).equals(ON) ? ENABLED : DISABLED) + ".");
                }
                else {
                    toggleEnhancementEnable(channel, prop, args[0]);
                }
            }
        }
        prop.store(new FileOutputStream("resources/bot.properties"), null);
    }

    private void toggleEnhancementEnable(TextChannel channel, Properties prop, String arg) {
        prop.setProperty(EnhancementToggleCommand.ENHANCEMENT, arg);
        channel.sendMessage("Enhancements are now " + (prop.getProperty(ENHANCEMENT).equals(ON) ? ENABLED : DISABLED) + ".");
    }

    private void addPlayersToOverride(Message message, TextChannel channel, Server server, DiscordApi api, Properties prop) {
        String overrideUsers = prop.getProperty(ENHANCEMENT_OVERRIDE, "");
        List<User> users = new ArrayList<>();
        Arrays.stream(overrideUsers.split(",")).forEach(s -> api.getCachedUserById(s).ifPresent(users::add));
        message.getMentionedUsers().stream().filter(mentionedUser -> !users.contains(mentionedUser)).forEach(users::add);
        prop.setProperty(ENHANCEMENT_OVERRIDE, users.stream().map(DiscordEntity::getIdAsString).reduce((s, s2) -> s + "," + s2).orElse(""));
        channel.sendMessage("Overrides have been added for " + users.stream().map(user -> user.getDisplayName(server)).reduce((s, s2) -> s + ", " + s2) + ". Enhancements are currently " + (prop.getProperty(ENHANCEMENT).equals(ON) ? ENABLED : DISABLED) + ".");
    }
}
