package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import logic.RandomColor;
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
import sheets.SheetsHandler;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointCommand implements CommandExecutor {
    @Command(aliases = {"~p", "~pp", "~plot", "~plotpoints"}, description = "Modifies the plot points of a user", privateMessages = false, usage = "~p add|sub|set|addall|addhere party|usermention [number]")
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
            else if ("addall".equals(arg)) {
                command = CommandType.ADDALL;
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
        executeCommand(command, targets, amount, author, party, channel);
    }

    /**
     * Execute a command based on the command type. If an invalid command is entered, send an error embed message
     *
     * @param commandType The type of command to execute
     * @param targets     The users to affect with the command
     * @param amount      The number of plot points to add or set
     * @param author      The author of the message containing the command
     * @param party       The party to add plot points to (if any)
     * @param channel     The channel the command was sent from
     */
    private void executeCommand(CommandType commandType, List<User> targets, int amount, MessageAuthor author, String party, TextChannel channel) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        if (commandType == CommandType.ADD) {
            addPlotPointsAndSendSummary(targets, amount, channel, author);
        }
        else if (commandType == CommandType.SUB) {
            addPlotPointsAndSendSummary(targets, amount * -1, channel, author);
        }
        else if (commandType == CommandType.SET) {
            setPlotPointsAndSendSummary(targets, amount, channel, author);
        }
        else if (commandType == CommandType.ADDALL) {
            addPlotPointsToPartyAndSendSummary(PartyHandler.getPartyMembers(party, channel.getApi()), amount, channel, author);
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
     * @param targets The users to set plot points for
     * @param amount  The number of plot points the users will have
     * @param channel The channel to send the embed to
     * @param author  The author of the message
     */
    private void setPlotPointsAndSendSummary(List<User> targets, int amount, TextChannel channel, MessageAuthor author) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        for (User target : targets) {
            final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(target);
            final boolean successfullySet = setPlotPoints(target, amount, channel);
            if (plotPoints.isPresent() && successfullySet) {
                changes.add(Triple.of(target, plotPoints.get(), amount));
            }
        }
        channel.sendMessage(generateEmbed(changes, channel, author));
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
        targets.forEach(target -> modifyUserPlotPoints(amount, channel, changes, target));
        channel.sendMessage(generateEmbed(changes, channel, author));
    }

    /**
     * Adds plot points to an entire party
     *
     * @param targets The list of users in the party
     * @param amount  The number of plot points to add
     * @param channel The channel the command was sent in
     * @param author  The author that invoked the command
     */
    private void addPlotPointsToPartyAndSendSummary(List<CompletableFuture<User>> targets, int amount, TextChannel channel, MessageAuthor author) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        List<CompletableFuture<Void>> list = new ArrayList<>();
        for (CompletableFuture<User> userCompletableFuture : targets) {
            list.add(userCompletableFuture.thenComposeAsync(user -> CompletableFuture.runAsync(() -> modifyUserPlotPoints(amount, channel, changes, user))));
        }
        final CompletableFuture<Void> afterUpdateFuture = CompletableFuture.allOf(list.toArray(new CompletableFuture[]{}));
        afterUpdateFuture.thenAcceptAsync(unused -> channel.sendMessage(generateEmbed(changes, channel, author)));
    }

    /**
     * Modifies the plot points for a user and add the change in plot points it was successful
     *
     * @param amount  The number of plot points to add
     * @param channel The channel the message was sent in
     * @param changes The list of changes in the plot points
     * @param user    The user to modify the plot points of
     */
    private void modifyUserPlotPoints(int amount, TextChannel channel, List<Triple<User, Integer, Integer>> changes, User user) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
        final Optional<Integer> newPlotPoints = addPlotPoints(user, amount, channel);
        if (plotPoints.isPresent() && newPlotPoints.isPresent()) {
            changes.add(Triple.of(user, plotPoints.get(), newPlotPoints.get()));
        }
    }

    /**
     * Sets the plot points for a user. Sends an embed if there's an error in getting or setting plot points.
     *
     * @param target  The user to set plot points for
     * @param number  The new number of plot points
     * @param channel The channel the command was sent from
     * @return Whether the plot points were successfully modified
     */
    private boolean setPlotPoints(User target, int number, TextChannel channel) {
        Optional<Integer> oldPP = SheetsHandler.getPlotPoints(target);
        if (oldPP.isPresent()) {
            SheetsHandler.setPlotPoints(target, number).exceptionally(throwable -> {
                channel.sendMessage(new EmbedBuilder().setColor(Color.RED).setDescription("I was unable to set the plot points of " + getUsernameInChannel(target, channel)));
                return Optional.empty();
            });
            return true;
        }
        else {
            channel.sendMessage(new EmbedBuilder().setColor(Color.RED).setDescription("I was unable to find the plot points of " + getUsernameInChannel(target, channel)));
            return false;
        }
    }

    /**
     * Adds plot points to one user
     *
     * @param user    The user to add plot points to
     * @param number  The number of plot points to add
     * @param channel The channel the message was sent from
     * @return The new number of plot points the user has
     */
    private Optional<Integer> addPlotPoints(User user, int number, TextChannel channel) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
        if (plotPoints.isPresent()) {
            int oldPP = plotPoints.get();
            final int newPP = oldPP + number;
            SheetsHandler.setPlotPoints(user, newPP).exceptionally(throwable -> {
                channel.sendMessage(new EmbedBuilder().setColor(Color.RED).setDescription("I was unable to set the plot points of " + getUsernameInChannel(user, channel)));
                return Optional.empty();
            });
            return SheetsHandler.getPlotPoints(user);
        }
        return Optional.empty();
    }

    /**
     * Gets the display name of the user in a channel
     *
     * @param user    The user to check the nickname of
     * @param channel The channel to check
     * @return The display name of the user in a channel, or their name if it's not a server channel
     */
    private String getUsernameInChannel(User user, Channel channel) {
        return channel.asServerTextChannel().map(serverTextChannel -> user.getDisplayName(serverTextChannel.getServer())).orElseGet(user::getName);
    }

    /**
     * Returns an embed for changes in plot points
     *
     * @param plotPointChanges A list of triples that represent the change in plot points. The triple contains the user whose plot points are being changed, the old number of plot points, and the new number of plot points.
     * @param channel          The channel the command was sent in
     * @param author           The author that invoked the command
     * @return An embed containing the changes in a user(s)'s plot points. If there is more than 1 user, add the changes as fields. Otherwise, set the author of the embed as the user and the description as the change in plot points.
     */
    private EmbedBuilder generateEmbed(List<Triple<User, Integer, Integer>> plotPointChanges, Channel channel, MessageAuthor author) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Plot Points!").setColor(RandomColor.getRandomColor());
        if (author.asUser().isPresent()) {
            builder.setAuthor(author.asUser().get());
        }
        else {
            builder.setAuthor(author);
        }
        if (plotPointChanges.size() == 1) {
            final String description = plotPointChanges.get(0).getMiddle() + " → " + plotPointChanges.get(0).getRight();
            builder.setAuthor(plotPointChanges.get(0).getLeft()).setDescription(description);
        }
        else {
            for (Triple<User, Integer, Integer> changes : plotPointChanges) {
                builder.addField(getUsernameInChannel(changes.getLeft(), channel), changes.getMiddle() + " → " + changes.getRight());
            }
        }
        return builder;
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
        if (author.asUser().isPresent()) {
            author.asUser().ifPresent(builder::setAuthor);
        }
        else {
            builder.setAuthor(author);
        }
        for (User user : users) {
            SheetsHandler.getPlotPoints(user).ifPresent(points -> builder.addField(getUsernameInChannel(user, channel), String.valueOf(points)));
        }
        return builder;
    }

    public enum CommandType {
        ADD, SUB, SET, ADDHERE, GET, ADDALL
    }

}
