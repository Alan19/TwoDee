package listeners;

import doom.DoomWriter;
import logic.PlotPointEnhancementHelper;
import logic.RandomColor;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import sheets.PPManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

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
                            enhanceRoll(event, reaction, message);
                            //Wipe reactions and then add star emoji to show that it was enhanced with plot points
                            removeEnhancementEmojis(message);
                            message.addReaction("\uD83C\uDF1F");

                        }
                    });
                }
            });
        });
    }

    private void enhanceRoll(ReactionAddEvent event, Reaction reaction, Message message) {
        //Get user roll value and add to that based on reaction. Then deduct plot points.
        int rollVal = Integer.parseInt(message.getEmbeds().get(0).getFields().get(6).getValue());
        Emoji emoji = reaction.getEmoji();
        int toAdd = getAddAmount(emoji);
        User user = event.getUser();
        String gameMasterID = getGameMaster();
        if (user.getIdAsString().equals(gameMasterID)) {
            sendDoomPointEnhancementMessage(toAdd, rollVal, event.getChannel(), user);
        } else {
            sendPlotPointEnhancementMessage(event.getChannel(), rollVal, toAdd, user);
        }
    }

    private String getGameMaster() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("resources/bot.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props.getProperty("gameMaster");
    }

    /**
     * Subtracts plot point from player to enhance a roll
     *
     * @param channel The channel the message was reacted to
     * @param rollVal The original roll value
     * @param toAdd   The number of points to add to the roll value
     * @param user    The user object that reacted to the roll
     */
    private void sendPlotPointEnhancementMessage(TextChannel channel, int rollVal, int toAdd, User user) {
        PPManager manager = new PPManager();
        int oldPP = manager.getPlotPoints(user.getIdAsString());
        int newPP = manager.setPlotPoints(user.getIdAsString(), oldPP - toAdd);
        new MessageBuilder()
                .setEmbed(
                        new EmbedBuilder()
                                .setAuthor(user)
                                .addField("Enhanced Total", rollVal + " → " + Integer.toString(rollVal + toAdd))
                                .addField("Plot Points", oldPP + " → " + newPP)
                                .setColor(RandomColor.getRandomColor())
                )
                .send(channel);
    }

    /**
     * Helper method to help DM enhance rolls with doom points and send an embed with the new value
     *
     * @param toAdd   The amount of points to add to the roll
     * @param rollVal The original value of the roll
     * @param channel The channel the message was sent from
     */
    private void sendDoomPointEnhancementMessage(int toAdd, int rollVal, TextChannel channel, User user) {
        DoomWriter doomWriter = new DoomWriter();
        int oldDoom = doomWriter.getDoom();
        doomWriter.addDoom(toAdd * -1);
        int newDoom = doomWriter.getDoom();
        new MessageBuilder()
                .setEmbed(
                        new EmbedBuilder()
                                .setColor(RandomColor.getRandomColor())
                                .setAuthor(user)
                                .addField("Enhanced Total", rollVal + " → " + (rollVal + toAdd))
                                .addField("Doom Points", oldDoom + " → " + newDoom))
                .send(channel);
    }

    /**
     * Helper method to remove the enhancement (number) emojis from a message
     *
     * @param message The message object whose emojis will be removed
     */
    private void removeEnhancementEmojis(Message message) {
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();
        for (String emoji : helper.getOneToTwelveEmojiMap().values()) {
            message.removeReactionsByEmoji(emoji);
        }
        message.getServer().ifPresent(server -> message.removeReactionsByEmoji(server.getCustomEmojiById("525867366303793182").get(), server.getCustomEmojiById("525867383890509864").get()));
        message.removeReactionsByEmoji("\uD83C\uDDFD");
    }

    /**
     * Helper method to get the number of points to add to a roll
     *
     * @param emoji The emoji the user reacted to
     * @return The number of points to add
     */
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
