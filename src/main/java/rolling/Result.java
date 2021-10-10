package rolling;

import java.util.ArrayList;
import java.util.List;

public class Result {
    private final List<Integer> regularDice;
    private final List<Integer> plotDice;
    private final List<Integer> keptDice;
    private final List<Integer> chaosDice;
    private final List<Integer> flatBonuses;
    private final int kept;

    public Result(List<Roll> rolls, List<Integer> flatBonuses, int kept, int plotPointsUsed) {
        this.regularDice = new ArrayList<>();
        this.plotDice = new ArrayList<>();
        this.keptDice = new ArrayList<>();
        this.chaosDice = new ArrayList<>();
        this.flatBonuses = flatBonuses;
        this.kept = kept;
        rolls.forEach(this::acceptDice);
    }


    private void acceptDice(Roll roll) {
        // TODO Add logic for plot dice
        switch (roll.getType()) {
            case "pd":
            case "ed":
                plotDice.add(roll.getValue());
                break;
            case "cd":
                chaosDice.add(roll.getValue());
                break;
            case "kd":
                keptDice.add(roll.getValue());
                break;
            default:
                regularDice.add(roll.getValue());
                break;
        }
    }

    public int getTotal() {
        return 0;
    }

    public List<Integer> getDroppedDice() {
        return null;
    }

    public List<Integer> getFlatBonuses() {
        return null;
    }

    public List<Integer> getPickedDice() {
        return null;
    }

    public List<Integer> getKeptDice() {
        return null;
    }

    public List<Integer> getPlotDice() {
        return null;
    }

    public List<Integer> getRegularAndChaosDice() {
        return null;
    }

    public int getOpportunities() {
        return 0;
    }

    public int getPlotPointsSpent() {
        return 0;
    }
}
