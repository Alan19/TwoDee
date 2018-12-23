package listeners;

import logic.PlotPointEnhancementHelper;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Listener that listens for a user using plot point enhancement
 */
public class PlotPointEnhancementListener implements EventListener {

    public PlotPointEnhancementListener(DiscordApi api) {
        startListening(api);
    }

    @Override
    public void startListening(DiscordApi api) {
        api.addReactionAddListener(event -> {
            //Do nothing if the bot is the one who reacts
            if (event.getUser().isYourself()) {
                return;
            }
            //Check if bot has made the react on the post already
            event.getReaction().ifPresent(reaction -> {
                if (reaction.containsYou()) {
                    PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();
                    event.requestMessage().thenAcceptAsync(message -> {
                        //Get user roll value and add to that based on reaction. Then deduct plot points.
                        int rollVal = Integer.parseInt(message.getEmbeds().get(0).getFields().get(4).getValue());
                        int toAdd = 0;
                        Emoji emoji = reaction.getEmoji();
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
                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setAuthor(event.getUser())
                                .addField("Enhancing roll...", rollVal + " → " + (rollVal + toAdd));
                        new MessageBuilder()
                                .setEmbed(embedBuilder)
                                .send(event.getChannel());
                        try {
                            message.removeAllReactions().get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });

        });
    }
}
