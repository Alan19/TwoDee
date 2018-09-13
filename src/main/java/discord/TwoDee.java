package discord;

import logic.CommandHandler;
import logic.DiceRoller;
import logic.StatisticsGenerator;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import sheets.SheetsQuickstart;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import static java.lang.System.out;

public class TwoDee {

    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("src/main/resources/bot.properties"));
            String token = prop.getProperty("token");
            new DiscordApiBuilder().setToken(token).login().thenAccept(api -> {
                // Add a listener that outputs statistics when you prefix a message with ~s
                api.addMessageCreateListener(event -> {
                    if (event.getMessage().getContent().startsWith("~s")) {
                        StatisticsGenerator statistics = new StatisticsGenerator(event.getMessage().getContent());
                        new MessageBuilder()
                                .setEmbed(statistics.generateStatistics(event.getMessage().getAuthor()))
                                .send(event.getChannel());
                    }
                });

                //Add a listener that outputs a dice roll result
                api.addMessageCreateListener(event -> {
                    if (messageStartsWith(event, "~r")) {
                        DiceRoller diceRoller = new DiceRoller(event.getMessage().getContent());
                        new MessageBuilder()
                                .setEmbed(diceRoller.generateResults(event.getMessage().getAuthor()))
                                .send(event.getChannel());
                    }
                });

                //Send startup messsage
                new MessageBuilder()
                        .setContent(getStartupMessage())
                        .send(api.getTextChannelById("484544303247523840").get());
                // Print the invite url of your bot
                out.println("You can invite the bot by using the following url: " + api.createBotInvite());

                //Listen for shutdown command
                api.addMessageCreateListener(event -> {
                    if (messageStartsWith(event, "~kill")){
                        new MessageBuilder()
                                .setContent("TwoDee shutting down...")
                                .send(event.getChannel());
                        api.disconnect();
                        System.exit(1);
                    }
                });

                //Add listener for fetching skills
                api.addMessageCreateListener(event -> {
                    if (messageStartsWith(event, "~f")){
                        try {
                            SheetsQuickstart aSheet = new SheetsQuickstart(event.getMessage().getAuthor().getIdAsString());
                        } catch (IOException | GeneralSecurityException e) {
                            e.printStackTrace();
                        }
                    }
                });
            })
                    // Log any exceptions that happened
                    .exceptionally(ExceptionLogger.get());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean messageStartsWith(MessageCreateEvent event, String s) {
        return event.getMessage().getContent().startsWith(s);
    }

    //Returns a random dice roll line
    public static String getRollTitleMessage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/rollLines.txt"))) {
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
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/StartupLines.txt"))) {
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
}
