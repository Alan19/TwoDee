package logic;

import com.google.api.services.sheets.v4.model.ValueRange;
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
import sheets.SheetsQuickstart;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
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

    private static boolean validSkill(String skillName, String param) {
        String skill = skillName.replaceAll("\\s+", "").toLowerCase();
        return skill.equals(param);
    }

    //Checks to see if any parameters are words to find appropriate replacements in the Google doc
    private String handleCommand(String command) {
        String convertedCommand = getConvertedCommand(command);
        if (convertedCommand == null) return null;
        System.out.println(convertedCommand);
        return convertedCommand;
    }

    private String getConvertedCommand(String command) {
        String[] paramArray = command.split(" ");
        for (int i = 0; i < paramArray.length; i++) {
            //If a parameter is a string, look into sheets for appropriate dice
            if (paramArray[i].chars().allMatch(Character::isLetter) && skillExists(paramArray, i)) {
                return null;
            }
        }
        return String.join(" ", paramArray);
    }

    //If the skill exists, renames array element and returns true. Otherwise, returns false.
    private boolean skillExists(String[] paramArray, int i) {
        try {
            SheetsQuickstart characterInfo = new SheetsQuickstart(author.getIdAsString());
            String change = retrieveDice(paramArray[i].toLowerCase(), characterInfo.getResult());
            //If skill is not found, kill function immediately
            if (change == null) {
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setAuthor(author)
                                .setDescription("Cannot find skill: " + paramArray[i])
                        )
                        .send(channel);
                return true;
            }
            paramArray[i] = change;

        } catch (IOException | GeneralSecurityException e) {
            new MessageBuilder()
                    .setContent("Cannot retrieve spreadsheet!")
                    .send(channel);
            e.printStackTrace();
        }
        return false;
    }

    private void commandSelector(String message) {
        message = message.replaceAll("\\s+", " ");
        String prefix = message.split(" ")[0];
        PlotPointEnhancementHelper pHelper = new PlotPointEnhancementHelper();
        switch (prefix) {
            //Statistics listener
            case "~s":
            case "~stat":
            case "~stats":
            case "~statistics":
                message = handleCommand(message);
                assert message != null;
                StatisticsContext context = new StatisticsContext(message);
                try {
                    Message statsMessage = new MessageBuilder()
                            .setEmbed(
                                    context.getEmbedBuilder()
                                            .setAuthor(author)
                                            .setTitle(TwoDee.getRollTitleMessage())
                                            .setColor(RandomColor.getRandomColor())
                            )
                            .send(channel)
                            .get();
                    statsMessage.addReaction("❌");
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                break;

            //Dice roll listener. Sends extra embeds for plot points and doom
            case "~r":
            case "~roll":
                //Special case for GM
                try {
                    Properties prop = new Properties();
                    prop.load(new FileInputStream("resources/bot.properties"));
                    if (author.getIdAsString().equals(prop.getProperty("gameMaster"))) {
                        if (commandContainsPlotDice(message)) {
                            DoomWriter writer = new DoomWriter();
                            writer.addDoom(getPlotPointsSpent(message) * -1);
                            EmbedBuilder doomEmbed = writer.generateDoomEmbed();
                            channel.sendMessage(doomEmbed);
                        }
                    } else {
                        message = handleCommand(message);
                        assert message != null;
                        DiceRoller diceRoller = new DiceRoller(message);
                        deductPlotPoints(message);

                        Message rollMessage = new MessageBuilder()
                                .setEmbed(diceRoller.generateResults(author))
                                .send(channel)
                                .get();
                        if (diceRoller.getDoom() >= 1) {
                            rollMessage.addReaction("✴");
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
                message = handleCommand(message);
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

    //Convert a skill into a dice value (euphemanu -> d12)
    private String retrieveDice(String param, ValueRange result) {
        List<List<Object>> values = result.getValues();
        for (List<Object> skill : values) {
            if (skill.size() == 2 && validSkill((String) skill.get(0), param)) {
                Integer skillVal = Integer.parseInt((String) skill.get(1));
                return reduceDice(skill, skillVal);

            }
        }
        return null;
    }

    /*
    If a dice is over d12 reduce dice to facets % 12 d12 dice and the remainder as a dice if the remainder is greater
    than 2. For example (d16 -> d12 d4, d14 -> d12, d100 -> 8d12 d4)
     */
    private String reduceDice(List<Object> skill, Integer skillVal) {

        if (skillVal > 12) {
            StringBuilder pool = new StringBuilder();
            for (int i = 0; i < skillVal / 12; i++) {
                pool.append("d12 ");
            }
            if (skillVal % 12 > 2) {
                pool.append("d").append(skillVal % 12);
            }
            return pool.toString().trim();
        }
        return "d" + skill.get(1);
    }
}
