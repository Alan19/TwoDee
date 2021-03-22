package dicerolling;

import discord.TwoDee;
import logic.RandomColor;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DiceRoller {
    public static final String NONE = "*none*";
    private final RollResult rollResult;
    private final DicePool dicePool;

    public DiceRoller(DicePool dicePool) {
        this.dicePool = dicePool;
        rollResult = new RollResult(dicePool.getNumberOfKeptDice());
        Random random = new Random();
        rollDice(random, dicePool);
    }

    public int getDoom() {
        return rollResult.getDoomGenerated();
    }

    public EmbedBuilder generateResults(MessageAuthor author) {
        //Build embed
        return buildResultEmbed(author, rollResult);
    }

    private EmbedBuilder buildResultEmbed(MessageAuthor author, RollResult result) {
        return new EmbedBuilder()
                .setDescription("Here's the result for" + createDicePoolString(dicePool))
                .setTitle(TwoDee.getRollTitleMessage())
                .setAuthor(author)
                .setColor(RandomColor.getRandomColor())
                .addField("Regular dice", formatRegularDiceResults(result.getRegularDice(), true), true)
                .addField("Plot dice", formatRegularDiceResults(result.getPlotDice(), true), true)
                .addField("Kept dice", formatRegularDiceResults(result.getKeptDice(), true), true)
                .addField("Picked", formatRegularDiceResults(result.getAllPickedDice(), false), true)
                .addField("Flat bonuses", formatRegularDiceResults(result.getFlatBonuses(), false), true)
                .addField("Dropped", formatRegularDiceResults(result.getAllDroppedDice(), false), true)
                .addField("Total", String.valueOf(result.getTotal()), true)
                .addField("Tier Hit", result.getTierHit(), true);
    }

    private String createDicePoolString(DicePool dicePool) {
        StringBuilder dicePoolString = new StringBuilder();
        dicePool.getRegularDice().forEach(integer -> dicePoolString.append(" d").append(integer));
        dicePool.getPlotDice().forEach(integer -> dicePoolString.append(" pd").append(integer));
        dicePool.getKeptDice().forEach(integer -> dicePoolString.append(" kd").append(integer));
        dicePool.getFlatBonuses().stream().map(integer -> (integer > 0) ? ("+" + integer) : integer).forEach(dicePoolString::append);
        return dicePoolString.toString();
    }

    /**
     * Converts a list of integers to a string with the 1s bolded
     *
     * @param s       A list of integers with dice outcomes
     * @param boldOne If 1s should be bolded in the list
     * @return A string of integers separated by commas with the 1s bolded, or None if the list is empty
     */
    private String formatRegularDiceResults(List<Integer> s, boolean boldOne) {
        if (s.isEmpty()) {
            return NONE;
        }
        else {
            return s.stream().map(die -> die == 1 && boldOne ? "**1**" : String.valueOf(die)).collect(Collectors.joining(", "));
        }
    }


    //Roll all of the dice. Plot dice have a minimum value of its maximum roll/2
    private void rollDice(Random random, DicePool dicePool) {
        //Roll dice
        rollDie(random, dicePool.getRegularDice());
        rollKeptDie(random, dicePool.getKeptDice());
        rollPlotDice(random, dicePool.getPlotDice());
        addFlatBonus(dicePool.getFlatBonuses());
    }

    private void addFlatBonus(List<Integer> flatBonus) {
        flatBonus.forEach(rollResult::addFlatBonus);
    }

    /**
     * Adds plot dice to the roll result. A plot dice has a minimum value of half its facets
     *
     * @param random   The random number generator that generates the dice value
     * @param plotDice The list of plot dice to roll
     */
    private void rollPlotDice(Random random, List<Integer> plotDice) {
        plotDice.stream()
                .mapToInt(die -> plotDice.size() <= 1 ? Math.max(random.nextInt(die) + 1, die / 2) : random.nextInt(die) + 1)
                .forEach(rollResult::addPlotDice);
    }

    private void rollDie(Random random, List<Integer> dice) {
        dice.stream()
                .mapToInt(die -> random.nextInt(die) + 1)
                .forEach(rollResult::addRegularDice);
    }

    private void rollKeptDie(Random random, List<Integer> keptDice) {
        keptDice.stream()
                .mapToInt(keptDie -> random.nextInt(keptDie) + 1)
                .forEach(rollResult::addKeptDice);
    }

    public int getTotal() {
        return rollResult.getTotal();
    }
}
