package dicerolling;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import discord.TwoDee;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import util.RandomColor;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that stores information about the results of a getResults from a dice pool
 */
public class RollResult implements PoolResultWithEmbed {
    public static final String NONE = "*none*";
    private final int diceKept;
    private final boolean opportunity;
    private final boolean enhanceable;
    private final List<Integer> regularDice;
    private final List<Integer> plotDice;
    private final List<Integer> keptDice;
    private final List<Integer> chaosDice;
    private final List<Integer> flatBonus;
    private final String poolString;
    private final int plotPointsSpent;

    public RollResult(List<Integer> regularDice, List<Integer> plotDice, List<Integer> keptDice, List<Integer> chaosDice, List<Integer> enhancedDice, List<Integer> flatBonus, int diceKept, int discount, boolean opportunity, boolean enhanceable) {
        final Random random = new Random();
        this.poolString = createDicePoolString(regularDice, plotDice, keptDice, chaosDice, enhancedDice, flatBonus);
        this.plotPointsSpent = plotDice.stream().mapToInt(die -> die / 2).sum() - discount;
        this.regularDice = regularDice.stream().map(die -> random.nextInt(die) + 1).collect(Collectors.toList());
        this.plotDice = Stream.concat(plotDice.stream(), enhancedDice.stream()).map(die -> (plotDice.size() + enhancedDice.size() > 1) ? random.nextInt(die) + 1 : Math.max(die / 2, random.nextInt(die) + 1)).collect(Collectors.toList());
        this.keptDice = keptDice.stream().map(die -> random.nextInt(die) + 1).collect(Collectors.toList());
        this.chaosDice = chaosDice.stream().map(die -> (random.nextInt(die) + 1) * -1).collect(Collectors.toList());
        this.flatBonus = flatBonus;
        this.diceKept = diceKept;
        this.opportunity = opportunity;
        this.enhanceable = enhanceable;
    }

    public int getPlotPointsSpent() {
        return plotPointsSpent;
    }

    /**
     * Generates the string for the dice in the dice pool
     *
     * @param regularDice  The amount of dice in the dice pool
     * @param plotDice     The plot dice in the dice pool
     * @param keptDice     The kept dice in the dice pool
     * @param chaosDice    The chaos dice in the dice pool
     * @param enhancedDice The enhanced dice in the dice pool
     * @return The dice pool in dice as a string
     */
    private String createDicePoolString(List<Integer> regularDice, List<Integer> plotDice, List<Integer> keptDice, List<Integer> chaosDice, List<Integer> enhancedDice, List<Integer> flatBonus) {
        final int flatModifier = flatBonus.stream().mapToInt(value -> value).sum();
        final String flatModifierString = getFlatModifierString(flatModifier);
        return MessageFormat.format("{0} {1} {2} {3} {4} {5}",
                formatDiceListForRepeatingDiceOfTheSameType(regularDice, DiceType.REGULAR),
                formatDiceListForRepeatingDiceOfTheSameType(plotDice, DiceType.PLOT_DIE),
                formatDiceListForRepeatingDiceOfTheSameType(enhancedDice, DiceType.ENHANCED_DIE),
                formatDiceListForRepeatingDiceOfTheSameType(keptDice, DiceType.KEPT_DIE),
                formatDiceListForRepeatingDiceOfTheSameType(chaosDice, DiceType.CHAOS_DIE),
                flatModifierString);
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
     * Generates an embed for the result of the dice getResults
     *
     * @return An embed with the results of each die, the total, the dice picked and dropped, and the tiers hit
     */
    @Override
    public EmbedBuilder getResultEmbed() {
        //Build embed
        final EmbedBuilder rollResultEmbed = new EmbedBuilder()
                .setDescription("Here's the result for " + poolString)
                .setTitle(TwoDee.getRollTitleMessage())
                .setColor(RandomColor.getRandomColor())
                .addField("Regular dice", formatRegularDiceResults(getRegularDice(), true), true)
                .addField("Plot dice", formatRegularDiceResults(plotDice, true), true)
                .addField("Kept dice", formatRegularDiceResults(keptDice, true), true)
                .addField("Picked", formatRegularDiceResults(getAllPickedDice(), false), true)
                .addField("Flat bonuses", formatRegularDiceResults(flatBonus, false), true)
                .addField("Dropped", formatRegularDiceResults(getAllDroppedDice(), false), true)
                .addField("Total", String.valueOf(getTotal()), true)
                .addField("Tier Hit", getTierHit(), true);
        if (enhanceable) {
            rollResultEmbed.setFooter("Enhance this getResults with plot points in the next 60 seconds by clicking on the reactions below!");
        }
        return rollResultEmbed;
    }

    private List<Integer> getRegularDice() {
        return Stream.concat(regularDice.stream(), chaosDice.stream()).collect(Collectors.toList());
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


    @Override
    public int getTotal() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedChaosDie = TreeMultiset.create(Comparator.naturalOrder());
        sortedChaosDie.addAll(chaosDice);


        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        final int plotDie = sortedPlotDice.stream().limit(1).mapToInt(Integer::intValue).findFirst().orElse(0);
        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        return sortedRegularDice.stream().limit(diceKept).mapToInt(Integer::intValue).sum() + plotDie + keptDice.stream().mapToInt(Integer::intValue).sum() + flatBonus.stream().mapToInt(Integer::intValue).sum() + sortedChaosDie.stream().limit(2).mapToInt(Integer::intValue).sum();
    }

    @Override
    public int getDoomGenerated() {
        return opportunity ? Math.toIntExact(regularDice.stream().filter(integer -> integer == 1).count() + plotDice.stream().filter(integer -> integer == 1).count() + keptDice.stream().filter(integer -> integer == 1).count()) : 0;
    }

    public List<Integer> getPickedRegularDice() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        Multiset<Integer> sortedChaosDice = TreeMultiset.create(Comparator.naturalOrder());
        sortedChaosDice.addAll(chaosDice);

        return Stream.concat(sortedRegularDice.stream().limit(diceKept), sortedChaosDice.stream().limit(2)).collect(Collectors.toList());
    }

    public List<Integer> getDroppedRegularDice() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        Multiset<Integer> sortedChaosDice = TreeMultiset.create(Comparator.naturalOrder());
        sortedChaosDice.addAll(chaosDice);

        return Stream.concat(sortedRegularDice.stream().skip(diceKept), sortedChaosDice.stream().skip(2)).collect(Collectors.toList());
    }

    public OptionalInt getPickedPlotDie() {
        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        return sortedPlotDice.stream().limit(1).mapToInt(Integer::intValue).findFirst();
    }

    public List<Integer> getDegradedPlotDice() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        return sortedPlotDice.stream().skip(1).collect(Collectors.toList());
    }

    public List<Integer> getAllPickedDice() {
        final ArrayList<Integer> picked = new ArrayList<>(getPickedRegularDice());
        getPickedPlotDie().ifPresent(picked::add);
        picked.addAll(keptDice);
        return picked;
    }

    public List<Integer> getAllDroppedDice() {
        final ArrayList<Integer> dropped = new ArrayList<>(getDroppedRegularDice());
        dropped.addAll(getDegradedPlotDice());
        return dropped;
    }

}
