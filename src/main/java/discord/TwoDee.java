package discord;

import io.vavr.control.Try;
import listeners.LanguageAutocomplete;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import pw.mihou.velen.interfaces.Velen;
import slashcommands.SlashCommandRegister;

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
            final Velen velen = SlashCommandRegister.setupVelen();
            new DiscordApiBuilder()
                    .setToken(token)
                    .setAllIntentsExcept(Intent.GUILD_PRESENCES)
                    .setUserCacheEnabled(true)
                    .addListener(velen)
                    .login().thenAccept(api -> {
                        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite() + "&scope=bot%20applications.commands");
                        api.addListener(new LanguageAutocomplete());
                        // Uncomment this line when a command is altered
                        // TODO do this a smarter way
                        velen.registerAllSlashCommands(api).exceptionally(ExceptionLogger.get());
                        //Send startup messsage
                        Try.of(() -> prop.getProperty("main_channel_id"))
                                .onFailure(Throwable::printStackTrace)
                                .onSuccess(s -> new MessageBuilder()
                                        .setContent(getStartupMessage())
                                        .send(api.getTextChannelById(s).orElseThrow(() -> new IllegalStateException("Failed to find channel for ID: " + s))));
                    })
                    // Log any exceptions that happened
                    .exceptionally(ExceptionLogger.get());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Returns a random dice getResults line
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

    //Returns a random emoji removal getResults line
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

    //Returns a random serverwide emoji removal getResults line
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
