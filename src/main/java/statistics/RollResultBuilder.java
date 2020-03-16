package statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A builder version of RollResult
 */
public class RollResultBuilder {
    private List<Integer> dice;
    private List<Integer> plotDice;
    private List<Integer> keptDice;
    private List<Integer> flatBonus;
    private int keepHowMany;

    public RollResultBuilder(int keep) {
        dice = new ArrayList<>();
        plotDice = new ArrayList<>();
        keptDice = new ArrayList<>();
        flatBonus = new ArrayList<>();
        keepHowMany = keep;
    }

    public RollResultBuilder(List<Integer> dice, List<Integer> plotDice, List<Integer> keptDice, List<Integer> flatBonus, int keepHowMany) {
        this.dice = dice;
        this.plotDice = plotDice;
        this.keptDice = keptDice;
        this.flatBonus = flatBonus;
        this.keepHowMany = keepHowMany;

    }

    public RollResultBuilder addResult(int result) {
        dice.add(result);
        dice.sort(Comparator.reverseOrder());
        dice = new ArrayList<>(dice.subList(0, Math.min(keepHowMany, dice.size())));
        return this;
    }

    public RollResultBuilder addPlotResult(int i) {
        plotDice.add(i);
        plotDice.sort(Comparator.reverseOrder());
        return this;
    }

    public RollResultBuilder addKeptResult(int i) {
        keptDice.add(i);
        keptDice.sort(Comparator.reverseOrder());
        return this;
    }

    public RollResultBuilder addFlatBonus(int i) {
        flatBonus.add(i);
        flatBonus.sort(Comparator.reverseOrder());
        return this;
    }

    public int getResult() {
        int sum = 0;
        sum += dice.stream().limit(Math.min(keepHowMany, dice.size())).mapToInt(sortedResult -> sortedResult).sum();
        sum += plotDice.stream().mapToInt(value -> value).sum();
        sum += keptDice.stream().mapToInt(kd -> kd).sum();
        sum += flatBonus.stream().mapToInt(f -> f).sum();
        return sum;
    }

    public RollResultBuilder copy() {
        return new RollResultBuilder(new ArrayList<>(dice), new ArrayList<>(plotDice), new ArrayList<>(keptDice), new ArrayList<>(flatBonus), keepHowMany);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RollResultBuilder that = (RollResultBuilder) o;
        return keepHowMany == that.keepHowMany &&
                Objects.equals(dice, that.dice) &&
                Objects.equals(plotDice, that.plotDice) &&
                Objects.equals(keptDice, that.keptDice) &&
                Objects.equals(flatBonus, that.flatBonus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dice, plotDice, keptDice, flatBonus, keepHowMany);
    }
}
