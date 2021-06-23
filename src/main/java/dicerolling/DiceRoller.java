package dicerolling;

import discord.TwoDee;
import util.RandomColor;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Creates a RollResult object by using a dice pool
 */
public class DiceRoller {
    public static final String NONE = "*none*";
    private final RollResult rollResult;
    private final DicePool dicePool;

    public DiceRoller(DicePool dicePool) {
        this.dicePool = dicePool;
        rollResult = new RollResult(dicePool.getNumberOfKeptDice());
        // Disable enhancement by default if there is plot dice in the pool
        if (!dicePool.getPlotDice().isEmpty()) {
            dicePool.enableEnhancement(false);
        }
        Random random = new Random();
        rollDice(random, dicePool);
    }

    /**
     * Gets the number of doom in rollResult
     *
     * @return The number of doom points generated from this dice pool
     */
    public int getDoom() {
        return rollResult.getDoomGenerated();
    }

    /**
     * Generates an embed for the result of the dice roll
     *
     * @param author The author of the message
     * @return An embed with the results of each die, the total, the dice picked and dropped, and the tiers hit
     */
    public EmbedBuilder generateResults(MessageAuthor author) {
        //Build embed
        final EmbedBuilder rollResultEmbed = new EmbedBuilder()
                .setDescription("Here's the result for " + createDicePoolString(dicePool))
                .setTitle(TwoDee.getRollTitleMessage())
                .setAuthor(author)
                .setColor(RandomColor.getRandomColor())
                .addField("Regular dice", formatRegularDiceResults(rollResult.getRegularDice(), true), true)
                .addField("Plot dice", formatRegularDiceResults(rollResult.getPlotDice(), true), true)
                .addField("Kept dice", formatRegularDiceResults(rollResult.getKeptDice(), true), true)
                .addField("Picked", formatRegularDiceResults(rollResult.getAllPickedDice(), false), true)
                .addField("Flat bonuses", formatRegularDiceResults(rollResult.getFlatBonuses(), false), true)
                .addField("Dropped", formatRegularDiceResults(rollResult.getAllDroppedDice(), false), true)
                .addField("Total", String.valueOf(rollResult.getTotal()), true)
                .addField("Tier Hit", rollResult.getTierHit(), true);
        if (dicePool.isEnhancementEnabled()) {
            rollResultEmbed.setFooter("Enhance this roll with plot points in the next 60 seconds by clicking on the reactions below!");
        }
        return rollResultEmbed;
    }

    /**
     * Generates the string for the dice in the dice pool
     *
     * @param dicePool The dice pool to generate the string for
     * @return The dice pool in dice as a string
     */
    private String createDicePoolString(DicePool dicePool) {
        final int flatModifier = dicePool.getFlatBonuses().stream().mapToInt(value -> value).sum();
        final String flatModifierString = getFlatModifierString(flatModifier);
        return formatDiceListForRepeatingDiceOfTheSameType(dicePool.getRegularDice(), DiceType.REGULAR) + " " +
                formatDiceListForRepeatingDiceOfTheSameType(dicePool.getPlotDice(), DiceType.PLOT_DIE) + " " +
                formatDiceListForRepeatingDiceOfTheSameType(dicePool.getKeptDice(), DiceType.KEPT_DIE) + " " +
                formatDiceListForRepeatingDiceOfTheSameType(dicePool.getChaosDice(), DiceType.CHAOS_DIE) + " " +
                flatModifierString;
    }

    /**
     * Adds a + to the front of the flat modifier if it's positive, and make it an empty string if it's 0
     *
     * @param flatModifier The value of the flat modifer
     * @return The flat modifier as a formatted string
     */
    private String getFlatModifierString(int flatModifier) {
        final String flatModifierString;
        if (flatModifier == 0) {
            flatModifierString = "";
        }
        else if (flatModifier > 0) {
            flatModifierString = "+" + flatModifier;
        }
        else {
            flatModifierString = String.valueOf(flatModifier);
        }
        return flatModifierString;
    }

    /**
     * Formats te list of rolled dice of a certain type to group the repeating dice together (4 d12s next to each other in the list becomes 4d12)
     *
     * @param diceList The list of dice rolled
     * @param diceType The type of dice being rolled, which determines the abbreviation in the string
     * @return A formatted string of the dice being rolled
     */
    private String formatDiceListForRepeatingDiceOfTheSameType(List<Integer> diceList, DiceType diceType) {
        StringBuilder outputString = new StringBuilder();
        if (!diceList.isEmpty()) {
            int lastDiceFacets = diceList.get(0);
            int consecutiveDiceCount = 0;
            for (int dice : diceList) {
                if (lastDiceFacets != dice) {
                    outputString.append(consecutiveDiceCount).append(diceType.getAbbreviation()).append(lastDiceFacets).append(" ");
                    lastDiceFacets = dice;
                    consecutiveDiceCount = 1;
                }
                else {
                    consecutiveDiceCount++;
                }
            }
            outputString.append(consecutiveDiceCount).append(diceType.getAbbreviation()).append(lastDiceFacets);
        }
        return outputString.toString();
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


    //Roll all of the dice. Plot dice have a minimum value of its facets / 2 if there is only one plot die
    private void rollDice(Random random, DicePool dicePool) {
        //Roll dice
        rollDie(random, dicePool.getRegularDice());
        rollKeptDie(random, dicePool.getKeptDice());
        rollPlotDice(random, dicePool.getPlotDice());
        rollChaosDice(random, dicePool.getChaosDice());
        addFlatBonus(dicePool.getFlatBonuses());
    }

    /**
     * Adds all flat bonuses to the result
     *
     * @param flatBonus The value of the flat bonus
     */
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

    /**
     * Rolls all chaos die. Chaos die are effectively kept dice, but instead inflict a penalty.
     *
     * @param random    The random number generator
     * @param chaosDice The list of chaos die
     */
    private void rollChaosDice(Random random, List<Integer> chaosDice) {
        chaosDice.stream()
                .mapToInt(chaosDie -> (random.nextInt(chaosDie) + 1) * -1)
                .forEach(rollResult::addKeptDice);
    }

    /**
     * Rolls all regular dice
     *
     * @param random The random number generator
     * @param dice   The list of regular dice
     */
    private void rollDie(Random random, List<Integer> dice) {
        dice.stream()
                .mapToInt(die -> random.nextInt(die) + 1)
                .forEach(rollResult::addRegularDice);
    }

    /**
     * Rolls all kept dice
     *
     * @param random   The random number generator
     * @param keptDice The list of kept dice
     */
    private void rollKeptDie(Random random, List<Integer> keptDice) {
        keptDice.stream()
                .mapToInt(keptDie -> random.nextInt(keptDie) + 1)
                .forEach(rollResult::addKeptDice);
    }

}
