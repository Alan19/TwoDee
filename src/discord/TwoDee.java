package discord;

import logic.StatisticsGenerator;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.io.*;
import java.util.Properties;

import static java.lang.System.*;

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
                        event.getChannel().sendMessage(statistics.generateStatistics());
                    }
                });

                // Print the invite url of your bot
                out.println("You can invite the bot by using the following url: " + api.createBotInvite());
            })
                    // Log any exceptions that happened
                    .exceptionally(ExceptionLogger.get());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
