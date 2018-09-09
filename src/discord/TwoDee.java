package discord;

import logic.DiceRoller;
import logic.StatisticsGenerator;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import static java.lang.System.out;

public class TwoDee {

    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("src/bot.properties"));
            String token = prop.getProperty("token");
            new DiscordApiBuilder().setToken(token).login().thenAccept(api -> {
                // Add a listener that outputs statistics when you prefix a message with ~s
                api.addMessageCreateListener(event -> {
                    if (event.getMessage().getContent().startsWith("~s")) {
                        StatisticsGenerator statistics = new StatisticsGenerator(event.getMessage().getContent());
                        new MessageBuilder()
                                .setEmbed(statistics.generateStatistics())
                                .send(event.getChannel());
                    }
                });
                //Add listener for dice roll command
                api.addMessageCreateListener(event -> {
                    if (event.getMessage().getContent().startsWith("~r")){
                        DiceRoller diceRoller = new DiceRoller(event.getMessage().getContent());
                    }
                })
                // Print the invite url of your bot
                out.println("You can invite the bot by using the following url: " + api.createBotInvite());
            })
                    // Log any exceptions that happened
                    .exceptionally(ExceptionLogger.get());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Returns a random dice roll line
    public static String getRollTitleMessage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/rollLines.txt"))) {
            ArrayList<String> rollLines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null)
            {
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
