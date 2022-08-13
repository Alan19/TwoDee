package listeners;

import configs.Settings;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.listener.interaction.ButtonClickListener;
import util.DiscordHelper;
import util.RandomColor;

public class QuoteButtonListener implements ButtonClickListener {
    private final String quote;

    public QuoteButtonListener(String quote) {
        this.quote = quote;
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        if (event.getButtonInteraction().getCustomId().equals("quote-undo")) {
            Settings.removeQuote(quote);
            event.getButtonInteraction().createOriginalMessageUpdater().removeAllComponents().update();
            final EmbedBuilder removalEmbed = new EmbedBuilder()
                    .setTitle("Removed the following quote:")
                    .setDescription(quote)
                    .setColor(RandomColor.getRandomColor())
                    // TODO Attach an avatar to the footer
                    .setFooter("Requested by " + DiscordHelper.getUsernameFromInteraction(event.getButtonInteraction(), event.getButtonInteraction().getUser()), DiscordHelper.getLocalAvatar(event.getInteraction(), event.getInteraction().getUser()));
            event.getButtonInteraction().createFollowupMessageBuilder().addEmbed(removalEmbed).send();
        }
        else if (event.getButtonInteraction().getCustomId().equals("quote-confirm")) {
            event.getButtonInteraction().createOriginalMessageUpdater().removeAllComponents().update();
        }

    }
}
