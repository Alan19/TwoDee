package discord;

import logic.CommandHandler;
import logic.PlotPointEnhancementHelper;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;

public class TwoDee {

    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("resources/bot.properties"));
            String token = prop.getProperty("token");
            new DiscordApiBuilder().setToken(token).login().thenAccept(api -> {
                //Send startup messsage
                new MessageBuilder()
                        .setContent(getStartupMessage())
                        .send(api.getTextChannelById("484544303247523840").get());
                // Print the invite url of your bot
                out.println("You can invite the bot by using the following url: " + api.createBotInvite());

                //Listen for commands
                api.addMessageCreateListener(event -> {
                            if (event.getMessage().getContent().startsWith("~")) {
                                new CommandHandler(event.getMessage().getContent(), event.getMessage().getAuthor(), event.getChannel(), api);
                            }
                        }
                );

                //Listen for a user reacting an X to a message
                api.addReactionAddListener(event -> {
                    event.requestMessage().thenAcceptAsync(message -> {
                        Reaction reaction = event.getReaction().get();
                        if (reaction.getEmoji().equalsEmoji("❌") && reaction.containsYou() && !event.getUser().isYourself()){
                            message.delete();
                        }
                    });
                });

                //Listen for a user
                api.addReactionAddListener(event -> {
                    PlotPointEnhancementHelper pHelper = new PlotPointEnhancementHelper();
                    ArrayList<String> enhancementEmojis = new ArrayList<>(pHelper.getOneToTwelveEmojiMap().keySet());
                    boolean botDidNotAdd = !event.getUser().isBot();
                    if (!botDidNotAdd){
                        return;
                    }
                    AtomicBoolean botAlreadyAdded = new AtomicBoolean(false);
                    event.getReaction().ifPresent(reaction -> botAlreadyAdded.set(reaction.containsYou()));
//                    event.requestMessage().thenAcceptAsync(message -> {
//                        System.out.println("Checking value...");
//                        int rollVal = Integer.parseInt(message.getEmbeds().get(0).getFields().get(5).getValue());
//                        int toAdd = pHelper.getOneToTwelveEmojiMap().get(event.getReaction().get().getEmoji().asUnicodeEmoji());
//                        EmbedBuilder embedBuilder = new EmbedBuilder()
//                                .setAuthor(event.getUser())
//                                .addField("Enhancing roll...", rollVal + " → " + (rollVal + toAdd));
//                        new MessageBuilder()
//                                .setEmbed(embedBuilder)
//                                .send(event.getChannel());
//                    });
                    try {
                        Message message = event.requestMessage().get();
                        System.out.println("Checking value...");
                        int rollVal = Integer.parseInt(message.getEmbeds().get(0).getFields().get(4).getValue());
                        Emoji emoji = event.getReaction().get().getEmoji();
                        int toAdd = 0;
                        if (emoji.isCustomEmoji()){
                            String tag = emoji.asKnownCustomEmoji().get().getMentionTag();
                            String trimmedEmoji = tag.substring(2, tag.length() - 1);
                            toAdd = pHelper.getOneToTwelveEmojiMap().get(trimmedEmoji);
                        }
                        else {
                            String unicodeEmoji = emoji.asUnicodeEmoji().get();
                            toAdd = pHelper.getOneToTwelveEmojiMap().get(unicodeEmoji);
                        }

                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setAuthor(event.getUser())
                                .addField("Enhancing roll...", rollVal + " → " + (rollVal + toAdd));
                        new MessageBuilder()
                                .setEmbed(embedBuilder)
                                .send(event.getChannel());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });
            })
                    // Log any exceptions that happened
                    .exceptionally(ExceptionLogger.get());
        } catch (IOException e) {
            e.printStackTrace();
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


}
