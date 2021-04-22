package commands;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import players.PartyHandler;
import sheets.PlotPointHandler;
import sheets.SheetsHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ReplenishCommand implements CommandExecutor {
    @Command(aliases = {"~replenish", "~sr", "~p addall"}, description = "Adds plot points to all party members of the specified party", privateMessages = false, usage = "~replenish <roleping> [amount]")
    public void replenishPlotPoints(DiscordApi api, TextChannel channel, Message message, MessageAuthor author, Object[] parameters) {
        final List<Role> parties = new ArrayList<>(message.getMentionedRoles());
        // Add party roles mentioned by strings to parties
        Arrays.stream(parameters)
                .filter(partyName -> partyName instanceof String)
                .forEach(partyName -> PartyHandler.getPartyByName((String) partyName).flatMap(party -> api.getRoleById(party.getRoleID())).ifPresent(parties::add));
        // If there is a number, run the replenish function using the parties and that number as the amount to replenish
        Arrays.stream(parameters)
                .filter(o -> o instanceof Long).findFirst()
                .ifPresent(pointsToReplenish -> replenishParties(parties.get(0), Math.toIntExact((long) pointsToReplenish), channel, author));
    }

    /**
     * Adds the specified number of plot points to all players with the input roles, and send an embed with the results
     * <p>
     * The embed will list players that the bot cannot successfully edit
     *
     * @param party   The party to add plot points to
     * @param points  The number of plot point to add ot each player
     * @param channel The channel the message was sent from
     * @param author  The author of the message
     */
    private void replenishParties(Role party, int points, TextChannel channel, MessageAuthor author) {
        List<Triple<User, Integer, Integer>> plotPointChanges = new ArrayList<>();
        List<User> uneditablePlayers = new ArrayList<>();
        final List<CompletableFuture<Optional<Integer>>> replenishmentFutures = party.getUsers().stream()
                .filter(user -> SheetsHandler.getPlotPoints(user).isPresent())
                .map(user -> addPlotPointsToUser(points, plotPointChanges, uneditablePlayers, user))
                .collect(Collectors.toList());
        CompletableFuture.allOf(replenishmentFutures.toArray(new CompletableFuture[]{}))
                .thenAccept(unused -> sendReplenishmentResultEmbed(channel, author, plotPointChanges, uneditablePlayers));
    }

    /**
     * Sends an embed that contains the changes in plot points
     * <p>
     * Players whose plot points cannot be modified will be listed in the embed
     *
     * @param channel           The channel to send the embed to
     * @param author            The author that sent the command
     * @param plotPointChanges  A list of triples containing changes in plot points
     * @param uneditablePlayers A list of players whose plot points could not be edited
     */
    private void sendReplenishmentResultEmbed(TextChannel channel, MessageAuthor author, List<Triple<User, Integer, Integer>> plotPointChanges, List<User> uneditablePlayers) {
        final EmbedBuilder replenishmentEmbed = PlotPointHandler.generateEmbed(plotPointChanges, channel, author).setTitle("Session Replenishment!");
        if (!uneditablePlayers.isEmpty()) {
            replenishmentEmbed.setDescription("I was unable to edit the plot points of:\n - " + uneditablePlayers.stream().map(user -> PlotPointHandler.getUsernameInChannel(user, channel)).collect(Collectors.joining("\n - ")));
        }
        channel.sendMessage(replenishmentEmbed);
    }

    /**
     * Generates a CompletableFuture that adds plot points to players, and updates the list of changes and the list of uneditable players
     *
     * @param points            The number of plot points to add to each the player
     * @param plotPointChanges  The list of changes in plot points
     * @param uneditablePlayers The list of uneditable players
     * @param user              The player to edit the plot points for
     * @return A CompletableFuture representing the completion of the addition of plot points and side effects
     */
    private CompletableFuture<Optional<Integer>> addPlotPointsToUser(int points, List<Triple<User, Integer, Integer>> plotPointChanges, List<User> uneditablePlayers, User user) {
        final Optional<Integer> oldPlotPointCount = SheetsHandler.getPlotPoints(user);
        if (oldPlotPointCount.isPresent()) {
            final int newPlotPointCount = oldPlotPointCount.get() + points;
            return SheetsHandler.setPlotPoints(user, newPlotPointCount)
                    .thenApply(integer -> {
                        integer.ifPresent(newPoints -> plotPointChanges.add(Triple.of(user, oldPlotPointCount.get(), newPlotPointCount)));
                        return integer;
                    })
                    .exceptionally(throwable -> {
                        uneditablePlayers.add(user);
                        return Optional.empty();
                    });
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

}
