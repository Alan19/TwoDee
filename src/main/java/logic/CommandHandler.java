package logic;

import com.google.api.services.sheets.v4.model.ValueRange;
import com.vdurmont.emoji.EmojiParser;
import dicerolling.DiceRoller;
import discord.TwoDee;
import doom.DoomHandler;
import doom.DoomWriter;
import statistics.StatisticsContext;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class CommandHandler {

    private DiscordApi api;
    private TextChannel channel;
    private MessageAuthor author;


    public CommandHandler(String content, MessageAuthor author, TextChannel channel, DiscordApi api) {
        this.author = author;
        this.channel = channel;
        this.api = api;
        commandSelector(content);
        System.out.println(content);
    }

    private void commandSelector(String message) {
        message = message.replaceAll("\\s+", " ");
        String prefix = message.split(" ")[0];
        PlotPointEnhancementHelper pHelper = new PlotPointEnhancementHelper();
        switch (prefix) {
            //Dice roll listener. Sends extra embeds for plot points and doom
            case "~r":
            case "~roll":
                //Special case for GM
                try {
                    Properties prop = new Properties();
                    prop.load(new FileInputStream("resources/bot.properties"));
                    //Subtract doom points if GM is rolling
                    if (author.getIdAsString().equals(prop.getProperty("gameMaster"))) {
                        if (commandContainsPlotDice(message)) {
                            DoomWriter writer = new DoomWriter();
                            writer.addDoom(getPlotPointsSpent(message) * -1);
                            EmbedBuilder doomEmbed = writer.generateDoomEmbed();
                            channel.sendMessage(doomEmbed);
                        }
                    } else {
                        message = new CommandProcessor(author, channel).handleCommand(message);
                        assert message != null;
                        DiceRoller diceRoller = new DiceRoller(message);
                        deductPlotPoints(message);

                        Message rollMessage = new MessageBuilder()
                                .setEmbed(diceRoller.generateResults(author))
                                .send(channel)
                                .get();
                        if (diceRoller.getDoom() >= 1) {
                            rollMessage.addReaction(EmojiParser.parseToUnicode(":eight_pointed_black_star:"));
                        }
                        EmbedBuilder doomEmbed = diceRoller.addPlotPoints(author, api);
                        if (doomEmbed != null) {
                            new MessageBuilder()
                                    .setEmbed(doomEmbed)
                                    .send(channel);
                            new MessageBuilder()
                                    .setEmbed(diceRoller.addDoom(diceRoller.getDoom()))
                                    .send(channel);
                        }
                        pHelper.addPlotPointEnhancementEmojis(rollMessage);
                    }
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                break;

            //Version of ~r that doesn't generate doom
            case "~t":
            case "~test":
                message = new CommandProcessor(author, channel).handleCommand(message);
                assert message != null;
                DiceRoller doomlessRoller = new DiceRoller(message);
                try {
                    Message testMessage = new MessageBuilder()
                            .setEmbed(doomlessRoller.generateResults(author))
                            .send(channel)
                            .get();
                    pHelper.addPlotPointEnhancementEmojis(testMessage);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                break;


            //Doom management
            case "~d":
            case "~doom":
                DoomHandler doomHandler = new DoomHandler(message);
                new MessageBuilder()
                        .setEmbed(doomHandler.newDoom())
                        .send(channel);
                break;

            //Kill the bot!
            case "~stop":
                new MessageBuilder()
                        .setContent("TwoDee shutting down...")
                        .send(channel);
                api.disconnect();
                System.exit(1);
                break;

            //Add, subtract, and set plot points
            case "~p":
            case "~pp":
            case "~plot":
            case "~plotpoints":
                PlotPointHandler plotPointHandler = new PlotPointHandler(message, author, api);
                new MessageBuilder()
                        .setEmbed(plotPointHandler.processCommandType())
                        .send(channel);
                break;

            //Help command
            case "~h":
            case "~help":
                HelpCommand helpCommand;
                if (message.split(" ").length == 2) {
                    helpCommand = new HelpCommand(message.split(" ")[1], author);
                } else {
                    helpCommand = new HelpCommand(author);
                }

                new MessageBuilder()
                        .setEmbed(helpCommand.getHelp())
                        .send(channel);
                break;

            case "~snack":
                SnackCommand snackCommand = new SnackCommand(author);
                new MessageBuilder()
                        .setEmbed(snackCommand.dispenseSnack())
                        .send(channel);
                break;

            default:
                return;
        }
        new EmbedBuilder()
                .setAuthor(author)
                .setDescription("Command not recognized");
    }



    private void deductPlotPoints(String message) {
        if (commandContainsPlotDice(message)) {
            String ppCommand = generatePlotPointCommand(message);
            PlotPointHandler ppHandler = new PlotPointHandler(ppCommand, author, api);
            EmbedBuilder ppNotification = ppHandler.processCommandType();
            channel.sendMessage(ppNotification);
        }
    }

    /**
     * If a user uses plot dice, remove plot points equal to half of the dice's total value
     *
     * @param message
     * @return
     */
    private String generatePlotPointCommand(String message) {
        int ppUsage = getPlotPointsSpent(message);
        return "~p sub " + ppUsage;
    }

    private int getPlotPointsSpent(String message) {
        String[] commandParams = message.split(" ");
        int ppUsage = 0;
        for (String args : commandParams) {
            if (args.contains("pd")) {
                if (args.startsWith("pd")) {
                    ppUsage += Integer.parseInt(args.replaceAll("[^\\d.]", "")) / 2;
                } else {
                    int i = 0;
                    String multiplier = "";
                    while (args.charAt(i) != 'p') {
                        multiplier += args.charAt(i);
                        i++;
                    }
                    ppUsage += Integer.parseInt(multiplier) * Integer.parseInt(args.substring(args.indexOf("pd") + 2)) / 2;
                }
            }
        }
        return ppUsage;
    }

    private boolean commandContainsPlotDice(String message) {
        for (String arg : message.split(" ")) {
            if (arg.contains("pd")) {
                return true;
            }
        }
        return false;
    }
}
