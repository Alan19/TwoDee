package dicerolling;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A PoolResult that disregards dropped dice to improve performance by consolidating duplicate results
 * This reduces the number of elements tracked in the result HashMap
 */
public class FastRollResult implements BuildablePoolResult {
    private final Multiset<Integer> regularDice;
    private final Multiset<Integer> plotDice;
    private final Multiset<Integer> keptDice;
    private final int flatBonus;
    private final int doomGenerated;
    private final int diceKept;

    public FastRollResult(int numberOfDiceKept) {
        regularDice = TreeMultiset.create(Comparator.reverseOrder());
        plotDice = TreeMultiset.create(Comparator.reverseOrder());
        keptDice = TreeMultiset.create(Comparator.reverseOrder());
        flatBonus = 0;
        doomGenerated = 0;
        diceKept = numberOfDiceKept;
    }

    /**
     * Private constructor to create another FastRollResult based on the original
     *
     * @param regularDice   The set of regular dice rolled that the new object will have
     * @param plotDice      The set of plot dice rolled that the new object will have
     * @param keptDice      The set of kept dice rolled that the new object will have
     * @param flatBonus     A list of the flat bonuses in the pool
     * @param doomGenerated The amount of doom generated by the getResults
     * @param diceKept      The number of regular dice kept
     */
    private FastRollResult(Multiset<Integer> regularDice, Multiset<Integer> plotDice, Multiset<Integer> keptDice, int flatBonus, int doomGenerated, int diceKept) {
        this.regularDice = regularDice;
        this.plotDice = plotDice;
        this.keptDice = keptDice;
        this.flatBonus = flatBonus;
        this.doomGenerated = doomGenerated;
        this.diceKept = diceKept;
    }

    /**
     * Adds a new dice to the regular dice result pool and drop the ones that are not kept
     *
     * @param diceValue The dice value of the new dice to add
     * @return A deep copy of FastRollResult with the die added and the excess regular dice discarded and an updated doom counter if the value of the die is 1
     */
    @Override
    public BuildablePoolResult addRegularDice(int diceValue) {
        Multiset<Integer> updatedDice = copyDiceList(regularDice);
        updatedDice.add(diceValue);
        final List<Integer> kept = updatedDice.stream().limit(diceKept).collect(Collectors.toList());
        final Multiset<Integer> newDiceList = TreeMultiset.create(Comparator.reverseOrder());
        newDiceList.addAll(kept);
        return new FastRollResult(newDiceList, plotDice, keptDice, flatBonus, doomGenerated + (diceValue == 1 ? 1 : 0), diceKept);
    }

    /**
     * Adds a new plot dice to the result and move all of the plot dice beyond the first to the regular dice result pool
     *
     * @param diceValue The value of the plot die
     * @return A deep copy of FastRollResult with the plot die added and the excess plot dice moved to the regular dice result pool
     */
    @Override
    public FastRollResult addPlotDice(int diceValue) {
        Multiset<Integer> updatedPlotDice = copyDiceList(plotDice);
        updatedPlotDice.add(diceValue);

        final List<Integer> kept = updatedPlotDice.stream().limit(1).collect(Collectors.toList());
        final Multiset<Integer> newPlotDiceList = TreeMultiset.create(Comparator.reverseOrder());
        newPlotDiceList.addAll(kept);

        final FastRollResult[] fastRollResult = {new FastRollResult(regularDice, newPlotDiceList, keptDice, flatBonus, doomGenerated + (diceValue == 1 ? 1 : 0), diceKept)};
        updatedPlotDice.stream().skip(1).forEach(integer -> fastRollResult[0] = (FastRollResult) fastRollResult[0].addRegularDice(integer));
        return fastRollResult[0];
    }

    /**
     * Adds a new kept die to the result
     *
     * @param diceValue The value of the kept die
     * @return A deep copy of FastRollResult with the kept die added and an updated doom counter if the value of the die is 1
     */
    @Override
    public FastRollResult addKeptDice(int diceValue) {
        Multiset<Integer> updatedDice = copyDiceList(keptDice);
        updatedDice.add(diceValue);

        return new FastRollResult(regularDice, plotDice, updatedDice, flatBonus, doomGenerated + (diceValue == 1 ? 1 : 0), diceKept);
    }

    /**
     * Add a flat bonus to the result
     *
     * @param bonus The value of the flat bonus
     * @return A deep copy of this FastRollResult with the flat bonuses being added to the existing flat bonus
     */
    @Override
    public FastRollResult addFlatBonus(int bonus) {
        return new FastRollResult(regularDice, plotDice, keptDice, flatBonus + bonus, doomGenerated, diceKept);
    }

    /**
     * Deep copies a TreeMultiset and sets it to use reverse order
     *
     * @param diceSet The set to copy
     * @return A deep copy of the set that sorts elements in reverse order
     */
    public Multiset<Integer> copyDiceList(Multiset<Integer> diceSet) {
        Multiset<Integer> updatedDice = TreeMultiset.create(Comparator.reverseOrder());
        updatedDice.addAll(diceSet);
        return updatedDice;
    }

    /**
     * Implementation of getTotal() to return the result of the getResults
     *
     * @return The result of the getResults
     */
    @Override
    public int getTotal() {
        return regularDice.stream().mapToInt(value -> value).sum() + plotDice.stream().mapToInt(value -> value).sum() + keptDice.stream().mapToInt(value -> value).sum() + flatBonus;
    }

    /**
     * @return The amount of doom generated by this getResults
     */
    public int getDoomGenerated() {
        return doomGenerated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FastRollResult that = (FastRollResult) o;
        return flatBonus == that.flatBonus &&
                doomGenerated == that.doomGenerated &&
                diceKept == that.diceKept &&
                Objects.equals(regularDice, that.regularDice) &&
                Objects.equals(plotDice, that.plotDice) &&
                Objects.equals(keptDice, that.keptDice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regularDice, plotDice, keptDice, flatBonus, doomGenerated, diceKept);
    }
}
