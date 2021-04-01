package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import logic.RandomColor;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import players.Party;
import players.PartyHandler;
import sheets.SheetsHandler;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointCommand implements CommandExecutor {
    //TODO Make use embeds from generateEmbed
    @Command(aliases = {"~p", "~pp", "~plot", "~plotpoints"}, description = "Modifies the plot points of a user", privateMessages = false, usage = "~p add|sub|set|addall|addhere party|usermention [number]")
    public void processCommandType(Object[] params, MessageAuthor author, TextChannel channel, Server server) {
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
            else if (arg instanceof Integer) {
                amount = (int) arg;
            }
            else if (arg instanceof User) {
                targets.add(((User) arg));
            }
            else if (PartyHandler.getParties().stream().map(Party::getName).anyMatch(s -> s.equals(arg))) {
                party = (String) arg;
            }
        }
        if (targets.isEmpty() && author.asUser().isPresent()) {
            targets.add(author.asUser().get());
        }
        executeCommand(command, targets, amount, author, party, channel);
    }

    //Execute a command based on the command type. If an invalid command is entered, send an error embed message
    private void executeCommand(CommandType commandType, List<User> targets, int number, MessageAuthor author, String party, TextChannel channel) {
        switch (commandType) {
            case ADD:
                targets.forEach(user -> channel.sendMessage(addPlotPoints(user, number, channel)));
                return;
            case SUB:
                targets.forEach(user -> channel.sendMessage(addPlotPoints(user, number * -1, channel)));
                return;
            case SET:
                targets.forEach(user -> channel.sendMessage(setPlotPoints(user, number, channel)));
                return;
            case ADDALL:
                channel.sendMessage(addPlotPointsToParty(number, party, channel));
                return;
            default:
                if (author.asUser().isPresent()) {
                    channel.sendMessage(getPlotPoints(targets.isEmpty() ? Collections.singletonList(author.asUser().get()) : targets, channel));
                }
        }
    }

    private EmbedBuilder addPlotPointsToParty(int number, String partyName, Channel channel) {
        DiscordApi api = channel.getApi();
        EmbedBuilder allPlayerEmbed = new EmbedBuilder().setDescription(MessageFormat.format("Allocating {0} plot point(s) to {1}", number, partyName));
        PartyHandler.getParties().stream()
                .filter(party -> party.getName().equals(partyName)).findFirst()
                .ifPresent(party -> party.getMembers()
                        .forEach(partyMember -> api.getUserById(partyMember.getDiscordId()).thenAccept(user -> addPlotPoints(user, number, channel))));
        return allPlayerEmbed;
    }

    private EmbedBuilder setPlotPoints(User target, int number, Channel channel) {
        Optional<Integer> oldPP = SheetsHandler.getPlotPoints(target);
        if (oldPP.isPresent()) {
            SheetsHandler.setPlotPoints(target, number);
            return generateEmbed(Collections.singletonList(Triple.of(target, oldPP.get(), number)), channel);
        }
        return new EmbedBuilder();
    }

    /**
     * Adds plot points to one user
     *
     * @param user    The user to add plot points to
     * @param number  The number of plot points to add
     * @param channel The channel the message was sent from
     * @return An embed containing the change in plot points for the user
     */
    EmbedBuilder addPlotPoints(User user, int number, Channel channel) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
        if (plotPoints.isPresent()) {
            int oldPP = plotPoints.get();
            final int newPP = oldPP + number;
            SheetsHandler.setPlotPoints(user, newPP);
            return generateEmbed(Collections.singletonList(Triple.of(user, oldPP, newPP)), channel);
        }
        final String displayName = getUsernameInChannel(user, channel);
        return new EmbedBuilder().setAuthor(user).setDescription("An error occurred when setting plot points for " + displayName);
    }

    private String getUsernameInChannel(User user, Channel channel) {
        return channel.asServerTextChannel().map(serverTextChannel -> user.getDisplayName(serverTextChannel.getServer())).orElseGet(user::getName);
    }

    private EmbedBuilder generateEmbed(List<Triple<User, Integer, Integer>> plotPointChange, Channel channel) {
        EmbedBuilder builder = new EmbedBuilder().setDescription("Plot Points!").setColor(RandomColor.getRandomColor());
        for (Triple<User, Integer, Integer> changes : plotPointChange) {
            builder.addField(getUsernameInChannel(changes.getLeft(), channel), changes.getMiddle() + " → " + changes.getRight());
        }
        return builder;
    }

    private EmbedBuilder getPlotPoints(List<User> users, Channel channel) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Plot Points!");
        for (User user : users) {
            SheetsHandler.getPlotPoints(user).ifPresent(points -> builder.addField(getUsernameInChannel(user, channel), String.valueOf(points)));
        }
        return builder;
    }

    public enum CommandType {
        ADD, SUB, SET, ADDHERE, GET, ADDALL
    }

}
