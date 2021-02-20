package dicerolling;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class that stores information about the results of a roll from a dice pool
 */
public class RollResult implements PoolResultWithEmbed {
    private final int diceKept;
    private final List<Integer> regularDice;
    private final List<Integer> plotDice;
    private final List<Integer> keptDice;
    private final List<Integer> flatBonus;

    public RollResult(int diceKept) {
        this.diceKept = diceKept;
        this.regularDice = new ArrayList<>();
        this.plotDice = new ArrayList<>();
        this.keptDice = new ArrayList<>();
        this.flatBonus = new ArrayList<>();
    }

    @Override
    public PoolResult addRegularDice(int diceValue) {
        regularDice.add(diceValue);
        return this;
    }

    @Override
    public PoolResult addPlotDice(int diceValue) {
        plotDice.add(diceValue);
        return this;
    }

    @Override
    public PoolResult addKeptDice(int diceValue) {
        keptDice.add(diceValue);
        return this;
    }

    @Override
    public PoolResult addFlatBonus(int bonus) {
        flatBonus.add(bonus);
        return this;
    }

    @Override
    public int getTotal() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        final int plotDie = sortedPlotDice.stream().limit(1).mapToInt(Integer::intValue).findFirst().orElse(0);
        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        return sortedRegularDice.stream().limit(diceKept).mapToInt(Integer::intValue).sum() + plotDie + keptDice.stream().mapToInt(Integer::intValue).sum() + flatBonus.stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public int getDoomGenerated() {
        return Math.toIntExact(regularDice.stream().filter(integer -> integer == 1).count() + plotDice.stream().filter(integer -> integer == 1).count() + keptDice.stream().filter(integer -> integer == 1).count());
    }

    @Override
    public List<Integer> getRegularDice() {
        return regularDice;
    }

    @Override
    public List<Integer> getPickedRegularDice() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        return sortedRegularDice.stream().limit(diceKept).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getDroppedRegularDice() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        return sortedRegularDice.stream().skip(diceKept).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getPlotDice() {
        return plotDice;
    }

    @Override
    public int getPickedPlotDie() {
        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        return sortedPlotDice.stream().limit(1).mapToInt(Integer::intValue).findFirst().orElse(0);
    }

    @Override
    public List<Integer> getDegradedPlotDice() {
        Multiset<Integer> sortedRegularDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedRegularDice.addAll(regularDice);

        Multiset<Integer> sortedPlotDice = TreeMultiset.create(Comparator.reverseOrder());
        sortedPlotDice.addAll(plotDice);

        sortedRegularDice.addAll(sortedPlotDice.stream().skip(1).collect(Collectors.toList()));

        return sortedPlotDice.stream().skip(1).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getAllPickedDice() {
        final ArrayList<Integer> picked = new ArrayList<>(getPickedRegularDice());
        picked.add(getPickedPlotDie());
        picked.addAll(getKeptDice());
        return picked;
    }

    @Override
    public List<Integer> getAllDroppedDice() {
        final ArrayList<Integer> dropped = new ArrayList<>(getDroppedRegularDice());
        dropped.addAll(getDegradedPlotDice());
        return dropped;
    }

    @Override
    public List<Integer> getKeptDice() {
        return keptDice;
    }

    @Override
    public List<Integer> getFlatBonuses() {
        return flatBonus;
    }
}
