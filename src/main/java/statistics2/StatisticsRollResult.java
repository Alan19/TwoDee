package statistics2;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import rolling.DiceType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatisticsRollResult {
    private final Multiset<Integer> keptDice;
    private List<Integer> normalDice;
    private int chosenPlotDie;
    private List<Integer> chaosDice;
    private int doomGenerated;
    private int flatBonus;

    public StatisticsRollResult(List<Integer> normalDice, int plotDice, Multiset<Integer> keptDice, List<Integer> chaosDice, int doomGenerated, int flatBonus) {
        this.keptDice = keptDice;
        this.normalDice = normalDice;
        this.chosenPlotDie = plotDice;
        this.chaosDice = chaosDice;
        this.doomGenerated = doomGenerated;
        this.flatBonus = flatBonus;
    }

    public StatisticsRollResult() {
        keptDice = HashMultiset.create();
        normalDice = new ArrayList<>();
        chaosDice = new ArrayList<>();
        flatBonus = 0;
        doomGenerated = 0;
    }

    public StatisticsRollResult copy() {
        return new StatisticsRollResult(new ArrayList<>(normalDice), chosenPlotDie, HashMultiset.create(keptDice), new ArrayList<>(chaosDice), doomGenerated, flatBonus);
    }

    public StatisticsRollResult addDie(int die, DiceType type) {
        return switch (type) {
            case REGULAR, ASSURED_DIE -> addNormalDie(die);
            case PLOT_DIE, ENHANCED_DIE -> addPlotDie(die);
            case KEPT_DIE -> addKeptDie(die);
            case CHAOS_DIE -> addChaosDie(die);
        };
    }

    private StatisticsRollResult addChaosDie(int die) {
        StatisticsRollResult copy = copy();
        copy.chaosDice.add(die);
        copy.chaosDice.sort(Comparator.naturalOrder());
        copy.chaosDice = copy.chaosDice.subList(0, Math.min(2, copy.chaosDice.size()));
        return copy;
    }

    private StatisticsRollResult addNormalDie(int die) {
        StatisticsRollResult copy = copy();
        copy.normalDice.add(die);
        copy.normalDice.sort(Comparator.reverseOrder());
        copy.normalDice = copy.normalDice.subList(0, Math.min(2, copy.normalDice.size()));
        if (die == 1) {
            copy.doomGenerated++;
        }
        return copy;
    }

    // Adds a plot die to the pool. We are accepting the result from the calling function since the calling function knows if a minimum value should be set.
    // If there's already a plot die, then move the lower of the two to a normal die
    private StatisticsRollResult addPlotDie(int die) {
        StatisticsRollResult copy = copy();
        if (copy.chosenPlotDie == 0) {
            copy.chosenPlotDie = die;
        }
        else {
            copy.chosenPlotDie = Math.max(die, chosenPlotDie);
            copy.normalDice.add(Math.min(die, chosenPlotDie));
            copy.normalDice.sort(Comparator.reverseOrder());
            copy.normalDice = copy.normalDice.subList(0, Math.min(2, copy.normalDice.size()));
        }
        if (die == 1) {
            copy.doomGenerated++;
        }
        return copy;
    }

    // All kept dice are kept, but we sort them for equality purposes
    private StatisticsRollResult addKeptDie(int die) {
        StatisticsRollResult copy = copy();
        copy.keptDice.add(die);
        if (die == 1) {
            copy.doomGenerated++;
        }
        return copy;
    }

    // Same logic as plot die, the calling function will set the flat bonus since it's more efficient to sum it there
    public StatisticsRollResult setFlatBonus(int flatBonus) {
        StatisticsRollResult copy = copy();
        copy.flatBonus = flatBonus;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StatisticsRollResult that = (StatisticsRollResult) o;
        return doomGenerated == that.doomGenerated && flatBonus == that.flatBonus && Objects.equal(keptDice, that.keptDice) && Objects.equal(normalDice, that.normalDice) && Objects.equal(chosenPlotDie, that.chosenPlotDie) && Objects.equal(chaosDice, that.chaosDice);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keptDice, normalDice, chosenPlotDie, chaosDice, doomGenerated, flatBonus);
    }

    public int getTotal() {
        return flatBonus + chosenPlotDie + keptDice.stream().mapToInt(value -> value).sum() + normalDice.stream().mapToInt(value -> value).sum() + chaosDice.stream().mapToInt(value -> value).sum();
    }

    public int getDoomGenerated() {
        return doomGenerated;
    }
}
