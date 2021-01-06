package dicerolling;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FastRollResult implements PoolResult {
    private final Multiset<Integer> regularDice;
    private final Multiset<Integer> plotDice;
    private final Multiset<Integer> keptDice;
    private final int flatBonus;
    private final int doomGenerated;
    private final int diceKept;

    public FastRollResult() {
        regularDice = TreeMultiset.create(Comparator.reverseOrder());
        plotDice = TreeMultiset.create(Comparator.reverseOrder());
        keptDice = TreeMultiset.create(Comparator.reverseOrder());
        flatBonus = 0;
        doomGenerated = 0;
        diceKept = 2;
    }

    private FastRollResult(Multiset<Integer> regularDice, Multiset<Integer> plotDice, Multiset<Integer> keptDice, int flatBonus, int doomGenerated, int diceKept) {
        this.regularDice = regularDice;
        this.plotDice = plotDice;
        this.keptDice = keptDice;
        this.flatBonus = flatBonus;
        this.doomGenerated = doomGenerated;
        this.diceKept = diceKept;
    }

    /**
     * Adds a new dice to the dice pool and drop the ones that are not kept
     *
     * @param diceValue The dice value of the new dice to add
     * @return A copy of this object with the new dice added
     */
    public FastRollResult addRegularDice(int diceValue) {
        Multiset<Integer> updatedDice = copyDiceList(regularDice);
        updatedDice.add(diceValue);
        final List<Integer> kept = updatedDice.stream().limit(diceKept).collect(Collectors.toList());
        final Multiset<Integer> newDiceList = TreeMultiset.create(Comparator.reverseOrder());
        newDiceList.addAll(kept);
        return new FastRollResult(newDiceList, plotDice, keptDice, flatBonus, doomGenerated + diceValue == 1 ? 1 : 0, diceKept);
    }

    public FastRollResult addPlotDice(int diceValue) {
        Multiset<Integer> updatedPlotDice = copyDiceList(plotDice);
        updatedPlotDice.add(diceValue);

        final List<Integer> kept = updatedPlotDice.stream().limit(1).collect(Collectors.toList());
        final Multiset<Integer> newPlotDiceList = TreeMultiset.create(Comparator.reverseOrder());
        newPlotDiceList.addAll(kept);

        final FastRollResult[] fastRollResult = {new FastRollResult(regularDice, newPlotDiceList, keptDice, flatBonus, doomGenerated, diceKept)};
        updatedPlotDice.stream().skip(1).forEach(integer -> fastRollResult[0] = fastRollResult[0].addRegularDice(integer));
        return fastRollResult[0];
    }

    public FastRollResult addKeptDice(int diceValue) {
        Multiset<Integer> updatedDice = copyDiceList(keptDice);
        updatedDice.add(diceValue);

        return new FastRollResult(regularDice, plotDice, updatedDice, flatBonus, doomGenerated + diceValue == 1 ? 1 : 0, diceKept);
    }

    public FastRollResult addFlatBonus(int bonus) {
        return new FastRollResult(regularDice, plotDice, keptDice, flatBonus + bonus, doomGenerated, diceKept);
    }

    private Multiset<Integer> copyDiceList(Multiset<Integer> keptDice) {
        Multiset<Integer> updatedDice = TreeMultiset.create(Comparator.reverseOrder());
        updatedDice.addAll(keptDice);
        return updatedDice;
    }

    @Override
    public int getTotal() {
        return regularDice.stream().mapToInt(value -> value).sum() + plotDice.stream().mapToInt(value -> value).sum() + keptDice.stream().mapToInt(value -> value).sum() + flatBonus;
    }

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
