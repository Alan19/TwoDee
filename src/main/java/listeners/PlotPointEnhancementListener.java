package listeners;

import doom.DoomHandler;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import roles.Storytellers;
import sheets.SheetsHandler;
import util.PlotPointEnhancementHelper;
import util.RandomColor;

import java.util.Map;
import java.util.Optional;

/**
 * Listener that listens for a user using plot point enhancement
 */
public class PlotPointEnhancementListener implements ReactionAddListener {

    public static final String CANCEL_EMOJI = "\uD83C\uDDFD";
    public static final String STAR_EMOJI = "\uD83C\uDF1F";

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        //Do nothing if the bot is the one who reacts
        if (!event.getUser().map(User::isYourself).orElse(false)) {
            event.getReaction().ifPresent(reaction -> {
                //Check if bot has made the react on the post already
                if (reaction.containsYou()) {
                    event.requestMessage().thenAcceptAsync(message -> {
                        if (reaction.getEmoji().equalsEmoji(CANCEL_EMOJI)) {
                            PlotPointEnhancementHelper.removeEnhancementEmojis(message);
                        }
                        else if (PlotPointEnhancementHelper.isEmojiNumberEmoji(reaction.getEmoji())) {
                            enhanceRoll(event, reaction, message);
                            //Wipe reactions and then add star emoji to show that it was enhanced with plot points
                            PlotPointEnhancementHelper.removeEnhancementEmojis(message);
                            message.addReaction(STAR_EMOJI);
                        }
                    });
                }
            });
        }
    }

    /**
     * Enhances a getResults by deducting the appropriate number of plot / doom points from the user
     *
     * @param event    The reaction event that contains the reaction being used
     * @param reaction The reaction that provides the user that added the reaction
     * @param message  The message to be enhanced
     */
    private void enhanceRoll(ReactionAddEvent event, Reaction reaction, Message message) {
        //Get user getResults value and add to that based on reaction. Then deduct plot points.
        int rollVal = Integer.parseInt(message.getEmbeds().get(0).getFields().get(6).getValue());
        Emoji emoji = reaction.getEmoji();
        Optional<Integer> toAdd = getAddAmount(emoji);
        toAdd.ifPresent(count -> {
            if (Storytellers.isMessageAuthorStoryteller(message.getAuthor())) {
                event.requestUser().thenAccept(user -> sendDoomPointEnhancementMessage(user, message.getChannel(), rollVal, count));
            }
            else {
                event.requestUser().thenAccept(user -> sendPlotPointEnhancementMessage(user, message.getChannel(), rollVal, count));
            }
        });

    }

    /**
     * Subtracts plot point from player to enhance a getResults
     *
     * @param user    The user object that reacted to the getResults
     * @param channel The channel the message was reacted to
     * @param rollVal The original getResults value
     * @param toAdd   The number of points to add to the getResults value
     */
    private void sendPlotPointEnhancementMessage(User user, TextChannel channel, int rollVal, int toAdd) {
        Optional<Integer> oldPP = SheetsHandler.getPlotPoints(user);
        if (oldPP.isPresent()) {
            final int newPP = oldPP.get() - toAdd;
            SheetsHandler.setPlotPoints(user, newPP);
            final EmbedBuilder enhanceRollEmbed = new EmbedBuilder()
                    .setAuthor(user)
                    .addField("Enhanced Total", rollVal + " → " + (rollVal + toAdd))
                    .addField("Plot Points", oldPP.get() + " → " + newPP)
                    .setColor(RandomColor.getRandomColor());
            new MessageBuilder().addEmbed(enhanceRollEmbed).send(channel);
        }
    }

    /**
     * Helper method to help DM enhance rolls with doom points and send an embed with the new value
     *
     * @param channel The channel the message was sent from
     * @param rollVal The original value of the getResults
     * @param toAdd   The amount of points to add to the getResults
     */
    private void sendDoomPointEnhancementMessage(User user, TextChannel channel, int rollVal, int toAdd) {
        int oldDoom = DoomHandler.getDoom();
        DoomHandler.addDoom(toAdd * -1);
        int newDoom = DoomHandler.getDoom();
        final EmbedBuilder doomExpenditureEmbed = new EmbedBuilder()
                .setColor(RandomColor.getRandomColor())
                .setAuthor(user)
                .addField("Enhanced Total", rollVal + " → " + (rollVal + toAdd))
                .addField("Doom Points", oldDoom + " → " + newDoom);
        new MessageBuilder().addEmbed(doomExpenditureEmbed).send(channel);
    }

    /**
     * Helper method to get the number of points to add to a getResults
     *
     * @param emoji The emoji the user reacted to
     * @return The number of points to add as an optional
     */
    private Optional<Integer> getAddAmount(Emoji emoji) {
        for (Map.Entry<Integer, String> intToEmojiEntry : PlotPointEnhancementHelper.getOneToFourEmojiMap().entrySet()) {
            if (emoji.equalsEmoji(intToEmojiEntry.getValue())) {
                return Optional.of(intToEmojiEntry).map(Map.Entry::getKey);
            }
        }
        return Optional.empty();
    }
}
