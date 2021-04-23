package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import players.Party;
import players.PartyHandler;
import sheets.PlotPointHandler;
import sheets.SheetsHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointCommand implements CommandExecutor {
    @Command(aliases = {"~p", "~pp", "~plot", "~plotpoints"}, description = "Modifies the plot points of a user", privateMessages = false, usage = "~p add|sub|set <usermention> [number]")
    public void processCommandType(Object[] params, MessageAuthor author, TextChannel channel, Server server, Message message) {
        List<User> targets = new ArrayList<>();
        CommandType command = CommandType.GET;
        int amount = 1;
        String party = "";
        for (Object arg : params) {
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
            else if (arg instanceof Long) {
                amount = Math.toIntExact((long) arg);
            }
            else if (PartyHandler.getParties().stream().map(Party::getName).anyMatch(s -> s.equals(arg))) {
                party = (String) arg;
            }
        }
        if (message.getMentionedUsers().isEmpty() && author.asUser().isPresent()) {
            targets.add(author.asUser().get());
        }
        else {
            targets = message.getMentionedUsers();
        }
        executeCommand(command, targets, amount, author, channel);
    }

    /**
     * Execute a command based on the command type. If an invalid command is entered, send an error embed message
     *
     * @param commandType The type of command to execute
     * @param targets     The users to affect with the command
     * @param amount      The number of plot points to add or set
     * @param author      The author of the message containing the command
     * @param channel     The channel the command was sent from
     */
    private void executeCommand(CommandType commandType, List<User> targets, int amount, MessageAuthor author, TextChannel channel) {
        if (commandType == CommandType.ADD) {
            addPlotPointsAndSendSummary(targets, amount, channel, author);
        }
        else if (commandType == CommandType.SUB) {
            addPlotPointsAndSendSummary(targets, amount * -1, channel, author);
        }
        else if (commandType == CommandType.SET) {
            setPlotPointsAndSendSummary(targets, amount, channel, author);
        }
        else {
            if (author.asUser().isPresent()) {
                channel.sendMessage(getPlotPoints(targets.isEmpty() ? Collections.singletonList(author.asUser().get()) : targets, channel, author));
            }
        }
    }

    /**
     * Sets plot points for the specified users and sends an embed describing the changes
     *
     * @param playersToModify The users to set plot points for
     * @param amount          The number of plot points the users will have
     * @param channel         The channel to send the embed to
     * @param author          The author of the message
     */
    private void setPlotPointsAndSendSummary(List<User> playersToModify, int amount, TextChannel channel, MessageAuthor author) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        final List<CompletableFuture<Optional<Integer>>> afterPlotPointUpdateFuture = playersToModify.stream()
                .filter(user -> SheetsHandler.getPlotPoints(user).isPresent())
                .map(user -> PlotPointHandler.setPlotPointsAndLog(changes, new ArrayList<>(), user, amount, SheetsHandler.getPlotPoints(user).get() + amount))
                .collect(Collectors.toList());
        CompletableFuture.allOf(afterPlotPointUpdateFuture.toArray(new CompletableFuture[]{})).thenAcceptAsync(unused -> channel.sendMessage(PlotPointHandler.generateEmbed(changes, channel, author)));
    }

    /**
     * Adds plot points to the specified users and sends an embed describing the changes
     *
     * @param targets The users to add plot points to
     * @param amount  The number of plot points to add
     * @param channel The channel to send the embed to
     * @param author  The message author that invoked the command
     */
    private void addPlotPointsAndSendSummary(List<User> targets, int amount, TextChannel channel, MessageAuthor author) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        // TODO Log errors
        final List<CompletableFuture<Optional<Integer>>> plotPointUpdateFuture = targets.stream()
                .filter(user -> SheetsHandler.getPlotPoints(user).isPresent())
                .map(user -> PlotPointHandler.addPlotPointsToUser(amount, changes, new ArrayList<>(), user))
                .collect(Collectors.toList());
        CompletableFuture.allOf(plotPointUpdateFuture.toArray(new CompletableFuture[]{}))
                .thenApply(unused -> channel.sendMessage(PlotPointHandler.generateEmbed(changes, channel, author)));
    }

    /**
     * Gets the number of plot points for a list of users
     *
     * @param users   The list of users to check
     * @param channel The channel the command was sent from
     * @param author  The author that issued the command
     * @return An embed with the number of plot points for each user in fields
     */
    private EmbedBuilder getPlotPoints(List<User> users, Channel channel, MessageAuthor author) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Plot Points!");
        builder.setAuthor(author);
        for (User user : users) {
            SheetsHandler.getPlotPoints(user).ifPresent(points -> builder.addField(PlotPointHandler.getUsernameInChannel(user, channel), String.valueOf(points)));
        }
        return builder;
    }

    public enum CommandType {
        ADD, SUB, SET, ADDHERE, GET, ADDALL
    }

}
