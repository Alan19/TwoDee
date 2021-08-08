package logic;

import dicerolling.DicePoolBuilder;
import dicerolling.RollResult;
import doom.DoomHandler;
import listeners.RollComponentInteractionListener;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.MessageUpdater;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.ButtonClickListener;
import org.javacord.api.util.event.ListenerManager;
import pw.mihou.velen.interfaces.*;
import roles.Storytellers;
import sheets.PlotPointUtils;
import util.ComponentUtils;
import util.RandomColor;
import util.UtilFunctions;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
 */
public class RollLogic implements VelenSlashEvent, VelenEvent {
    public static void setupRollCommand(Velen velen) {
        RollLogic rollLogic = new RollLogic();
        final List<SlashCommandOption> rollCommandOptions = getRollCommandOptions();
        rollCommandOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "opportunity", "Allows for opportunities on the roll. Defaults to true.", false));
        VelenCommand.ofHybrid("roll", "Rolls some dice!", velen, rollLogic, rollLogic).addOptions(rollCommandOptions.toArray(new SlashCommandOption[0])).addShortcuts("r").setServerOnly(true, 468046159781429250L).attach();
    }

    static List<SlashCommandOption> getRollCommandOptions() {
        // TODO Add target difficulty option
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "dicepool", "The dice pool to roll with.", true));
        options.add(SlashCommandOption.create(SlashCommandOptionType.INTEGER, "discount", "The number of plot points to discount (negative results in a plot point cost increase).", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.INTEGER, "dicekept", "The number of dice kept. Keeps two dice by default.", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "enhanceable", "Allows the roll to be enhanced after the roll.", false));
        return options;
    }

    /**
     * Creates a completable future of the embeds that have to be sent. Updates plot points and doom points if they were generated.
     *
     * @param username   The name of the user in the channel the embeds will be sent in
     * @param user       The user, used for getting the user's icon
     * @param rollResult The result of the roll
     * @return A completable future with the embeds generated by the roll
     */
    public static CompletableFuture<EmbedBuilder[]> getRollEmbeds(String username, User user, RollResult rollResult) {
        CompletableFuture<List<EmbedBuilder>> rollResultEmbeds = CompletableFuture.completedFuture(new ArrayList<>());
        rollResultEmbeds = UtilFunctions.appendElementToCompletableFutureList(rollResultEmbeds, rollResult.getResultEmbed().setAuthor(username, "", user.getAvatar()).setColor(RandomColor.getRandomColor()));

        // Add plot point expenditure embeds
        final int plotPointsSpent = rollResult.getPlotPointsSpent();
        if (plotPointsSpent != 0) {
            if (Storytellers.isUserStoryteller(user)) {
                final EmbedBuilder embedBuilder = DoomHandler.addDoom(plotPointsSpent * -1).setTitle("Using " + plotPointsSpent + " doom points!");
                rollResultEmbeds = UtilFunctions.appendElementToCompletableFutureList(rollResultEmbeds, embedBuilder);
            }
            else {
                final Optional<Integer> plotPointModifyFuture = PlotPointUtils.addPlotPointsToUser(user, plotPointsSpent * -1).join();
                final Optional<EmbedBuilder> plotPointSpentEmbed = plotPointModifyFuture.map(integer -> new EmbedBuilder()
                        .setTitle("Using " + plotPointsSpent + " plot points!")
                        .setColor(RandomColor.getRandomColor())
                        .addField(username, (integer + plotPointsSpent) + " → " + integer));
                if (plotPointSpentEmbed.isPresent()) {
                    rollResultEmbeds = UtilFunctions.appendElementToCompletableFutureList(rollResultEmbeds, plotPointSpentEmbed.get());
                }
            }
        }
        // Add doom embeds
        final int doomGenerated = rollResult.getDoomGenerated();
        if (doomGenerated > 0 && !Storytellers.isUserStoryteller(user)) {
            DoomHandler.addDoom(doomGenerated);
            final CompletableFuture<Optional<EmbedBuilder>> opportunityFuture = PlotPointUtils.addPlotPointsToUser(user, 1)
                    .thenApply(newPlotPoints -> newPlotPoints.map(integer -> new EmbedBuilder()
                            .setTitle("An opportunity!")
                            .setColor(Color.DARK_GRAY)
                            .addField(username, (integer - 1) + " → " + integer)
                            .addField(DoomHandler.getActivePool(), (DoomHandler.getDoom() - doomGenerated) + " → " + DoomHandler.getDoom())));
            rollResultEmbeds = UtilFunctions.appendOptionalToCompletableFutureList(rollResultEmbeds, opportunityFuture);
        }

        return rollResultEmbeds.thenApply(embedBuilders -> embedBuilders.toArray(new EmbedBuilder[0]));
    }

    /**
     * Handles a roll made through a slash command
     *
     * @param event       The event that contains the user, channel, and responder objects
     * @param dicePool    The dicepool the user is rolling
     * @param discount    The plot point discount on the roll
     * @param diceKept    The number of dice kept
     * @param enhanceable If there is an enhancement override on the roll
     * @param opportunity If opportunities are enabled
     */
    public static void handleSlashCommandRoll(SlashCommandInteraction event, String dicePool, Integer discount, Integer diceKept, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Boolean> enhanceable, Boolean opportunity) {
        // Attempt to roll dice with a valid dice pool. If the dice pool is valid, generate the result embeds and add components
        final User user = event.getUser();
        final CompletableFuture<InteractionOriginalResponseUpdater> respondLater = event.respondLater();
        final Optional<RollResult> resultOptional = new DicePoolBuilder(user, dicePool)
                .withDiceKept(diceKept)
                .withDiscount(discount)
                .withEnhanceable(enhanceable)
                .withOpportunity(opportunity)
                .getResults();

        if (resultOptional.isPresent()) {
            final RollResult rollInfo = resultOptional.get();
            final CompletableFuture<EmbedBuilder[]> rollEmbeds = getRollEmbeds(UtilFunctions.getUsernameFromSlashEvent(event, user), user, rollInfo);
            respondLater.thenAcceptBoth(rollEmbeds, (updater, embedBuilders) -> handleRollSideEffects(user, updater, embedBuilders, rollInfo));
        }
        else {
            respondLater.thenAccept(updater -> updater.setContent("Invalid dice pool!").setFlags(MessageFlag.EPHEMERAL).update());
        }
    }

    /**
     * Attach components to a roll result message and adds button click handlers to handle rerolls or enhancements
     *
     * @param updater The object for updating the message
     * @param embeds  The roll result embeds
     * @param result  The roll result object
     */
    public static void handleRollSideEffects(User user, InteractionOriginalResponseUpdater updater, EmbedBuilder[] embeds, RollResult result) {
        updater.addEmbeds(embeds)
                .addComponents(ComponentUtils.createRollComponentRows(true, result.isEnhanceable()))
                .update()
                .thenAccept(message -> attachInteractionEnhancementListener(user, result, message, updater));
    }

    public static void attachEnhancementListener(User user, RollResult result, Message message) {
        final AtomicReference<ListenerManager<ButtonClickListener>> reference = new AtomicReference<>();
        final RollComponentInteractionListener listener = new RollComponentInteractionListener(user, result, reference, new MessageUpdater(message).setEmbeds(message.getEmbeds().stream().map(Embed::toBuilder).toArray(EmbedBuilder[]::new)));
        final ListenerManager<ButtonClickListener> listenerManager = message.addButtonClickListener(listener);
        reference.set(listenerManager);
        listener.startRemoveTimer();
    }

    public static void attachInteractionEnhancementListener(User user, RollResult result, Message message, InteractionOriginalResponseUpdater updater) {
        final AtomicReference<ListenerManager<ButtonClickListener>> reference = new AtomicReference<>();
        final RollComponentInteractionListener listener = new RollComponentInteractionListener(user, result, reference, updater);
        final ListenerManager<ButtonClickListener> listenerManager = message.addButtonClickListener(listener);
        reference.set(listenerManager);
        listener.startRemoveTimer();
    }

    /**
     * Handles a roll made through a text command
     *
     * @param event   The event that contains the channel the message was sent from
     * @param user    The user that made the roll
     * @param builder The dice pool to roll with
     */
    public static void handleTextCommandRoll(MessageCreateEvent event, User user, DicePoolBuilder builder) {
        if (builder.getResults().isPresent()) {
            RollResult rollResult = builder.getResults().get();
            final CompletableFuture<EmbedBuilder[]> rollEmbeds = getRollEmbeds(UtilFunctions.getUsernameInChannel(user, event.getChannel()), user, rollResult);
            rollEmbeds.thenAccept(embeds -> new MessageBuilder()
                    .addEmbeds(embeds)
                    .addComponents(ComponentUtils.createRollComponentRows(true, rollResult.isEnhanceable())).send(event.getChannel())
                    .thenAccept(message -> attachEnhancementListener(user, rollResult, message)));
        }
        else {
            event.getChannel().sendMessage("Invalid dice pool!");
        }
    }

    /**
     * Removes the footer of the first embed in a message. Used for removing the 60 second warning from a roll embed.
     *
     * @param interactionMessage The message to remove the footer for
     * @return The list of embeds in that message with the first embed not having a footer
     */
    public static EmbedBuilder[] removeFirstEmbedFooter(Message interactionMessage) {
        EmbedBuilder[] afterEnhanceEmbed = interactionMessage.getEmbeds().stream().map(Embed::toBuilder).toArray(EmbedBuilder[]::new);
        if (afterEnhanceEmbed.length > 0) {
            afterEnhanceEmbed[0] = afterEnhanceEmbed[0].setFooter("");
        }
        return afterEnhanceEmbed;
    }


    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        String dicePool = String.join(" ", args);

        //Variables containing getResults information
        DicePoolBuilder builder = new DicePoolBuilder(user, dicePool).withOpportunity(true);
        handleTextCommandRoll(event, user, builder);
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Boolean opportunity = event.getOptionBooleanValueByName("opportunity").orElse(true);
        final Integer discount = event.getOptionIntValueByName("discount").orElse(0);
        final String dicePool = event.getOptionStringValueByName("dicepool").orElse("");
        final Integer diceKept = event.getOptionIntValueByName("dicekept").orElse(2);
        final Optional<Boolean> enhanceable = event.getOptionBooleanValueByName("enhanceable");

        handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, opportunity);
    }

}