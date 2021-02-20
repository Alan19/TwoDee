package dicerolling;

import statistics.resultvisitors.DifficultyVisitor;

import java.util.List;
import java.util.Map;

public interface PoolResultWithEmbed extends PoolResult {
    List<Integer> getRegularDice();

    List<Integer> getPickedRegularDice();

    List<Integer> getDroppedRegularDice();

    List<Integer> getPlotDice();

    int getPickedPlotDie();

    List<Integer> getDegradedPlotDice();

    List<Integer> getAllPickedDice();

    List<Integer> getAllDroppedDice();

    List<Integer> getKeptDice();

    List<Integer> getFlatBonuses();

    default String getTierHit() {
        DifficultyVisitor difficultyVisitor = new DifficultyVisitor();
        int total = getTotal();
        if (total < 3) {
            return "None";
        }
        StringBuilder output = new StringBuilder();
        for (Map.Entry<Integer, String> diffEntry : difficultyVisitor.getDifficultyMap().entrySet()) {
            if (difficultyVisitor.generateStageDifficulty(diffEntry.getKey() + 1) > total) {
                output.append(diffEntry.getValue());
                break;
            }
        }
        if (total < 10) {
            return String.valueOf(output);
        }
        for (Map.Entry<Integer, String> diffEntry : difficultyVisitor.getDifficultyMap().entrySet()) {
            if (difficultyVisitor.generateStageExtraordinaryDifficulty(diffEntry.getKey() + 1) > total) {
                output.append(", Extraordinary ").append(diffEntry.getValue());
                break;
            }
        }
        return String.valueOf(output);

    }
}
