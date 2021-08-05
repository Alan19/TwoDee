package listeners;

import com.vdurmont.emoji.EmojiParser;
import dicerolling.RollResult;
import doom.DoomHandler;
import logic.RollLogic;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageUpdater;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.listener.interaction.ButtonClickListener;
import sheets.PlotPointUtils;
import util.ComponentUtils;
import util.UtilFunctions;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RollComponentInteractionListener implements ButtonClickListener {
    private final User user;
    private final RollResult result;
    private final EmbedBuilder[] afterEnhancementEmbed;


    public RollComponentInteractionListener(User user, RollResult result, EmbedBuilder[] afterEnhancementEmbed) {
        this.user = user;
        this.result = result;
        this.afterEnhancementEmbed = afterEnhancementEmbed;
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
     * The message this component is attached to gets a component interaction, enhance the roll or reroll, and then remove the components
     *
     * @param event The ButtonClickEvent
     */
    @Override
    public void onButtonClick(ButtonClickEvent event) {
        final ButtonInteraction componentInteraction = event.getButtonInteraction();
        // Check for the type of component
        final String customId = componentInteraction.getCustomId();
        final Optional<Integer> enhanceCount = UtilFunctions.tryParseInt(customId);
        if ("confirm".equals(customId)) {
            componentInteraction.createOriginalMessageUpdater().removeAllComponents().update();
        }
        else {
            final Optional<Message> interactionMessage = componentInteraction.getMessage();
            if ("reroll".equals(customId)) {
                // Handles a reroll by removing the components from the original message, then sending a new message with the new embeds and components and then attach a listener
                final Optional<RollResult> reroll = result.reroll();
                if (reroll.isPresent() && componentInteraction.getChannel().isPresent() && interactionMessage.isPresent()) {
                    RollResult rerollResult = reroll.get();
                    componentInteraction.createOriginalMessageUpdater()
                            .removeAllComponents()
                            .removeAllEmbeds()
                            .addEmbed(afterEnhancementEmbed[0])
                            .update()
                            .thenAccept(unused -> interactionMessage.ifPresent(message -> message.addReaction(EmojiParser.parseToUnicode(":bulb:"))));
                    CompletableFuture<Optional<Integer>> optionalCompletableFuture = rollBackChanges();
                    optionalCompletableFuture.thenAccept(integer -> RollLogic.getRollEmbeds(UtilFunctions.getUsernameInChannel(componentInteraction.getUser(), componentInteraction.getChannel().get()), componentInteraction.getUser(), rerollResult).thenAccept(embedBuilders -> sendReroll(componentInteraction, interactionMessage.get(), rerollResult, embedBuilders)));
                }
            }
            else if (enhanceCount.isPresent()) {
                final int count = enhanceCount.get();
                getEnhancementEmbed(componentInteraction.getUser(), result.getTotal(), count)
                        .thenAccept(enhanceEmbed -> componentInteraction.createOriginalMessageUpdater()
                                .removeAllComponents()
                                .removeAllEmbeds()
                                .addEmbeds(afterEnhancementEmbed)
                                .addEmbed(enhanceEmbed)
                                .update())
                        .thenAccept(unused -> interactionMessage.ifPresent(message -> message.addReaction(EmojiParser.parseToUnicode(":star2:"))));
            }
        }
    }

    private CompletableFuture<Optional<Integer>> rollBackChanges() {
        CompletableFuture<Optional<Integer>> optionalCompletableFuture = CompletableFuture.completedFuture(Optional.empty());
        if (result.getPlotPointsSpent() != 0 || result.getDoomGenerated() > 0) {
            int plotPointChange = result.getPlotPointsSpent();
            if (result.getDoomGenerated() > 0) {
                plotPointChange--;
            }
            optionalCompletableFuture = PlotPointUtils.addPlotPointsToUser(user, plotPointChange);
        }
        if (result.getDoomGenerated() != 0) {
            DoomHandler.addDoom(result.getDoomGenerated() * -1);
        }
        return optionalCompletableFuture;
    }

    private void sendReroll(ButtonInteraction componentInteraction, Message interationMessage, RollResult rerollResult, EmbedBuilder[] embedBuilders) {
        final EmbedBuilder[] afterEnhanceEmbed = embedBuilders.clone();
        afterEnhanceEmbed[0] = embedBuilders[0].setFooter("");
        new MessageBuilder()
                .addEmbeds(embedBuilders)
                .addComponents(ComponentUtils.createRollComponentRows(false, rerollResult.isEnhanceable()))
                .replyTo(interationMessage)
                .send(componentInteraction.getChannel().get())
                .thenAccept(message -> message.addButtonClickListener(new RollComponentInteractionListener(user, rerollResult, afterEnhancementEmbed)).removeAfter(60, TimeUnit.SECONDS).addRemoveHandler(() -> new MessageUpdater(message).removeAllComponents().setEmbeds(afterEnhanceEmbed).applyChanges()));
    }
}
