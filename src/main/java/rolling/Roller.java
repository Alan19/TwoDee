package rolling;

import com.google.common.collect.ImmutableList;
import doom.DoomHandler;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import util.UtilFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static rolling.DicePoolBuilder.*;
import static rolling.RollResult.NONE;

public class Roller {
    /**
     * Attempts to parse a pool of dice into a list of dice and modifiers.
     *
     * @param pool     The dice pool string
     * @param operator A function that converts a given skill into dice. Usually used for pulling information from character sheets.
     * @return A Try that may contain a list of dice and a list of flat modifiers.
     */
    public static Try<Pair<List<Dice>, List<Integer>>> parse(String pool, Function<List<String>, Try<List<Dice>>> operator) {
        String[] paramArray = pool.split(" ");
        final Triple<List<Dice>, List<Integer>, List<String>> diceModifierSkillTuple = Arrays.stream(paramArray)
                .map(Roller::processPoolParam)
                .collect(PoolCollector.toTripleList());
        final List<String> skills = diceModifierSkillTuple.getRight();
        final List<Dice> dice = diceModifierSkillTuple.getLeft();
        final List<Integer> modifiers = diceModifierSkillTuple.getMiddle();
        return operator.apply(skills).map(skill -> Pair.of(ImmutableList.<Dice>builder().addAll(dice).addAll(skill).build(), modifiers));
    }

    /**
     * Attempts to parse a given element in the dice pool to either a dice, modifier, or skill
     *
     * @param s One element in the dice pool
     * @return A triple to represent an object being one of three data types, a list of dice, an integer, or a string
     */
    private static Triple<List<Dice>, Integer, String> processPoolParam(String s) {
        final Matcher diceMatcher = DICE_PATTERN.matcher(s);
        final Matcher flatBonusMatcher = FLAT_BONUS_PATTERN.matcher(s);
        final Matcher flatPenaltyMatcher = FLAT_PENALTY_PATTERN.matcher(s);
        //Any type of dice
        if (diceMatcher.matches()) {
            final int count = UtilFunctions.tryParseInt(diceMatcher.group("count")).orElse(1);
            final String type = diceMatcher.group("type");
            final int facets = Integer.parseInt(diceMatcher.group("facets"));
            return Triple.of(IntStream.range(0, count).mapToObj(value -> new Dice(type, facets)).collect(Collectors.toList()), null, null);
        }
        //Flat bonus
        else if (flatBonusMatcher.matches()) {
            return Triple.of(null, Integer.parseInt(flatBonusMatcher.group("value")), null);
        }
        //Flat penalty
        else if (flatPenaltyMatcher.matches()) {
            return Triple.of(null, Integer.parseInt(flatBonusMatcher.group("value")) * -1, null);
        }
        else {
            return Triple.of(null, null, s);
        }
    }

    /**
     * Rolls each dice in the dice pool
     *
     * @param diceModifierPair A pair that contains a list of dice to be rolled, and the list of modifiers
     * @return A pair with a list of dice roll results, and a list of roll modifiers
     */
    public static Pair<List<Roll>, List<Integer>> roll(Pair<List<Dice>, List<Integer>> diceModifierPair) {
        Random random = new Random();
        return Pair.of(diceModifierPair.getLeft().stream().map(dice -> new Roll(dice.getName(), random.nextInt(dice.getValue()) + 1)).collect(Collectors.toList()), diceModifierPair.getRight());
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
    public static Try<Pair<Result, CompletableFuture<Pair<Integer, Integer>>>> handleOpportunities(Result result, int plotPointsSpent, boolean opportunities, IntFunction<CompletableFuture<Integer>> doomHandler, IntFunction<CompletableFuture<Integer>> plotPointHandler) {
        int doomCount = opportunities ? result.getOpportunities() : 0;
        if (doomCount > 0) {
            plotPointsSpent--;
        }
        final CompletableFuture<Pair<Integer, Integer>> tryCompletableFuture = doomHandler.apply(doomCount).thenCombine(plotPointHandler.apply(plotPointsSpent * -1), Pair::of);
        return Try.success(Pair.of(result, tryCompletableFuture));
    }

    public static List<EmbedBuilder> output(Result result, int plotPointsSpent, boolean opportunities, Integer newDoomPoints, Integer newPlotPoints) {
        List<EmbedBuilder> outputs = new ArrayList<>();
        outputs.add(new EmbedBuilder()
                .addField("Regular and chaos dice", formatRegularDiceResults(result.getRegularAndChaosDice(), true), true)
                .addField("Plot dice", formatRegularDiceResults(result.getPlotDice(), true), true)
                .addField("Kept dice", formatRegularDiceResults(result.getKeptDice(), true), true)
                .addField("Picked", formatRegularDiceResults(result.getPickedDice(), false), true)
                .addField("Flat bonuses", formatRegularDiceResults(result.getFlatBonuses(), false), true)
                .addField("Dropped", formatRegularDiceResults(result.getDroppedDice(), false), true)
                .addField("Total", String.valueOf(result.getTotal()), true)
                .addField("Tier Hit", getTierHit(result.getTotal()), true));
        boolean opportunityTriggered = opportunities && result.getOpportunities() > 0;
        if (plotPointsSpent != 0) {
            outputs.add(new EmbedBuilder()
                    .setTitle("Using " + plotPointsSpent + " plot point(s)!")
                    .addField("Plot points", getPlotPointSpendingText(plotPointsSpent, newPlotPoints, opportunityTriggered)));
        }
        if (opportunities && result.getOpportunities() > 0) {
            outputs.add(new EmbedBuilder()
                    .setTitle("An opportunity!")
                    .addField("Plot points", newPlotPoints - 1 + " → " + newPlotPoints)
                    .addField(DoomHandler.getActivePool(), newDoomPoints - result.getOpportunities() + " → " + newDoomPoints)
            );
        }
        return outputs;
    }

    public static String getPlotPointSpendingText(int plotPointsSpent, Integer newPlotPoints, boolean opportunityTriggered) {
        return newPlotPoints + plotPointsSpent - (opportunityTriggered ? 1 : 0) + " → " + (newPlotPoints - (opportunityTriggered ? 1 : 0));
    }

    //    public static Result createResult(Pair<List<Roll>, List<Integer>> rollModifierPair, int plotPointsUsed, Boolean enhanceable, Boolean opportunity, Integer diceKept) {
//        final Result result = new Result(rollModifierPair.getLeft(), rollModifierPair.getRight(), diceKept, plotPointsUsed);
//        return
//        EmbedBuilder output = new EmbedBuilder()
//                .addField("Regular and chaos dice", formatRegularDiceResults(result.getRegularAndChaosDice(), true), true)
//                .addField("Plot dice", formatRegularDiceResults(result.getPlotDice(), true), true)
//                .addField("Kept dice", formatRegularDiceResults(result.getKeptDice(), true), true)
//                .addField("Picked", formatRegularDiceResults(result.getPickedDice(), false), true)
//                .addField("Flat bonuses", formatRegularDiceResults(result.getFlatBonuses(), false), true)
//                .addField("Dropped", formatRegularDiceResults(result.getDroppedDice(), false), true)
//                .addField("Total", String.valueOf(result.getTotal()), true)
//                .addField("Tier Hit", getTierHit(result.getTotal()), true);
//        // TODO Make sure check works with plot dice
//        if (opportunity && rollModifierPair.getLeft().stream().filter(roll -> roll.getValue() == 1))
//        if (plotPointsUsed != 0) {
//
//        }
//        EmbedBuilder doom = new EmbedBuilder();
//        EmbedBuilder plotPoints = new EmbedBuilder();
//        return Collections.list
//    }
//
    private static String getTierHit(int total) {
        return null;
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
}
