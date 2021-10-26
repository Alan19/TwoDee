package logic;

import doom.DoomHandler;
import io.vavr.API;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.*;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import pw.mihou.velen.interfaces.*;
import roles.Player;
import roles.PlayerHandler;
import rolling.Result;
import rolling.RollParameters;
import rolling.Roller;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.vavr.API.$;

/**
 * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
 */
public class RollLogic implements VelenSlashEvent, VelenEvent {

    public static void setupRollCommand(Velen velen) {
        RollLogic rollLogic = new RollLogic();
        final List<SlashCommandOption> rollCommandOptions = getRollCommandOptions();
        rollCommandOptions.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "opportunity", "Allows for opportunities on the roll. Defaults to true.", false));
        VelenCommand.ofHybrid("newroll", "Rolls some dice!", velen, rollLogic, rollLogic).addOptions(rollCommandOptions.toArray(new SlashCommandOption[0])).addShortcuts("r").setServerOnly(true, 468046159781429250L).attach();
    }

    static List<SlashCommandOption> getRollCommandOptions() {
        // TODO Add target difficulty option
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.STRING, "dicepool", "The dice pool to roll with.", true));
        options.add(SlashCommandOption.create(SlashCommandOptionType.INTEGER, "discount", "The number of plot points to discount (negative results in a plot point cost increase).", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.INTEGER, "dicekept", "The number of dice kept. Keeps two dice by default.", false));
        options.add(SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "enhanceable", "Allows the roll to be enhanced after the roll.", false));
        options.add(SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "target", "Adds an embed to provide assistance with hitting the target difficulty", false, Arrays.stream(new String[]{"easy", "average", "hard", "formidable", "heroic", "incredible", "ridiculous", "impossible"}).map(s -> new SlashCommandOptionChoiceBuilder().setName(s).setValue(s)).toArray(SlashCommandOptionChoiceBuilder[]::new)));
        return options;
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
    public static void handleSlashCommandRoll(SlashCommandInteraction event, String dicePool, Integer discount, Integer diceKept, @Nullable Boolean enhanceable, Boolean opportunity) {
        final User user = event.getUser();
        final CompletableFuture<InteractionOriginalResponseUpdater> updater = event.respondLater();
        Optional<Pair<Integer, Integer>> originalPointPair = SheetsHandler.getPlotPoints(user).flatMap(integer -> PlayerHandler.getPlayerFromUser(user).map(Player::getDoomPool).map(s -> Pair.of(integer, DoomHandler.getDoom(s))));
        rollDice(dicePool, discount, diceKept, opportunity, user)
                .handle((embedBuilders, throwable) -> Roller.sendResult(enhanceable, updater, embedBuilders.getLeft(), throwable, embedBuilders.getRight()))
                .thenCompose(future -> future)
                .thenAccept(trySend -> trySend.andThen(triple -> Roller.attachListener(user, new RollParameters(dicePool, discount, triple.getRight(), opportunity, diceKept), triple.getLeft(), originalPointPair.orElse(Pair.of(0, 0)))));
    }

    /**
     * A shortcut for rolling dice, updating the plot points and doom points, and generating the embed. A boolean is returned since the Result object is not carried to the next steps, and we need some way to check if a roll is enhanceable.
     *
     * @param dicePool    The dice pool to be rolled
     * @param discount    The discount on plot points in the roll
     * @param diceKept    The amount of dice kept
     * @param opportunity If the roll can have opportunities
     * @param user        The user who made the roll
     * @return A CompletableFuture that contains a Pair that that has a list of the embeds to send, and a boolean determines if the roll is enhanceable
     */
    public static CompletableFuture<Pair<List<EmbedBuilder>, Boolean>> rollDice(String dicePool, Integer discount, Integer diceKept, Boolean opportunity, User user) {
        return Roller.parse(dicePool, pool -> convertSkillsToDice(user, pool))
                .map(Roller::roll)
                .map(pair -> new Result(pair.getLeft(), pair.getRight(), diceKept))
                .map(result -> Roller.handleOpportunities(result, discount, opportunity, value -> DoomHandler.addDoom(user, value), value -> PlotPointUtils.addPlotPointsToPlayer(user, value)))
                .toCompletableFuture()
                .thenCompose(future -> future)
                .thenApply(triple -> Pair.of(Roller.output(triple.getLeft(), discount, opportunity, triple.getMiddle(), triple.getRight()), triple.getLeft().getPlotDiceCost() == 0));
    }

    /**
     * Converts skills into dice by replacing it with text from the spreadsheets. For example, if your pool is 2d8 stealth, and stealth is designated as 2d8 in the character sheet, the pool becomes 2d8 2d8.
     *
     * @param user The user whose character sheet will be checked
     * @param pool The pool that entered
     * @return A Try that attempts to convert a string with a skill into a string with only dice, or an exception if a skill cannot be found
     */
    private static Try<String> convertSkillsToDice(User user, String pool) {
        // We use join to get access to CompletionException, which would allow us to handle the exceptions thrown in the CompletableFuture
        //noinspection unchecked
        final Try<Map<String, String>> skillMap = Try.of(() -> SheetsHandler.getSkills(user).get()).mapFailure(API.Case($(t -> t instanceof ExecutionException), Throwable::getCause));
        return skillMap.map(map -> Arrays.stream(pool.split(" ")).map(s -> map.getOrDefault(s, s)).collect(Collectors.joining(" ")));
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

    /**
     * Handles a text roll by rolling the dice pool, generating the embed, attaching the listener.
     * The text command only allows for keeping 2 dice, can't specify discount, no enhancement override
     *
     * @param user        The user who made the roll
     * @param channel     The channel the message was sent from
     * @param pool        The pool to be rolled
     * @param opportunity If opportunities are enabled
     */
    public static void handleTextCommandRoll(User user, TextChannel channel, String pool, boolean opportunity) {
        Optional<Pair<Integer, Integer>> originalPointPair = SheetsHandler.getPlotPoints(user).flatMap(integer -> PlayerHandler.getPlayerFromUser(user).map(Player::getDoomPool).map(s -> Pair.of(integer, DoomHandler.getDoom(s))));
        rollDice(pool, 0, 2, opportunity, user)
                .handle((embedBuilders, throwable) -> Roller.sendResult(null, channel, embedBuilders.getLeft(), throwable, embedBuilders.getRight()))
                .thenCompose(future -> future)
                .thenAccept(trySend -> trySend.andThen(triple -> Roller.attachListener(user, new RollParameters(pool, 0, triple.getRight(), true, 2), triple.getLeft(), originalPointPair.orElse(Pair.of(0, 0)))));
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        String dicePool = String.join(" ", args);
        handleTextCommandRoll(user, event.getChannel(), dicePool, true);
    }

    @Override
    public void onEvent(SlashCommandInteraction event, User user, VelenArguments args, List<SlashCommandInteractionOption> options, InteractionImmediateResponseBuilder firstResponder) {
        final Boolean opportunity = event.getOptionBooleanValueByName("opportunity").orElse(true);
        final Integer discount = event.getOptionIntValueByName("discount").orElse(0);
        final String dicePool = event.getOptionStringValueByName("dicepool").orElse("");
        final Integer diceKept = event.getOptionIntValueByName("dicekept").orElse(2);
        final Boolean enhanceable = event.getOptionBooleanValueByName("enhanceable").orElse(null);

        handleSlashCommandRoll(event, dicePool, discount, diceKept, enhanceable, opportunity);
    }

}
