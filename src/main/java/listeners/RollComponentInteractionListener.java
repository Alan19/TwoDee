package listeners;

import com.vdurmont.emoji.EmojiParser;
import doom.DoomHandler;
import io.vavr.control.Either;
import logic.RollLogic;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageUpdater;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.embed.EmbedField;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.interaction.ButtonInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.ButtonClickListener;
import org.javacord.api.util.event.ListenerManager;
import roles.Player;
import roles.PlayerHandler;
import roles.Storytellers;
import rolling.RollResult;
import rolling.Roller;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;
import util.ComponentUtils;
import util.UtilFunctions;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RollComponentInteractionListener implements ButtonClickListener {
    private final User user;
    private final AtomicReference<ListenerManager<ButtonClickListener>> listenerReference;
    private final Either<MessageUpdater, InteractionOriginalResponseUpdater> updaterObject;
    private final int discount;
    private final boolean enhanceable;
    private final boolean opportunity;
    private final int diceKept;
    private final String pool;
    private final Integer originalPlotPoints;
    private final Integer originalDoomPoints;
    private ScheduledFuture<CompletableFuture<Void>> removeListenerTask;

    public RollComponentInteractionListener(User user, RollResult result, AtomicReference<ListenerManager<ButtonClickListener>> listenerReference, MessageUpdater messageUpdater) {
        this.user = user;
        this.result = result;
        this.listenerReference = listenerReference;
        this.updaterObject = Either.left(messageUpdater);
    }

    public RollComponentInteractionListener(User user, String pool, AtomicReference<ListenerManager<ButtonClickListener>> reference, InteractionOriginalResponseUpdater updater, int discount, boolean enhanceable, boolean opportunity, int diceKept, Pair<Integer, Integer> originalPointPair) {
        this.user = user;
        this.pool = pool;
        this.listenerReference = reference;
        this.updaterObject = Either.right(updater);
        this.discount = discount;
        this.enhanceable = enhanceable;
        this.opportunity = opportunity;
        this.diceKept = diceKept;
        this.originalPlotPoints = originalPointPair.getLeft();
        this.originalDoomPoints = originalPointPair.getRight();
    }

    public void startRemoveTimer() {
        if (listenerReference.get() != null) {
            removeListenerTask = user.getApi().getThreadPool().getScheduler().schedule(this::removeHandler, 60, TimeUnit.SECONDS);
        }
    }

    /**
     * The message this component is attached to gets a component interaction, enhance the roll or reroll, and then remove the components and the listener.
     * <p>
     * If accept is received, remove the components on the message.
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
        final Message interactionMessage = componentInteraction.getMessage();

        if (!removeListenerTask.isDone() && removeListenerTask.getDelay(TimeUnit.MILLISECONDS) > 0) {
            removeListenerTask.cancel(true);
            if ("accept".equals(customId)) {
                componentInteraction.createOriginalMessageUpdater().removeAllComponents().update();
                interactionMessage.addReaction(EmojiParser.parseToUnicode(":heavy_check_mark:"));
            }
            else if (enhanceCount.isPresent()) {
                enhanceRoll(componentInteraction, enhanceCount.get(), interactionMessage);
            }
            else if ("reroll".equals(customId)) {
                handleReroll(componentInteraction, interactionMessage);
            }
            listenerReference.get().remove();
        }

    }

    /**
     * Removes the components from the message, then enhances a roll using plot points or doom points.
     * <p>
     * After the roll is enhanced, react to the message with a star emoji.
     *
     * @param componentInteraction The button interaction
     * @param enhanceCount         The number of plot points the user is spending
     * @param interactionMessage   The message that contained the interaction
     */
    private void enhanceRoll(ButtonInteraction componentInteraction, Integer enhanceCount, Message interactionMessage) {
        final Optional<Integer> total = interactionMessage.getEmbeds()
                .get(0)
                .getFields()
                .stream()
                .filter(embedField -> embedField.getName().equals("Total"))
                .findFirst()
                .map(EmbedField::getValue)
                .flatMap(UtilFunctions::tryParseInt);
        if (total.isPresent()) {
            if (Storytellers.isUserStoryteller(componentInteraction.getUser())) {
                componentInteraction.createOriginalMessageUpdater()
                        .removeAllComponents()
                        .addEmbeds(RollLogic.removeFirstEmbedFooter(interactionMessage))
                        .addEmbeds(getDoomEnhancementEmbed(total.get(), enhanceCount))
                        .update()
                        .thenAccept(unused -> interactionMessage.addReaction(EmojiParser.parseToUnicode(":imp:")));
            }
            else {
                getEnhancementEmbed(componentInteraction.getUser(), total.get(), enhanceCount).thenAccept(enhanceEmbed -> componentInteraction.createOriginalMessageUpdater()
                                .removeAllComponents()
                                .addEmbeds(RollLogic.removeFirstEmbedFooter(interactionMessage))
                                .addEmbed(enhanceEmbed)
                                .update())
                        .thenAccept(unused -> interactionMessage.addReaction(EmojiParser.parseToUnicode(":star2:")));
            }
        }
    }

    /**
     * Enhances the roll using the current doom pool
     *
     * @param result The numerical result of the roll
     * @param count  The number of doom points to enhance the roll with
     * @return An embed containing the enhanced result, and the change in doom points
     */
    private EmbedBuilder getDoomEnhancementEmbed(int result, int count) {
        final EmbedBuilder enhanceEmbed = new EmbedBuilder()
                .setTitle("Enhancing a roll!")
                .addField("Enhanced Total", result + " → " + (result + count));
        DoomHandler.addDoom(count * -1);
        return enhanceEmbed.addField(DoomHandler.getActivePool(), DoomHandler.getDoom(DoomHandler.getActivePool()) + count + " → " + DoomHandler.getDoom(DoomHandler.getActivePool()));

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
     * Handles a re-roll. First rolls back doom and plot points, then rolls and changes doom and plot points, and finally removes embeds from the original message, removes the footer, adds a reaction, and replies to the original roll and sends the new roll.
     *
     * @param interaction        The button interaction
     * @param interactionMessage The message the button interaction is attached to
     */
    private void handleReroll(ButtonInteraction interaction, Message interactionMessage) {
        rollBackChanges().thenCompose(unused -> RollLogic.rollDice(pool, discount, diceKept, opportunity, user))
                .thenCompose(pair -> interaction.createOriginalMessageUpdater()
                        .removeAllComponents()
                        .addEmbeds(interactionMessage.getEmbeds().get(0).toBuilder().setFooter(""))
                        .update()
                        .thenAccept(unused -> interactionMessage.addReaction(EmojiParser.parseToUnicode(":bulb:")))
                        .thenApply(unused -> pair))
                .thenAccept(pair -> new MessageBuilder()
                        .addEmbeds(pair.getLeft())
                        .addComponents(ComponentUtils.createRollComponentRows(false, enhanceable))
                        .replyTo(interactionMessage)
                        .send(interactionMessage.getChannel())
                        .thenAccept(message -> Roller.attachListener(user, , Pair.of(originalPlotPoints, originalDoomPoints), pool, opportunity, discount, diceKept, )));
    }
    }

    /**
     * Reverts the changes made to doom points or plot points. Used to quietly revert points before rolling again.
     *
     * @return A completable future indicating that the plot points and doom points have been rolled back
     */
    private CompletableFuture<Void> rollBackChanges() {
        PlayerHandler.getPlayerFromUser(user).map(Player::getDoomPool).ifPresent(s -> DoomHandler.setDoom(s, originalDoomPoints));
        return SheetsHandler.setPlotPoints(user, originalPlotPoints).thenAccept(integer -> {});
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
        updaterObject.fold(messageUpdater -> messageUpdater.removeAllComponents().applyChanges(), responseUpdater -> responseUpdater.removeAllComponents().update()).thenAccept(message -> message.addReaction(EmojiParser.parseToUnicode(":heavy_check_mark:")));
        listenerReference.get().remove();
        return CompletableFuture.completedFuture(null);
    }
}
