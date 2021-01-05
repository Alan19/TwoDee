package listeners;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;

public class DeleteStatsListener implements EventListener {


    private final DiscordApi api;

    public DeleteStatsListener(DiscordApi api) {
        this.api = api;
    }

    //Listen for a user reacting to the :x: react that the bot reacts with on certain posts. If a user uses the reaction, delete the post.
    @Override
    public void startListening() {
        api.addReactionAddListener(event -> {
            if (!event.getUser().map(User::isYourself).orElse(false)) {
                event.requestMessage().thenAcceptAsync(message -> event.getReaction().ifPresent(reaction -> {
                    if (reaction.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":x:")) && reaction.containsYou()) {
                        message.delete();
                    }
                }));
            }
        });
    }
}
