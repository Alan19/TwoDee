package discord;

import commander.CommandSpecBuilder;
import commander.Commander;
import commands.*;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import listeners.DeleteStatsListener;
import listeners.PlotPointEnhancementListener;
import logic.SnackLogic;
import logic.StopLogic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import slashcommands.SlashCommandListener;
import slashcommands.SlashCommands;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class TwoDee {
    private static final Logger LOGGER = LogManager.getLogger(TwoDee.class);

    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("resources/bot.properties"));
            String token = prop.getProperty("token");
            String channel = prop.getProperty("channel", "484544303247523840");
            new DiscordApiBuilder()
                    .setToken(token)
                    .setAllIntentsExcept(Intent.GUILD_PRESENCES)
                    .login()
                    .thenAccept(api -> {
                        //Send startup messsage
                        new MessageBuilder()
                                .setContent(getStartupMessage())
                                .send(api.getTextChannelById(channel)
                                        .orElseThrow(() -> new IllegalStateException("Failed to find Channel for Id: " + channel))
                                );

                        // Print the invite url of your bot
                        LOGGER.info(String.format("You can invite the bot by using the following url: %s", api.createBotInvite()));
                        //Create command handler
                        CommandHandler cmdHandler = new JavacordHandler(api);
                        cmdHandler.registerCommand(new StatisticsCommand());
                        cmdHandler.registerCommand(new RollCommand());
                        cmdHandler.registerCommand(new TestRollCommand());
                        cmdHandler.registerCommand(new HelpCommand(cmdHandler));
                        cmdHandler.registerCommand(new PlotPointCommand());
                        cmdHandler.registerCommand(new EmojiPurgeCommand());
                        cmdHandler.registerCommand(new EnhancementToggleCommand());
                        cmdHandler.registerCommand(new ReplenishCommand());

                        Commander commander = new Commander(api, "~",
                                StopLogic.getSpec(),
                                SnackLogic.getSpec()
                        );
                        commander.register();

                        //Create listeners
                        api.addListener(new PlotPointEnhancementListener());
                        api.addListener(new SlashCommandListener());
                        DeleteStatsListener deleteStatsListener = new DeleteStatsListener(api);
                        deleteStatsListener.startListening();
                    })
                    // Log any exceptions that happened
                    .exceptionally(ExceptionLogger.get());
        } catch (Throwable e) {
            LOGGER.error("Failed to start TwoDee", e);
        }
    }

    //Returns a random dice roll line
    public static String getRollTitleMessage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("resources/rollLines.txt"))) {
            ArrayList<String> rollLines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                rollLines.add(line);
                line = reader.readLine();
            }
            return rollLines.get(new Random().nextInt(rollLines.size()));
        } catch (IOException e) {
            LOGGER.error("Failed to read roll lines", e);
        }
        return "I'm out of witty lines!";
    }

    //Returns a random startup line
    private static String getStartupMessage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("resources/StartupLines.txt"))) {
            ArrayList<String> startupLines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                startupLines.add(line);
                line = reader.readLine();
            }
            return startupLines.get(new Random().nextInt(startupLines.size()));
        } catch (IOException e) {
            LOGGER.error("Failed to read start up lines", e);
        }
        return "I'm out of witty lines!";
    }

    //Returns a random emoji removal roll line
    public static String getEmojiRemovalMessage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("resources/emojiRemovalLines.txt"))) {
            ArrayList<String> rollLines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                rollLines.add(line);
                line = reader.readLine();
            }
            return rollLines.get(new Random().nextInt(rollLines.size()));
        } catch (IOException e) {
            LOGGER.error("Failed to read emoji removal lines", e);
        }
        return "I'm out of witty lines!";
    }

    //Returns a random serverwide emoji removal roll line
    public static String getServerwideEmojiRemovalMessage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("resources/serverWideEmojiRemovalLines.txt"))) {
            ArrayList<String> rollLines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                rollLines.add(line);
                line = reader.readLine();
            }
            return rollLines.get(new Random().nextInt(rollLines.size()));
        } catch (IOException e) {
            LOGGER.error("Failed to read server wide emoji removal lines", e);
        }
        return "I'm out of witty lines!";
    }
}
