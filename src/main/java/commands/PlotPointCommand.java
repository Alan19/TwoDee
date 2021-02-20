package commands;

import com.vdurmont.emoji.EmojiParser;
import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import logic.RandomColor;
import logic.UserInfo;
import org.apache.commons.lang3.math.NumberUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import sheets.PlotPointManager;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointCommand implements CommandExecutor {

    private static String mainChannelID;

    static {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("resources/bot.properties"));
            mainChannelID = prop.getProperty("main_channel_id", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final UserInfo userInfo = new UserInfo();
    private final DiscordApi api;
    private Server server;

    public PlotPointCommand(DiscordApi api) {
        this.api = api;
    }

    @Command(aliases = {"~p", "~pp", "~plot", "~plotpoints"}, description = "Modifies the plot points of a user", privateMessages = false, usage = "~p [user_mention ...] add|sub|set|addall|addhere [number]")
    public void processCommandType(String[] params, DiscordApi api, MessageAuthor author, Message message, TextChannel channel) {
        List<String> targets = new ArrayList<>();
        server = message.getServer().get();
        CommandType command = CommandType.GET;
        int amount = 1;
        for (User user : message.getMentionedUsers()) {
            targets.add(user.getIdAsString());
        }
        if (targets.isEmpty()) {
            targets.add(author.getIdAsString());
        }
        for (String arg : params) {
            if ("add".equals(arg)) {
                command = CommandType.ADD;
            }
            else if ("addhere".equals(arg)) {
                command = CommandType.ADDHERE;
            }
            else if ("sub".equals(arg)) {
                command = CommandType.SUB;
            }
            else if ("set".equals(arg)) {
                command = CommandType.SET;
            }
            else if ("addall".equals(arg)) {
                command = CommandType.ADDALL;
            }
            else if (NumberUtils.isParsable(arg)) {
                amount = Integer.parseInt(arg);
            }
        }
        for (String targetID : targets) {
            new MessageBuilder()
                    .setEmbed(executeCommand(command, targetID, amount, author))
                    .send(channel);
        }
    }

    //Execute a command based on the command type. If an invalid command is entered, send an error embed message
    private EmbedBuilder executeCommand(CommandType commandType, String target, int number, MessageAuthor author) {
        switch (commandType) {
            case ADD:
                return addPlotPoints(target, number);

            case SUB:
                return addPlotPoints(target, number * -1);

            case ADDHERE:
                return addPlotPointsToPresentUsers(number);

            case SET:
                return setPlotPoints(target, number);

            case ADDALL:
                return addPlotPointsToAll(number);

            default:
                return getPlotPoints(target, author);
        }
    }

    private EmbedBuilder addPlotPointsToAll(int number) {
        EmbedBuilder allPlayerEmbed = new EmbedBuilder()
                .setTitle("Everyone's plot points!");
        for (String ID : userInfo.getUsers()) {
            String message = "";
            try {
                allPlayerEmbed.addField(api.getUserById(ID).get().getDisplayName(server), PlotPointManager.getPlotPoints(ID) + " → " +
                        PlotPointManager.setPlotPoints(ID, PlotPointManager.getPlotPoints(ID) + number));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            allPlayerEmbed.setDescription(message);
            Random random = new Random();
            allPlayerEmbed.setColor(new Color(random.nextFloat(), random.nextFloat(), random.nextFloat()));
        }
        return allPlayerEmbed;
    }

    private EmbedBuilder setPlotPoints(String target, int number) {
        int oldPP = PlotPointManager.getPlotPoints(target);
        int newPP = PlotPointManager.setPlotPoints(target, number);
        return generateEmbed(oldPP, newPP, target);
    }

    /**
     * Adds x plot points to all players in the main voice channel for session replenishment
     *
     * @param number The number of plot points to add to all players in the channel
     * @return An embed with the change in plot points for each player (Alan: 21 => 25)
     */
    private EmbedBuilder addPlotPointsToPresentUsers(int number) {
        EmbedBuilder allPlayerEmbed = new EmbedBuilder()
                .setTitle("Session replenishment!");
        boolean userPresent = false;
        try {

            for (String ID : userInfo.getUsers()) {
                if (isConnected(ID)) {
                    userPresent = true;
                    allPlayerEmbed.addField(api.getUserById(ID).get().getDisplayName(server), PlotPointManager.getPlotPoints(ID) + " → " +
                            PlotPointManager.setPlotPoints(ID, PlotPointManager.getPlotPoints(ID) + number));
                }
                Random random = new Random();
                allPlayerEmbed.setColor(new Color(random.nextFloat(), random.nextFloat(), random.nextFloat()));
                if (!userPresent) {
                    allPlayerEmbed.setDescription("It looks like no one's here " + EmojiParser.parseToUnicode(":pensive:"));
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return allPlayerEmbed;
    }

    private boolean isConnected(String id) throws InterruptedException, ExecutionException {
        return api.getServerVoiceChannelById(mainChannelID).get().isConnected(api.getUserById(id).get());
    }

    private EmbedBuilder addPlotPoints(String target, int number) {
        int oldPP = PlotPointManager.getPlotPoints(target);
        int newPP = PlotPointManager.setPlotPoints(target, PlotPointManager.getPlotPoints(target) + number);
        return generateEmbed(oldPP, newPP, target);
    }

    private EmbedBuilder generateEmbed(int oldPP, int newPP, String target) {
        try {
            User user = api.getUserById(target).get();
            return new EmbedBuilder()
                    .setAuthor(user.getDisplayName(server), "", user.getAvatar())
                    .setTitle("Plot points")
                    .setDescription(oldPP + " → " + newPP)
                    .setColor(RandomColor.getRandomColor());
        } catch (InterruptedException | ExecutionException e) {
            return new EmbedBuilder()
                    .setTitle("User not found!");
        }
    }

    private EmbedBuilder getPlotPoints(String target, MessageAuthor messageAuthor) {
        try {
            User user = api.getUserById(target).get();
            return new EmbedBuilder()
                    .setAuthor(user.getDisplayName(server), "", user.getAvatar())
                    .setTitle("Plot points")
                    .setDescription(String.valueOf(PlotPointManager.getPlotPoints(target)));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new EmbedBuilder()
                    .setAuthor(messageAuthor)
                    .setTitle("User not found!");
        }
    }

    public enum CommandType {
        ADD, SUB, SET, ADDHERE, GET, ADDALL
    }

}
