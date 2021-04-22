package sheets;

import logic.RandomColor;
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
     * Adds plot points to one user
     *
     * @param user    The user to add plot points to
     * @param number  The number of plot points to add
     * @param channel The channel the message was sent from
     * @return The new number of plot points the user has
     */
    public static Optional<Integer> addPlotPoints(User user, int number, TextChannel channel) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
        if (plotPoints.isPresent()) {
            int oldPP = plotPoints.get();
            final int newPP = oldPP + number;
            setPlotPoints(user, newPP, channel);
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
        EmbedBuilder builder = new EmbedBuilder().setTitle("Plot Points!").setColor(RandomColor.getRandomColor());
        builder.setAuthor(author);
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
}
