package rolling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Result {
    private final List<Integer> regularDice;
    private final List<RolledDie> plotDice;
    private final List<Integer> keptDice;
    private final List<Integer> chaosDice;
    private final List<Integer> flatBonuses;
    private final int kept;
    private final int plotDiceCost;

    public Result(List<RolledDie> rolls, List<Integer> flatBonuses, int kept) {
        this.regularDice = new ArrayList<>();
        this.plotDice = new ArrayList<>();
        this.keptDice = new ArrayList<>();
        this.chaosDice = new ArrayList<>();
        this.flatBonuses = flatBonuses;
        this.kept = kept;
        plotDiceCost = rolls.stream().filter(roll -> roll.getType().equals("pd")).mapToInt(RolledDie::getEnhancedValue).sum();
        rolls.forEach(this::acceptDice);
    }


    private void acceptDice(RolledDie roll) {
        switch (roll.getType()) {
            case "pd", "ed" -> plotDice.add(roll);
            case "cd" -> chaosDice.add(roll.getValue());
            case "kd" -> keptDice.add(roll.getValue());
            default -> regularDice.add(roll.getValue());
        }
    }

    public int getTotal() {
        return getPickedDice().stream().mapToInt(Integer::intValue).sum() + getFlatBonuses().stream().mapToInt(Integer::intValue).sum();
    }

    public List<Integer> getDroppedDice() {
        final Stream<Integer> droppedRegularDice = Stream.concat(regularDice.stream(), getPlotDice().stream().skip(1)).sorted(Comparator.reverseOrder()).skip(kept);
        return Stream.concat(getChaosDiceStream().sorted(Comparator.naturalOrder()).skip(2), droppedRegularDice).collect(Collectors.toList());
    }

    public List<Integer> getFlatBonuses() {
        return flatBonuses;
    }

    public List<Integer> getPickedDice() {
        Stream<Integer> pickedRegularDice = Stream.concat(regularDice.stream(), getPlotDice().stream().sorted(Comparator.reverseOrder()).skip(1)).sorted(Comparator.reverseOrder()).limit(kept);
        pickedRegularDice = Stream.concat(pickedRegularDice, getPlotDice().stream().sorted(Comparator.reverseOrder()).limit(1));
        final Stream<Integer> pickedChaosDiceAndKeptDice = Stream.concat(getChaosDiceStream().sorted(Comparator.naturalOrder()).limit(2), keptDice.stream());
        return Stream.concat(pickedRegularDice, pickedChaosDiceAndKeptDice).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    public List<Integer> getKeptDice() {
        return keptDice;
    }

    public List<Integer> getPlotDice() {
        return plotDice.stream()
                .map(integer -> (plotDice.size() <= 1) ? Math.max(integer.getValue(), integer.getEnhancedValue()) : integer.getValue())
                .collect(Collectors.toList());
    }

    private Stream<Integer> getChaosDiceStream() {
        return chaosDice.stream().map(integer -> integer * -1);
    }

    public List<Integer> getRegularAndChaosDice() {
        return Stream.concat(regularDice.stream(), getChaosDiceStream()).collect(Collectors.toList());
    }

    public int getOpportunities() {
        Stream<Integer> streamsToCheck = Stream.concat(regularDice.stream(), keptDice.stream());
        if (plotDice.size() > 1) {
            streamsToCheck = Stream.concat(streamsToCheck, plotDice.stream().skip(1).map(RolledDie::getValue));
        }
        return Math.toIntExact(streamsToCheck.filter(integer -> integer == 1).count());
    }

    public int getPlotDiceCost() {
        return plotDiceCost;
    }
}
