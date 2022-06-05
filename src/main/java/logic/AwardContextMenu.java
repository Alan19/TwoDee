package logic;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.UserContextMenuCommandEvent;
import org.javacord.api.interaction.UserContextMenuBuilder;
import org.javacord.api.listener.interaction.UserContextMenuCommandListener;
import util.DiscordHelper;
import util.RandomColor;
import util.UtilFunctions;

public class AwardContextMenu implements UserContextMenuCommandListener {

    public static void setupContextMenu(DiscordApi api) {
        new UserContextMenuBuilder().setName("Award Plot Point").createGlobal(api).thenAccept(userContextMenu -> System.out.println("Global context menu registered!"));
    }

    @Override
    public void onUserContextMenuCommand(UserContextMenuCommandEvent event) {
        event.getUserContextMenuInteraction().respondLater().thenAccept(updater -> PlotPointLogic.addPointsAndGetEmbed(event.getUserContextMenuInteraction().getTarget(), 1, event.getUserContextMenuInteraction().getChannel().orElseThrow(IllegalArgumentException::new))
                .thenApply(builder -> getEmbedBuilder(event, builder))
                .thenAccept(embedBuilder -> updater.addEmbed(embedBuilder).update()));
    }

    private EmbedBuilder getEmbedBuilder(UserContextMenuCommandEvent event, EmbedBuilder builder) {
        return builder.setTitle("Here's a reward for good play!")
                .setColor(RandomColor.getRandomColor())
                .setFooter("Requested by " + UtilFunctions.getUsernameInChannel(event.getUserContextMenuInteraction().getUser(), event.getUserContextMenuInteraction().getChannel().orElseThrow(IllegalArgumentException::new)), DiscordHelper.getLocalAvatar(event, event.getUserContextMenuInteraction().getUser()))
                .setThumbnail(DiscordHelper.getLocalAvatar(event, event.getUserContextMenuInteraction().getUser()));
    }

}
