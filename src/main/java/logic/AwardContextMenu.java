package logic;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.UserContextMenuCommandEvent;
import org.javacord.api.interaction.UserContextMenuBuilder;
import org.javacord.api.listener.interaction.UserContextMenuCommandListener;
import util.DiscordHelper;
import util.RandomColor;
import util.UtilFunctions;

/**
 * Class that contains functionality to add a context menu for giving a user a plot point, and a listener to listen to it being invoked and responding to it
 */
public class AwardContextMenu implements UserContextMenuCommandListener {

    public static final String COMMAND_NAME = "Award Plot Point";

    public static void setupContextMenu(DiscordApi api) {
        new UserContextMenuBuilder().setName(COMMAND_NAME).createGlobal(api);
    }

    @Override
    public void onUserContextMenuCommand(UserContextMenuCommandEvent event) {
        if (event.getUserContextMenuInteraction().getCommandName().equals(COMMAND_NAME)) {
            event.getUserContextMenuInteraction().respondLater().thenAccept(updater -> PlotPointLogic.addPointsAndGetEmbed(event.getUserContextMenuInteraction().getTarget(), 1, event.getUserContextMenuInteraction().getChannel().orElseThrow(IllegalArgumentException::new))
                    .thenApply(builder -> createAwardEmbed(event, builder))
                    .thenAccept(embedBuilder -> updater.addEmbed(embedBuilder).update()));
        }
    }

    /**
     * Adds a random color, the thumbnail of the target, and the footer containing the user to the embed for giving a player a plot point
     *
     * @param event   The event object
     * @param builder The plot point addition embed
     * @return A plot point change embed containing a random color, the user, and the target
     */
    private EmbedBuilder createAwardEmbed(UserContextMenuCommandEvent event, EmbedBuilder builder) {
        final String usernameInChannel = UtilFunctions.getUsernameInChannel(event.getUserContextMenuInteraction().getUser(), event.getUserContextMenuInteraction().getChannel().orElseThrow(IllegalArgumentException::new));
        final Icon userAvatar = DiscordHelper.getLocalAvatar(event, event.getUserContextMenuInteraction().getUser());
        final Icon targetAvatar = DiscordHelper.getLocalAvatar(event, event.getUserContextMenuInteraction().getTarget());
        return builder.setTitle("Here's a reward for good play!")
                .setColor(RandomColor.getRandomColor())
                .setFooter("Requested by " + usernameInChannel, userAvatar)
                .setThumbnail(targetAvatar);
    }

}
