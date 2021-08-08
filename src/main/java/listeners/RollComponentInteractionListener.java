package listeners;

import com.vdurmont.emoji.EmojiParser;
import dicerolling.RollResult;
import doom.DoomHandler;
import io.vavr.control.Either;
import logic.RollLogic;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageUpdater;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.ButtonClickListener;
import org.javacord.api.util.event.ListenerManager;
import sheets.PlotPointUtils;
import util.ComponentUtils;
import util.UtilFunctions;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RollComponentInteractionListener implements ButtonClickListener {
    private final User user;
    private final RollResult result;
    private final AtomicReference<ListenerManager<ButtonClickListener>> listenerReference;
    private final Either<MessageUpdater, InteractionOriginalResponseUpdater> updaterObject;
    private ScheduledFuture<CompletableFuture<Void>> removeListenerTask;

    public RollComponentInteractionListener(User user, RollResult result, AtomicReference<ListenerManager<ButtonClickListener>> listenerReference, MessageUpdater messageUpdater) {
        this.user = user;
        this.result = result;
        this.listenerReference = listenerReference;
        this.updaterObject = Either.left(messageUpdater);
    }

    public RollComponentInteractionListener(User user, RollResult result, AtomicReference<ListenerManager<ButtonClickListener>> listenerReference, InteractionOriginalResponseUpdater updater) {
        this.user = user;
        this.result = result;
        this.listenerReference = listenerReference;
        this.updaterObject = Either.right(updater);
    }

    public void startRemoveTimer() {
        if (listenerReference.get() != null) {
            removeListenerTask = user.getApi().getThreadPool().getScheduler().schedule(this::removeHandler, 60, TimeUnit.SECONDS);
        }
    }

    /**
     * The message this component is attached to gets a component interaction, enhance the roll or reroll, and then remove the components and the listener.
     * <p>
     * If confirm is received, remove the components on the message.
     * If reroll is received, reroll the roll.
     * If a number is received, enhance the roll.
     *
     * @param event The ButtonClickEvent
     */
    @Override
    public void onButtonClick(ButtonClickEvent event) {
        final ButtonInteraction componentInteraction = event.getButtonInteraction();
        // Check for the type of component
        final String customId = componentInteraction.getCustomId();
        final Optional<Integer> enhanceCount = UtilFunctions.tryParseInt(customId);
        final Optional<Message> interactionMessage = componentInteraction.getMessage();

        if (!removeListenerTask.isDone() && removeListenerTask.getDelay(TimeUnit.MILLISECONDS) > 0) {
            removeListenerTask.cancel(true);
            if ("confirm".equals(customId)) {
                componentInteraction.createOriginalMessageUpdater().removeAllComponents().update();
            }
            else if (enhanceCount.isPresent() && interactionMessage.isPresent()) {
                enhanceRoll(componentInteraction, enhanceCount.get(), interactionMessage.get());
            }
            else if ("reroll".equals(customId) && interactionMessage.isPresent()) {
                handleReroll(componentInteraction, interactionMessage.get());
            }
            listenerReference.get().remove();
        }

    }

    /**
     * Removes the components from the message, then enhances a roll using plot points and finally attach a star emoji
     *
     * @param componentInteraction The button interaction
     * @param enhanceCount         The number of plot points the user is spending
     * @param interactionMessage   The message that contained the interaction
     */
    private void enhanceRoll(ButtonInteraction componentInteraction, Integer enhanceCount, Message interactionMessage) {
        // TODO Make this support doom points
        getEnhancementEmbed(componentInteraction.getUser(), result.getTotal(), enhanceCount).thenAccept(enhanceEmbed -> componentInteraction.createOriginalMessageUpdater()
                        .removeAllComponents()
                        .addEmbeds(RollLogic.removeFirstEmbedFooter(interactionMessage))
                        .addEmbed(enhanceEmbed)
                        .update())
                .thenAccept(unused -> interactionMessage.addReaction(EmojiParser.parseToUnicode(":star2:")));
    }

    /**
     * Gets the embed for enhancing a roll with plot points
     *
     * @param user   The user that is enhancing the roll
     * @param result The result of the roll
     * @param count  The number of plot points the user is spending
     * @return An embed that contains the results of the enhancement after the character sheet gets modified
     */
    private CompletableFuture<EmbedBuilder> getEnhancementEmbed(User user, int result, int count) {
        final EmbedBuilder enhanceEmbed = new EmbedBuilder()
                .setTitle("Enhancing a roll!")
                .addField("Enhanced Total", result + " → " + (result + count));

        return PlotPointUtils.addPlotPointsToUser(user, count * -1).thenApply(integer -> {
            integer.ifPresent(newCount -> enhanceEmbed.addField("Plot Points!", (newCount + count) + " → " + newCount));
            return enhanceEmbed;
        });
    }

    /**
     * Handles the reroll of a roll by removing the components from the message,
     *
     * @param interaction        The button interaction
     * @param interactionMessage The message the button interaction is attached to
     */
    private void handleReroll(ButtonInteraction interaction, Message interactionMessage) {
        // Handles a reroll by removing the components from the original message, then sending a new message with the new embeds and components and then attach a listener
        final Optional<RollResult> reroll = result.reroll();
        if (reroll.isPresent() && interaction.getChannel().isPresent()) {
            RollResult rerollResult = reroll.get();
            interaction.createOriginalMessageUpdater()
                    .removeAllComponents()
                    .addEmbed(interactionMessage.getEmbeds().get(0).toBuilder().setFooter(""))
                    .update()
                    .thenAccept(unused -> interactionMessage.addReaction(EmojiParser.parseToUnicode(":bulb:")));
            final TextChannel channel = interaction.getChannel().get();
            rollBackChanges().thenAccept(integer -> RollLogic.getRollEmbeds(UtilFunctions.getUsernameInChannel(interaction.getUser(), channel), interaction.getUser(), rerollResult).thenAccept(embedBuilders -> sendRerollMessageAndAttachListener(channel, interactionMessage, rerollResult, embedBuilders)));
        }
    }

    /**
     * Reverts the changes made to doom points or plot points. Used to quietly revert points before rolling again.
     *
     * @return A completable future indicating that the plot points and doom points have been rolled back
     */
    private CompletableFuture<Void> rollBackChanges() {
        CompletableFuture<Optional<Integer>> future = CompletableFuture.completedFuture(Optional.empty());
        if (result.getPlotPointsSpent() != 0 || result.getDoomGenerated() > 0) {
            int plotPointChange = result.getPlotPointsSpent();
            if (result.getDoomGenerated() > 0) {
                plotPointChange--;
            }
            future = PlotPointUtils.addPlotPointsToUser(user, plotPointChange);
        }
        if (result.getDoomGenerated() != 0) {
            DoomHandler.addDoom(result.getDoomGenerated() * -1);
        }
        return future.thenAccept(integer -> {});
    }

    /**
     * Sends the outcome of a re-roll and attach a listener for roll enhancement
     *
     * @param channel            The ButtonInteraction to send the re-roll result to
     * @param interactionMessage The message to reference when re-rolling
     * @param rerollResult       The result of the re-roll
     * @param embedBuilders      The embeds to set the originals to once the edit is done, leaves out the help text in the footer
     */
    private void sendRerollMessageAndAttachListener(TextChannel channel, Message interactionMessage, RollResult rerollResult, EmbedBuilder[] embedBuilders) {
        new MessageBuilder()
                .addEmbeds(embedBuilders)
                .addComponents(ComponentUtils.createRollComponentRows(false, rerollResult.isEnhanceable()))
                .replyTo(interactionMessage)
                .send(channel)
                .thenAccept(message -> RollLogic.attachEnhancementListener(user, rerollResult, message));
    }

    /**
     * Removes the listener after 60 seconds and removes the components
     *
     * @return A void completable future
     */
    private CompletableFuture<Void> removeHandler() {
        updaterObject.fold(messageUpdater -> messageUpdater.removeAllComponents().applyChanges(), responseUpdater -> responseUpdater.removeAllComponents().update());
        listenerReference.get().remove();
        return CompletableFuture.completedFuture(null);
    }
}