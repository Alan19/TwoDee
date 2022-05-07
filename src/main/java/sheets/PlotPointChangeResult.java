package sheets;

import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import util.RandomColor;
import util.UtilFunctions;

import java.util.List;
import java.util.stream.Collectors;

public record PlotPointChangeResult(List<Triple<User, Integer, Integer>> plotPointChanges,
                                    List<User> unmodifiableUsers) {

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
        if (!unmodifiableUsers().isEmpty()) {
            embed.setDescription("I was unable to edit the plot points of:\n - " + unmodifiableUsers().stream().map(User::getDiscriminatedName).collect(Collectors.joining("\n - ")));
        }

        return embed;
    }
}
