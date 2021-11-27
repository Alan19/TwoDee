package rolling;

import com.google.common.collect.Range;
import io.vavr.control.Either;
import io.vavr.control.Try;
import listeners.RollComponentInteractionListener;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.ButtonClickListener;
import org.javacord.api.util.event.ListenerManager;
import util.UtilFunctions;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static rolling.DicePoolBuilder.*;

public class Roller {

    private static final Random random = new SecureRandom();
    public static final String NONE = "*none*";
    public static final Pattern SKILL_PATTERN = Pattern.compile("\\b[a-zA-Z]{3,}\\w*");

    /**
     * Attempts to parse a pool of dice into a list of dice and modifiers.
     *
     * @param pool                  The dice pool string
     * @param skillReplacerFunction A function that converts a given skill into dice. Usually used for pulling information from character sheets.
     * @return A Try that may contain a list of dice and a list of flat modifiers.
     */
    public static Try<Pair<List<Dice>, List<Integer>>> parse(String pool, Function<String, Try<String>> skillReplacerFunction) {
        Try<String> tryReplacePool = preprocessPool(pool, skillReplacerFunction);
        return tryReplacePool.flatMap(replacedPool -> {
            String[] paramArray = replacedPool.split(" ");
            return Arrays.stream(paramArray)
                    .map(Roller::processPoolParam)
                    .collect(PoolCollector.toTripleList());
        });
    }

    /**
     * Loops through the dice pool to see if there are any skills, and then replacing them if there is.
     *
     * @param pool                  The pool to check
     * @param skillReplacerFunction A function that takes the input string and outputs another string with the skills replaced.
     * @return A Try that contains the replaced string. If there are no skills, the Try is guaranteed to be a success but if not, the function passed in the parameter may return an exception.
     */
    private static Try<String> preprocessPool(String pool, Function<String, Try<String>> skillReplacerFunction) {
        final boolean anySkills = Arrays.stream(pool.split(" ")).anyMatch(s -> SKILL_PATTERN.matcher(s).matches());
        return anySkills ? skillReplacerFunction.apply(pool) : Try.success(pool);
    }

    /**
     * Attempts to parse a given element in the dice pool to either a dice, modifier, or skill
     *
     * @param s One element in the dice pool
     * @return A triple to represent an object being one of three data types, a list of dice, an integer, or a string
     */
    private static Try<Either<List<Dice>, Integer>> processPoolParam(String s) {
        final Matcher diceMatcher = DICE_PATTERN.matcher(s);
        final Matcher flatBonusMatcher = FLAT_BONUS_PATTERN.matcher(s);
        final Matcher flatPenaltyMatcher = FLAT_PENALTY_PATTERN.matcher(s);
        //Any type of dice
        if (diceMatcher.matches()) {
            final int count = UtilFunctions.tryParseInt(diceMatcher.group("count")).orElse(1);
            final String type = diceMatcher.group("type");
            final int facets = Integer.parseInt(diceMatcher.group("facets"));
            return Try.success(Either.left(IntStream.range(0, count).mapToObj(value -> new Dice(type, facets)).collect(Collectors.toList())));
        }
        //Flat bonus
        else if (flatBonusMatcher.matches()) {
            return Try.success(Either.right(Integer.parseInt(flatBonusMatcher.group("value"))));
        }
        //Flat penalty
        else if (flatPenaltyMatcher.matches()) {
            return Try.success(Either.right(Integer.parseInt(flatPenaltyMatcher.group("value")) * -1));
        }
        else {
            return Try.failure(new IllegalArgumentException(MessageFormat.format("`{0}` does not result in valid dice, or is not registered on the character sheet!", s)));
        }
    }

    /**
     * Rolls each dice in the dice pool
     *
     * @param diceModifierPair A pair that contains a list of dice to be rolled, and the list of modifiers
     * @return A pair with a list of dice roll results, and a list of roll modifiers
     */
    public static Pair<List<Roll>, List<Integer>> roll(Pair<List<Dice>, List<Integer>> diceModifierPair) {
        return Pair.of(diceModifierPair.getLeft().stream().map(Roller::createRoll).collect(Collectors.toList()), diceModifierPair.getRight());
    }

    private static Roll createRoll(Dice dice) {
        if (dice.getName().equals("pd") || dice.getName().equals("ed")) {
            return new Roll(dice.getName(), random.nextInt(dice.getValue()) + 1, dice.getValue() / 2);
        }
        return new Roll(dice.getName(), random.nextInt(dice.getValue()) + 1);
    }

    /**
     * Handles opportunities on a roll and returns
     *
     * @param result           An object containing the result of a roll
     * @param opportunities    If opportunities are enabled
     * @param doomHandler      Function to handle increases in doom from opportunities
     * @param plotPointHandler Function to handle changes in plot points from plot dice and opportunities
     * @return A try that attempts to contain a pair with the rolls and the modifiers but will return an exception there
     * is an error modifying plot points or doom points
     */
    public static CompletableFuture<Triple<Result, Integer, Integer>> handleOpportunities(Result result, int discount, boolean opportunities, IntFunction<Integer> doomHandler, IntFunction<CompletableFuture<Integer>> plotPointHandler) {
        int doomCount = opportunities ? result.getOpportunities() : 0;
        int plotPointsSpent = result.getPlotDiceCost() - discount;
        if (doomCount > 0) {
            plotPointsSpent--;
        }
        final int newDoomValue = doomHandler.apply(doomCount);
        final CompletableFuture<Integer> newPlotPointFuture = plotPointHandler.apply(plotPointsSpent * -1);
        return newPlotPointFuture.thenApply(newPlotPointValue -> Triple.of(result, newDoomValue, newPlotPointValue));
    }

    /**
     * Generates the output for the pool based on the results of the roll
     *
     * @param result                    The result object to format into an embed.
     * @param postProcessResultFunction A function that could be used to affect the embed for the roll after it has been created. Usually used to add the author, color, title, description, and footer for the embed.
     * @param doomPool                  The name of the active doom pool for the user
     * @param discount                  The plot point discount on the roll. Used to calculate the change in plot points for the plot point expenditure embed.
     * @param opportunities             If opportunities are triggered, used to generate the plot point and doom point embeds
     * @param newDoomPoints             The amount of doom points after the roll, to use for the doom point embed
     * @param newPlotPoints             The amount of plot points the user has after the roll
     * @return A list of embeds containing the result of the roll
     */
    public static List<EmbedBuilder> output(Result result, UnaryOperator<EmbedBuilder> postProcessResultFunction, String doomPool, int discount, boolean opportunities, Integer newDoomPoints, Integer newPlotPoints) {
        List<EmbedBuilder> outputs = new ArrayList<>();
        final EmbedBuilder rollEmbed = new EmbedBuilder()
                .addField("Regular and chaos dice", formatRegularDiceResults(result.getRegularAndChaosDice(), true), true)
                .addField("Plot dice", formatRegularDiceResults(result.getPlotDice(), true), true)
                .addField("Kept dice", formatRegularDiceResults(result.getKeptDice(), true), true)
                .addField("Picked", formatRegularDiceResults(result.getPickedDice(), false), true)
                .addField("Flat bonuses", formatRegularDiceResults(result.getFlatBonuses(), false), true)
                .addField("Dropped", formatRegularDiceResults(result.getDroppedDice(), false), true)
                .addField("Total", String.valueOf(result.getTotal()), true)
                .addField("Tier Hit", getTierHitString(result.getTotal()), true)
                .setFooter("Click on one of the components within the next 60 seconds to enhance or re-roll the roll");
        outputs.add(postProcessResultFunction.apply(rollEmbed));
        boolean opportunityTriggered = opportunities && result.getOpportunities() > 0;
        final int plotPointsSpent = result.getPlotDiceCost() - discount;
        if (plotPointsSpent != 0) {
            outputs.add(new EmbedBuilder()
                    .setTitle("Using " + plotPointsSpent + " plot point(s)!")
                    .addField("Plot points", getPlotPointSpendingText(plotPointsSpent, newPlotPoints, opportunityTriggered)));
        }
        if (opportunities && result.getOpportunities() > 0) {
            outputs.add(new EmbedBuilder()
                    .setTitle("An opportunity!")
                    .addField("Plot points", newPlotPoints - 1 + " → " + newPlotPoints)
                    .addField(doomPool, newDoomPoints - result.getOpportunities() + " → " + newDoomPoints)
            );
        }
        return outputs;
    }

    public static String getPlotPointSpendingText(int plotPointsSpent, Integer newPlotPoints, boolean opportunityTriggered) {
        return MessageFormat.format("{0} → {1}", newPlotPoints + plotPointsSpent - (opportunityTriggered ? 1 : 0), newPlotPoints - (opportunityTriggered ? 1 : 0));
    }

    public static String getTierHitString(int total) {
        Pair<Optional<String>, Optional<String>> tiers = getTierHit(total);
        StringBuilder difficultyString = new StringBuilder();
        tiers.getLeft()
                .ifPresent(difficultyString::append);
        tiers.getRight()
                .ifPresent(stringRangePair -> difficultyString.append(", Extraordinary ")
                        .append(stringRangePair)
                );
        return difficultyString.toString();

    }

    public static Pair<Optional<String>, Optional<String>> getTierHit(int total) {
        Pair<String, Range<Integer>> noneRange = Pair.of("None", Range.lessThan(3));
        Pair<String, Range<Integer>> easyRange = Pair.of("Easy", Range.closedOpen(3, 7));
        Pair<String, Range<Integer>> averageRange = Pair.of("Average", Range.closedOpen(7, 11));
        Pair<String, Range<Integer>> hardRange = Pair.of("Hard", Range.closedOpen(11, 15));
        Pair<String, Range<Integer>> formidableRange = Pair.of("Formidable", Range.closedOpen(15, 19));
        Pair<String, Range<Integer>> heroicRange = Pair.of("Heroic", Range.closedOpen(19, 23));
        Pair<String, Range<Integer>> incredibleRange = Pair.of("Incredible", Range.closedOpen(23, 27));
        Pair<String, Range<Integer>> ridiculousRange = Pair.of("Ridiculous", Range.closedOpen(27, 31));
        Pair<String, Range<Integer>> impossibleRange = Pair.of("Impossible", Range.atLeast(31));
        List<Pair<String, Range<Integer>>> difficultyRanges = Arrays.asList(impossibleRange, ridiculousRange, incredibleRange, heroicRange, formidableRange, hardRange, averageRange, easyRange, noneRange);
        return Pair.of(
                difficultyRanges.stream()
                        .filter(difficultyRange -> difficultyRange.getRight().contains(total))
                        .findFirst()
                        .map(Pair::getLeft),
                difficultyRanges.subList(0, difficultyRanges.size() - 1)
                        .stream()
                        .filter(stringRangePair -> stringRangePair.getRight().contains(total - 7))
                        .findFirst()
                        .map(Pair::getLeft)
        );

    }

    /**
     * Converts a list of integers to a string with the 1s bolded
     *
     * @param s       A list of integers with dice outcomes
     * @param boldOne If 1s should be bolded in the list
     * @return A string of integers separated by commas with the 1s bolded, or None if the list is empty
     */
    private static String formatRegularDiceResults(List<Integer> s, boolean boldOne) {
        if (s.isEmpty()) {
            return NONE;
        }
        else {
            return s.stream().map(die -> die == 1 && boldOne ? "**1**" : String.valueOf(die)).collect(Collectors.joining(", "));
        }
    }

    public static void attachListener(User user, RollParameters rollParameters, Message message, Pair<Integer, Integer> originalPointPair) {
        final AtomicReference<ListenerManager<ButtonClickListener>> reference = new AtomicReference<>();
        final RollComponentInteractionListener listener = new RollComponentInteractionListener(user, rollParameters, reference, originalPointPair, message);
        final ListenerManager<ButtonClickListener> listenerManager = message.addButtonClickListener(listener);
        reference.set(listenerManager);
        listener.startRemoveTimer();
    }

    /**
     * Attaches components to the roll, and then sends it, or sends an ephemeral message if there is an exception
     *
     * @param updaterFuture A CompletableFuture containing the updater object
     * @param embeds        The embeds that contain the results of the roll
     * @param throwable     The error for the roll that was added through the Try object
     * @param components    An array of components that typically contains buttons for enhancing and re-rolling
     * @return A CompletableFuture containing a Try that returns a success if nothing is thrown, with the updater, message, and the calculated enhanceable status of the roll
     */
    public static CompletableFuture<Try<Message>> sendResult(CompletableFuture<InteractionOriginalResponseUpdater> updaterFuture, List<EmbedBuilder> embeds, @Nullable Throwable throwable, HighLevelComponent[] components) {
        if (throwable != null) {
            return updaterFuture.thenAccept(updater -> updater.setContent(throwable.getMessage()).setFlags(InteractionCallbackDataFlag.EPHEMERAL).update()).thenApply(unused -> Try.failure(throwable));
        }
        else {
            return updaterFuture.thenCompose(updater -> updater.addEmbeds(embeds)
                    .addComponents(components)
                    .update()
                    .thenApply(Try::success));
        }
    }

    public static CompletableFuture<Try<Message>> sendResult(TextChannel channel, List<EmbedBuilder> embeds, @Nullable Throwable throwable, HighLevelComponent[] components) {
        if (throwable != null) {
            return channel.sendMessage(throwable.getMessage()).thenApply(message -> Try.failure(throwable));
        }
        else {
            return new MessageBuilder().addEmbeds(embeds)
                    .addComponents(components)
                    .send(channel)
                    .thenApply(Try::success);
        }
    }

}
