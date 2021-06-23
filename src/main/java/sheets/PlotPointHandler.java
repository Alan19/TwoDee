package sheets;

import util.RandomColor;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Class to manage modifications to the plot point and bleed cells
 */
public class PlotPointHandler {

    /**
     * Sets the plot points for a user. Sends an embed if there's an error in getting or setting plot points.
     *
     * @param target  The user to set plot points for
     * @param number  The new number of plot points
     * @param channel The channel the command was sent from
     * @return Whether the plot points were successfully modified
     */
    public static CompletableFuture<Optional<Integer>> setPlotPoints(User target, int number, TextChannel channel) {
        Optional<Integer> oldPP = SheetsHandler.getPlotPoints(target);
        if (oldPP.isPresent()) {
            return SheetsHandler.setPlotPoints(target, number).exceptionally(throwable -> sendErrorEmbed(channel, "I was unable to set the plot points of " + getUsernameInChannel(target, channel)));
        }
        else {
            channel.sendMessage(new EmbedBuilder().setColor(Color.RED).setDescription("I was unable to find the plot points of " + getUsernameInChannel(target, channel)));
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Helper function to send an error message to the channel and then return an empty optional since the function failed
     *
     * @param channel   The channel to send the message to
     * @param errorText The error text for the embed
     * @return An empty optional to be returned by the CompletableFuture that failed to update the plot point count
     */
    private static Optional<Integer> sendErrorEmbed(TextChannel channel, String errorText) {
        channel.sendMessage(new EmbedBuilder().setColor(Color.RED).setDescription(errorText));
        return Optional.empty();
    }

    /**
     * Gets the display name of the user in a channel
     *
     * @param user    The user to check the nickname of
     * @param channel The channel to check
     * @return The display name of the user in a channel, or their name if it's not a server channel
     */
    public static String getUsernameInChannel(User user, Channel channel) {
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
    public static EmbedBuilder generateEmbed(List<Triple<User, Integer, Integer>> plotPointChanges, Channel channel, MessageAuthor author) {
        return generateEmbed(plotPointChanges, channel, author.asUser().get());
    }

    public static EmbedBuilder generateEmbed(List<Triple<User, Integer, Integer>> plotPointChanges, Channel channel, User author) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Plot Points!").setColor(RandomColor.getRandomColor());
        builder.setAuthor(author);
        for (Triple<User, Integer, Integer> changes : plotPointChanges) {
            builder.addField(getUsernameInChannel(changes.getLeft(), channel), changes.getMiddle() + " → " + changes.getRight());
        }
        return builder;
    }

    public static CompletableFuture<Optional<Integer>> setPlotPointsAndLog(List<Triple<User, Integer, Integer>> plotPointChanges, List<User> uneditablePlayers, User user, int oldPlotPointCount, int newPlotPointCount) {
        return SheetsHandler.setPlotPoints(user, newPlotPointCount)
                .thenApply(integer -> {
                    integer.ifPresent(newPoints -> plotPointChanges.add(Triple.of(user, oldPlotPointCount, newPlotPointCount)));
                    return integer;
                })
                .exceptionally(throwable -> {
                    uneditablePlayers.add(user);
                    return Optional.empty();
                });
    }

    /**
     * Generates a CompletableFuture that adds plot points to players, and updates the list of changes and the list of uneditable players
     *
     * @param user              The player to edit the plot points for
     * @param points            The number of plot points to add to each the player
     * @param plotPointChanges  The list of changes in plot points
     * @param uneditablePlayers The list of uneditable players
     * @return A CompletableFuture representing the completion of the addition of plot points and side effects
     */
    public static CompletableFuture<Optional<Integer>> addPlotPointsToUser(User user, int points, List<Triple<User, Integer, Integer>> plotPointChanges, List<User> uneditablePlayers) {
        final Optional<Integer> oldPlotPointCount = SheetsHandler.getPlotPoints(user);
        if (oldPlotPointCount.isPresent()) {
            final int newPlotPointCount = oldPlotPointCount.get() + points;
            return setPlotPointsAndLog(plotPointChanges, uneditablePlayers, user, oldPlotPointCount.get(), newPlotPointCount);
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
