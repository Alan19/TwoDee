package sheets;

import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import util.RandomColor;
import util.UtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlotPointChangeResult {
    private final List<Triple<User, Integer, Integer>> plotPointChanges;
    private final List<User> unmodifiableUsers;

    public PlotPointChangeResult() {
        this.plotPointChanges = new ArrayList<>();
        this.unmodifiableUsers = new ArrayList<>();
    }

    public PlotPointChangeResult(List<Triple<User, Integer, Integer>> plotPointChanges, List<User> unmodifiableUsers) {
        this.plotPointChanges = plotPointChanges;
        this.unmodifiableUsers = unmodifiableUsers;
    }

    public List<Triple<User, Integer, Integer>> getPlotPointChanges() {
        return plotPointChanges;
    }

    public List<User> getUnmodifiableUsers() {
        return unmodifiableUsers;
    }

    public EmbedBuilder generateEmbed(TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder().setTitle("Plot Points!").setColor(RandomColor.getRandomColor());
        for (Triple<User, Integer, Integer> changes : plotPointChanges) {
            builder.addField(UtilFunctions.getUsernameInChannel(changes.getLeft(), channel), changes.getMiddle() + " → " + changes.getRight());
        }
        return builder;
    }

    /**
     * Create an embed that contains the changes in plot points
     * <p>
     * Players whose plot points cannot be modified will be listed in the embed
     *
     * @param channel The channel to send the embed to
     */
    public EmbedBuilder getReplenishEmbed(TextChannel channel) {
        final EmbedBuilder embed = generateEmbed(channel).setTitle("Session Replenishment!");
        if (!getUnmodifiableUsers().isEmpty()) {
            embed.setDescription("I was unable to edit the plot points of:\n - " + getUnmodifiableUsers().stream().map(user -> UtilFunctions.getUsernameInChannel(user, channel)).collect(Collectors.joining("\n - ")));
        }

        return embed;
    }
}
