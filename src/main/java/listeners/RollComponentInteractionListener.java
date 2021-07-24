package listeners;

import com.vdurmont.emoji.EmojiParser;
import dicerolling.RollResult;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;
import sheets.PlotPointUtils;
import util.UtilFunctions;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RollComponentInteractionListener implements MessageComponentCreateListener {
    private final RollResult result;
    private final long messageId;

    public RollComponentInteractionListener(RollResult result, long messageId) {
        this.result = result;
        this.messageId = messageId;
    }

    private static CompletableFuture<EmbedBuilder> getEnhancementEmbed(User user, int result, int count) {
        return CompletableFuture.completedFuture(new EmbedBuilder()
                .setTitle("Enhancing a roll!")
                .addField("Enhanced Total", result + " → " + (result + count)))
                .thenCombine(PlotPointUtils.addPlotPointsToUser(user, count * -1), (builder, integer) -> {
                    integer.ifPresent(newCount -> builder.addField("Plot Points!", (newCount + count) + " → " + newCount));
                    return builder;
                });
    }

    /**
     * The the message this component is attached to gets a component interaction, enhance the roll or reroll, and then remove the components
     *
     * @param event The MessageComponentCreateEvent
     */
    @Override
    public void onComponentCreate(MessageComponentCreateEvent event) {
        final MessageComponentInteraction componentInteraction = event.getMessageComponentInteraction();
        if (event.getMessageComponentInteraction().getMessageId() == messageId) {
            final boolean isEnhance = UtilFunctions.tryParseInt(componentInteraction.getCustomId()).isPresent();
            if (isEnhance) {
                final Optional<Integer> enhancementCount = UtilFunctions.tryParseInt(componentInteraction.getCustomId());
                if (enhancementCount.isPresent()) {
                    componentInteraction.createOriginalMessageUpdater().removeAllComponents().update();
                    getEnhancementEmbed(componentInteraction.getUser(), result.getTotal(), enhancementCount.get()).thenAccept(builder -> componentInteraction.getChannel()
                            .flatMap(channel -> componentInteraction.getMessage())
                            .ifPresent(message -> message.reply(builder).thenAccept(enhanceEmbed -> message.addReaction(EmojiParser.parseToUnicode(":star2:")))));
                }
            }

            else if (event.getInteraction().getChannel().isPresent()) {
//                final Optional<RollResult> reroll = result.reroll();
//                componentInteraction.createOriginalMessageUpdater().removeAllComponents().update();
//                reroll.map(rollResult -> componentInteraction.getMessage().ifPresent(message -> {
//                    final CompletableFuture<EmbedBuilder[]> rollEmbeds = RollHandlers.getRollEmbeds(UtilFunctions.getUsernameInChannel(componentInteraction.getUser(), message.getChannel()), componentInteraction.getUser(), rollResult).thenAccept(embedBuilders -> Arrays.stream(embedBuilders).map(builder -> builder));
//                }));
            }
        }

    }
}
