package logic;

import configs.Settings;
import doom.DoomHandler;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import pw.mihou.velen.interfaces.Velen;
import pw.mihou.velen.interfaces.VelenCommand;
import pw.mihou.velen.interfaces.VelenHybridHandler;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenHybridArguments;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.responder.VelenGeneralResponder;
import rolling.Result;
import rolling.RollOutput;
import rolling.RollParameters;
import rolling.Roller;
import sheets.PlotPointUtils;
import sheets.SheetsHandler;
import util.ComponentUtils;
import util.RandomColor;
import util.UtilFunctions;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Rolls a pool of dice based on the input. After rolling, adds doom points to doom pool and makes appropriate changes the player's plot point count based on input options. If the DM is rolling, plot points they spend come from the doom point pool.
 */
public class RollLogic implements VelenHybridHandler {

    public static final SlashCommandOption DICE_POOL = SlashCommandOption.create(SlashCommandOptionType.STRING, "dicepool", "The dice pool to roll with.", true);
    public static final SlashCommandOption DISCOUNT = SlashCommandOption.create(SlashCommandOptionType.LONG, "discount", "The number of plot points to discount (negative results in a plot point cost increase).", false);
    public static final SlashCommandOption DICE_KEPT = SlashCommandOption.create(SlashCommandOptionType.LONG, "dicekept", "The number of dice kept. Keeps two dice by default.", false);
    public static final SlashCommandOption ENHANCEABLE = SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "enhanceable", "Allows the roll to be enhanced after the roll.", false);
    public static final SlashCommandOption OPPORTUNITY = SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "opportunity", "Allows for opportunities on the roll. Defaults to true.", false);
    private final int diceKept;

    public RollLogic(int diceKept) {
        this.diceKept = diceKept;
    }

    public RollLogic() {
        this.diceKept = 2;
    }

    public static void setupRollCommand(Velen velen) {
        RollLogic rollLogic = new RollLogic();
        VelenCommand.ofHybrid("roll", "Roll some dice!", velen, rollLogic)
                .addOptions(DICE_POOL, DISCOUNT, ENHANCEABLE, DICE_KEPT, OPPORTUNITY)
                .addFormats("roll :[dicepool:of(string):hasMany()]")
                .addShortcuts("r", "r2")
                .attach();
        List.of(1, 3, 4, 5).forEach(integer -> VelenCommand.ofHybrid("roll%d".formatted(integer), "Roll some dice and keeps %s dice!".formatted(integer), velen, new RollLogic(integer))
                .addOptions(DICE_POOL, DISCOUNT, ENHANCEABLE, OPPORTUNITY)
                .addFormats("roll%d :[dicepool:of(string):hasMany()]".formatted(integer))
                .addShortcuts("r%d".formatted(integer))
                .attach());
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
    public static void handleRoll(VelenGeneralEvent event, String dicePool, Integer discount, Integer diceKept, @Nullable Boolean enhanceable, Boolean opportunity) {
        final User user = event.getUser();
        final RollParameters rollParameters = new RollParameters(dicePool, discount, enhanceable, opportunity, diceKept);
        final VelenGeneralResponder updaterFuture = event.createResponder();
        Optional<Pair<Integer, Integer>> originalPointPair = SheetsHandler.getPlotPoints(user)
                .flatMap(integer -> DoomHandler.getUserDoomPool(user)
                        .map(s -> Pair.of(integer, DoomHandler.getDoom(s)))
                );
        rollDice(dicePool, discount, diceKept, opportunity, user, UtilFunctions.getUsernameInChannel(user, event.getChannel()))
                .onFailure(throwable -> updaterFuture
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .setContent(throwable.getMessage()).respond())
                .onSuccess(rollOutput -> updaterFuture.addEmbeds(rollOutput.embeds().toArray(value -> new EmbedBuilder[]{}))
                        .addComponents(ComponentUtils.createRollComponentRows(true, Optional.ofNullable(enhanceable).orElse(rollOutput.plotDiceUsed()), rollOutput.rollTotal()))
                        .respond()
                        .thenAccept(message -> Roller.attachEmotesAndListeners(user, rollParameters, originalPointPair.orElse(Pair.of(0, 0)), rollOutput, message)));
    }

    /**
     * A shortcut for rolling dice, updating the plot points and doom points, and generating the embed. A boolean is returned since the Result object is not carried to the next steps, and we need some way to check if a roll is enhanceable.
     *
     * @param dicePool    The dice pool to be rolled, skills are converted into dice
     * @param discount    The discount on plot points in the roll
     * @param diceKept    The amount of dice kept
     * @param opportunity If the roll can have opportunities
     * @param user        The user who made the roll
     * @return A CompletableFuture that contains a Triple that that has a list of the embeds to send, a boolean states if the roll uses any plot dice, and the total of the roll
     */
    public static Try<RollOutput> rollDice(String dicePool, Integer discount, Integer diceKept, Boolean opportunity, User user, String username) {
        return Roller.parse(dicePool, pool -> convertSkillsToDice(user, pool))
                .map(Roller::roll)
                .map(pair -> new Result(pair.getLeft(), pair.getRight(), diceKept))
                // TODO Convert to CompletableFuture earlier
                .map(result -> Roller.handlePoints(result, discount, opportunity, value -> DoomHandler.addDoomOnOpportunity(user, value), value -> PlotPointUtils.addPlotPointsOnRoll(user, value)))
                .map(changes -> Roller.output(changes, builder -> postProcessResult(dicePool, user, builder, username), DoomHandler.getDoomPoolOrDefault(user), discount, opportunity));
    }

    /**
     * Modifies the embed generated on a roll to include information about the user, a random color, and a random roll title
     *
     * @param dicePool The pool of dice that was rolled, after conversion
     * @param user     The user that rolled
     * @param builder  The EmbedBuilder that contains the results of the dice roll
     * @param username The display name for the user, generally their nickname
     * @return An embed that has information about the user and the dice pool added to it
     */
    private static EmbedBuilder postProcessResult(String dicePool, User user, EmbedBuilder builder, String username) {
        // TODO Use server profile and nicknames
        final Try<String> processedDicePool = Roller.preprocessPool(dicePool, s -> convertSkillsToDice(user, s));
        return builder.setAuthor(username, null, user.getAvatar())
                .setColor(RandomColor.getRandomColor())
                .setTitle(Settings.getQuotes().getRandomRollQuote())
                .setDescription(MessageFormat.format("Here are the results for **{0}**", processedDicePool.getOrElse(dicePool)));
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
        return Try.of(() -> SheetsHandler.getSkills(user).get())
                .map(map -> Arrays.stream(pool.split(" ")).map(s -> map.getOrDefault(s, s)).collect(Collectors.joining(" ")));
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
    public void onEvent(VelenGeneralEvent event, VelenGeneralResponder responder, User user, VelenHybridArguments args) {
        final String dicePool = args.getManyWithName("dicepool").orElse("");
        final Integer kept = args.withName("dicekept").flatMap(VelenOption::asInteger).orElse(this.diceKept);
        final Boolean opportunity = args.withName("opportunity").flatMap(VelenOption::asBoolean).orElse(true);
        final Boolean enhanceable = args.withName("enhanceable").flatMap(VelenOption::asBoolean).orElse(null);
        final Long discount = args.withName("discount").flatMap(VelenOption::asLong).orElse(0L);

        handleRoll(event, dicePool, Math.toIntExact(discount), kept, enhanceable, opportunity);

    }
}
