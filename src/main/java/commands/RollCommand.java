package commands;

import com.vdurmont.emoji.EmojiParser;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import dicerolling.DiceRoller;
import doom.DoomWriter;
import logic.CommandProcessor;
import logic.PlotPointEnhancementHelper;
import logic.RandomColor;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import sheets.PPManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class RollCommand implements CommandExecutor {
    @Command(aliases = {"~r", "~roll"}, description = "A command that allows you to roll dice\n\tdie: A string representing a die. Some die examples are d4, pd12, 3d12, kd12, +3.", privateMessages = false, usage = "~r die|skill [die|skill ...]")
    public void onCommand(MessageAuthor author, Message message, TextChannel channel) {
        String messageContent = message.getContent();
        //Special case for GM
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("resources/bot.properties"));
            //Subtract doom points if GM is rolling
            if (author.getIdAsString().equals(prop.getProperty("gameMaster"))) {
                DiceRoller diceRoller = new DiceRoller(messageContent);
                new MessageBuilder()
                        .setEmbed(diceRoller.generateResults(message.getAuthor()))
                        .send(channel);
                if (commandContainsPlotDice(messageContent)) {
                    DoomWriter writer = new DoomWriter();
                    EmbedBuilder doomEmbed = writer.addDoom(getPlotPointsSpent(messageContent) * -1);
                    channel.sendMessage(doomEmbed);
                }
            }
            //Players
            else {
                String convertedMessage = new CommandProcessor(author, channel).handleCommand(messageContent);
                DiceRoller diceRoller = new DiceRoller(convertedMessage);
                deductPlotPoints(messageContent, author, channel);

                new MessageBuilder()
                        .setEmbed(diceRoller.generateResults(author))
                        .send(channel)
                        .thenAcceptAsync(sentMessage -> {
                            if (diceRoller.getDoom() >= 1) {
                                sentMessage.addReaction(EmojiParser.parseToUnicode(":eight_pointed_black_star:"));
                            }
                            EmbedBuilder doomEmbed = diceRoller.addPlotPoints(author);
                            if (doomEmbed != null) {
                                new MessageBuilder()
                                        .setEmbed(doomEmbed)
                                        .send(channel);
                                new MessageBuilder()
                                        .setEmbed(diceRoller.addDoom(diceRoller.getDoom()))
                                        .send(channel);
                            }
                            new PlotPointEnhancementHelper().addPlotPointEnhancementEmojis(sentMessage);
                        });

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean commandContainsPlotDice(String message) {
        for (String arg : message.split(" ")) {
            if (arg.contains("pd")) {
                return true;
            }
        }
        return false;
    }

    private int getPlotPointsSpent(String message) {
        String[] commandParams = message.split(" ");
        int ppUsage = 0;
        for (String args : commandParams) {
            if (args.contains("pd")) {
                if (args.startsWith("pd")) {
                    ppUsage += Integer.parseInt(args.replaceAll("[^\\d.]", "")) / 2;
                }
                else {
                    int i = 0;
                    StringBuilder multiplier = new StringBuilder();
                    while (args.charAt(i) != 'p') {
                        multiplier.append(args.charAt(i));
                        i++;
                    }
                    ppUsage += Integer.parseInt(multiplier.toString()) * Integer.parseInt(args.substring(args.indexOf("pd") + 2)) / 2;
                }
            }
        }
        return ppUsage;
    }

    private void deductPlotPoints(String message, MessageAuthor author, TextChannel channel) {
        String authorID = author.getIdAsString();
        if (commandContainsPlotDice(message)) {
            PPManager handler = new PPManager();
            int previous = handler.getPlotPoints(authorID);
            int current = handler.setPlotPoints(authorID, handler.getPlotPoints(authorID) - getPlotPointsSpent(message));
            new MessageBuilder()
                    .setEmbed(new EmbedBuilder()
                            .setAuthor(author)
                            .setTitle("Plot points!")
                            .setColor(RandomColor.getRandomColor())
                            .setDescription(previous + " → " + current))
                    .send(channel);
        }
    }

}
