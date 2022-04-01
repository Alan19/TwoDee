package discord;

import io.vavr.control.Try;
import language.LanguageLogic;
import listeners.LanguageAutocompleteListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import pw.mihou.velen.interfaces.Velen;
import slashcommands.SlashCommandRegister;

import java.io.*;
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

            LanguageLogic languageLogic = LanguageLogic.of(new File("resources/languages.json"))
                    .onFailure(error -> LOGGER.error("Failed to load language file", error))
                    .getOrElse(() -> LanguageLogic.of(
                            graph -> LOGGER.warn("Failed to handle update. Errored loading language file"))
                    );

            final Velen velen = SlashCommandRegister.setupVelen(languageLogic);
            new DiscordApiBuilder().setToken(token).setAllIntentsExcept(Intent.GUILD_PRESENCES).setUserCacheEnabled(true).addListener(velen).login().thenAccept(api -> {
                        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite() + "&scope=bot%20applications.commands");
                        // Uncomment this line when a command is altered
                        // TODO do this a smarter way
                        velen.registerAllSlashCommands(api);
                        //Send startup messsage
                        Try.of(() -> prop.getProperty("main_channel_id"))
                                .onFailure(Throwable::printStackTrace)
                                .onSuccess(s -> new MessageBuilder()
                                        .setContent(getStartupMessage())
                                        .send(api.getTextChannelById(s).orElseThrow(() -> new IllegalStateException("Failed to find channel for ID: " + s))));
                        api.addListener(new LanguageAutocompleteListener(languageLogic));
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
