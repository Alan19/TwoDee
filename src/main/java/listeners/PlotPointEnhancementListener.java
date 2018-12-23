package listeners;

import logic.PlotPointEnhancementHelper;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import sheets.PPManager;

import java.util.Map;

/**
 * Listener that listens for a user using plot point enhancement
 */
public class PlotPointEnhancementListener implements EventListener {

    private DiscordApi api;

    public PlotPointEnhancementListener(DiscordApi api) {
        this.api = api;
    }

    @Override
    public void startListening() {
        api.addReactionAddListener(event -> {
            //Do nothing if the bot is the one who reacts
            if (event.getUser().isYourself()) {
                return;
            }
            //Check if bot has made the react on the post already
            event.getReaction().ifPresent(reaction -> {
                if (reaction.containsYou()) {
                    event.requestMessage().thenAcceptAsync(message -> {
                        if (reaction.getEmoji().equalsEmoji("\uD83C\uDDFD")) {
                            removeEnhancementEmojis(message);
                        } else {
                            //Get user roll value and add to that based on reaction. Then deduct plot points.
                            int rollVal = Integer.parseInt(message.getEmbeds().get(0).getFields().get(4).getValue());
                            Emoji emoji = reaction.getEmoji();
                            int toAdd = getAddAmount(emoji);
                            PPManager manager = new PPManager();
                            User user = event.getUser();
                            int oldPP = manager.getPlotPoints(user.getIdAsString());
                            int newPP = manager.setPlotPoints(user.getIdAsString(), oldPP - toAdd);
                            EmbedBuilder embedBuilder = message.getEmbeds()
                                    .get(0)
                                    .toBuilder()
                                    .addInlineField("Enhancing roll...", rollVal + " → " + (rollVal + toAdd))
                                    .addInlineField("Plot points", oldPP + " → " + newPP);
                            message.edit(embedBuilder);
                            //Wipe reactions and then add star emoji to show that it was enhanced with plot points
                            removeEnhancementEmojis(message);
                            message.addReaction("\uD83C\uDF1F");
                        }
                    });
                }
            });

        });
    }

    private void removeEnhancementEmojis(Message message) {
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();
        for (String emoji : helper.getOneToTwelveEmojiMap().values()) {
            message.removeReactionsByEmoji(emoji);
        }
        message.getServer().ifPresent(server -> message.removeReactionsByEmoji(server.getCustomEmojiById("525867366303793182").get(), server.getCustomEmojiById("525867383890509864").get()));
        message.removeReactionsByEmoji("\uD83C\uDDFD");
    }

    private int getAddAmount(Emoji emoji) {
        int toAdd = 0;
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();
        if (emoji.isCustomEmoji()) {
            String tag = emoji.asKnownCustomEmoji().get().getMentionTag();
            String trimmedEmoji = tag.substring(2, tag.length() - 1);
            for (Map.Entry<Integer, String> emojiEntry : helper.getOneToTwelveEmojiMap().entrySet()) {
                if (emojiEntry.getValue().equals(trimmedEmoji)) {
                    toAdd = emojiEntry.getKey();
                }
            }
        } else {
            String unicodeEmoji = emoji.asUnicodeEmoji().get();
            for (Map.Entry<Integer, String> emojiEntry :
                    helper.getOneToTwelveEmojiMap().entrySet()) {
                if (emojiEntry.getValue().equals(unicodeEmoji)) {
                    toAdd = emojiEntry.getKey();
                }
            }
        }
        return toAdd;
    }
}
