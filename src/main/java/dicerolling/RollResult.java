package dicerolling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A builder version of RollResult
 */
public class RollResult {
    private List<Integer> dice;
    private List<Integer> plotDice;
    private List<Integer> keptDice;
    private List<Integer> flatBonus;
    private int doom;
    private int keepHowMany;
    private boolean cleanUnnecessary;

    public RollResult(int keep, boolean statisticsMode) {
        dice = new ArrayList<>();
        plotDice = new ArrayList<>();
        keptDice = new ArrayList<>();
        flatBonus = new ArrayList<>();
        doom = 0;
        keepHowMany = keep;
        cleanUnnecessary = statisticsMode;
    }

    private RollResult(List<Integer> dice, List<Integer> plotDice, List<Integer> keptDice, List<Integer> flatBonus, int keepHowMany, int doom, boolean cleanUnnecessary) {
        this.dice = dice;
        this.plotDice = plotDice;
        this.keptDice = keptDice;
        this.flatBonus = flatBonus;
        this.keepHowMany = keepHowMany;
        this.doom = doom;
        this.cleanUnnecessary = cleanUnnecessary;
    }

    public List<Integer> getDice() {
        return dice;
    }

    public List<Integer> getPlotDice() {
        return plotDice;
    }

    public List<Integer> getKeptDice() {
        return keptDice;
    }

    public List<Integer> getFlatBonus() {
        return flatBonus;
    }

    public RollResult addResult(int result) {
        dice.add(result);
        if (cleanUnnecessary) {
            dice.sort(Comparator.reverseOrder());
            dice = dice.subList(0, Math.min(keepHowMany, dice.size()));
        }
        if (result == 1) {
            doom++;
        }
        return this;
    }

    public RollResult addPlotResult(int i) {
        plotDice.add(i);
        if (cleanUnnecessary) {
            plotDice.sort(Comparator.reverseOrder());
        }
        return this;
    }

    public RollResult addKeptResult(int i) {
        keptDice.add(i);
        if (cleanUnnecessary) {
            keptDice.sort(Comparator.reverseOrder());
        }
        if (i == 1) {
            doom++;
        }
        return this;
    }

    public RollResult addFlatBonus(int i) {
        flatBonus.add(i);
        flatBonus.sort(Comparator.reverseOrder());
        if (i == 1) {
            doom++;
        }
        return this;
    }

    public int getTotal() {
        int sum = 0;
        ArrayList<Integer> diceCopy = new ArrayList<>(dice);
        diceCopy.sort(Comparator.reverseOrder());
        sum += diceCopy.stream().limit(Math.min(keepHowMany, dice.size())).mapToInt(sortedResult -> sortedResult).sum();
        sum += plotDice.stream().mapToInt(value -> value).sum();
        sum += keptDice.stream().mapToInt(kd -> kd).sum();
        sum += flatBonus.stream().mapToInt(f -> f).sum();
        return sum;
    }

    public RollResult copy() {
        return new RollResult(new ArrayList<>(dice), new ArrayList<>(plotDice), new ArrayList<>(keptDice), new ArrayList<>(flatBonus), keepHowMany, doom, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RollResult that = (RollResult) o;
        return
                doom == that.doom &&
                        keepHowMany == that.keepHowMany &&
                        Objects.equals(dice, that.dice) &&
                        Objects.equals(plotDice, that.plotDice) &&
                        Objects.equals(keptDice, that.keptDice) &&
                        Objects.equals(flatBonus, that.flatBonus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dice, plotDice, keptDice, flatBonus, keepHowMany);
    }

    public int getDoom() {
        return doom;
    }

    public List<Integer> getDropped() {
        ArrayList<Integer> sortedResults = new ArrayList<>(dice);
        sortedResults.sort(Comparator.reverseOrder());
        return IntStream.range(Math.min(sortedResults.size(), keepHowMany), sortedResults.size()).mapToObj(sortedResults::get).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Integer> getPickedDice() {
        ArrayList<Integer> sortedResults = new ArrayList<>(dice);
        sortedResults.sort(Comparator.reverseOrder());
        return sortedResults.subList(0, Math.min(keepHowMany, dice.size()));

    }
}
