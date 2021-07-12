package discord;

import commands.*;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import listeners.DeleteStatsListener;
import listeners.PlotPointEnhancementListener;
import logic.BleedLogic;
import logic.SnackLogic;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.util.logging.ExceptionLogger;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class TwoDee {

    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("resources/bot.properties"));
            String token = prop.getProperty("token");
            String channel = prop.getProperty("channel", "484544303247523840");
            final Velen velen = setupVelen();
            new DiscordApiBuilder().setToken(token).setAllIntentsExcept(Intent.GUILD_PRESENCES).addListener(velen).login().thenAccept(api -> {
                velen.registerAllSlashCommands(api);
                //Send startup messsage
                new MessageBuilder()
                        .setContent(getStartupMessage())
                        .send(api.getTextChannelById(channel)
                                .orElseThrow(() -> new IllegalStateException("Failed to find Channel for Id: " + channel))
                        );
                // Print the invite url of your bot
                System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
                CommandHandler cmdHandler = new JavacordHandler(api);
                cmdHandler.registerCommand(new StatisticsCommand());
                cmdHandler.registerCommand(new RollCommand());
                cmdHandler.registerCommand(new TestRollCommand());
                cmdHandler.registerCommand(new DoomCommand());
                cmdHandler.registerCommand(new StopCommand());
                cmdHandler.registerCommand(new HelpCommand(cmdHandler));
                cmdHandler.registerCommand(new PlotPointCommand());
                cmdHandler.registerCommand(new EnhancementToggleCommand());
                cmdHandler.registerCommand(new ReplenishCommand());

                //Create listeners
                api.addListener(new PlotPointEnhancementListener());
                DeleteStatsListener deleteStatsListener = new DeleteStatsListener(api);
                deleteStatsListener.startListening();

            })
                    // Log any exceptions that happened
                    .exceptionally(ExceptionLogger.get());
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private static Velen setupVelen() {
        Velen velen = Velen.builder().setDefaultPrefix(".").build();
        SnackLogic snackLogic = new SnackLogic();
        VelenCommand.ofHybrid("snack", "Gives you a snack!", velen, snackLogic, snackLogic);
        BleedLogic bleedLogic = new BleedLogic();
        VelenCommand.ofHybrid("bleed", "Applies plot point bleed!", velen, bleedLogic, bleedLogic).addOptions(SlashCommandOption.create(SlashCommandOptionType.MENTIONABLE, "target", "The party to bleed", true), SlashCommandOption.create(SlashCommandOptionType.INTEGER, "modifier", "The bonus or penalty on the bleed", false)).setServerOnly(true, 468046159781429250L).attach();
        return velen;
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return "I'm out of witty lines!";
    }
}
