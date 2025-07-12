package statistics.opposed.strategy;

import rolling.BuildablePoolResult;
import statistics.StatisticsLogic;

import java.util.HashMap;
import java.util.Map;

public class ExtraordinaryFailureStrategy implements OpposedStrategy {
    @Override
    public double getProbability(boolean attacker, HashMap<BuildablePoolResult, Long> attackerResults, HashMap<BuildablePoolResult, Long> defenderResults) {
        long totalSampleSpace = getTotalSampleSpace(attackerResults, defenderResults);
        if (attacker) {
            Map<Integer, Long> defenderRollStatistics = StatisticsLogic.getExtraordinaryDifficultyToCountMap(defenderResults);
            return (double) StatisticsLogic.getDifficultyToCountMap(attackerResults)
                    .entrySet()
                    .stream()
                    .map(attackerRollEntry -> defenderRollStatistics.entrySet().stream()
                            .filter(defenderRollEntry -> defenderRollEntry.getKey() > 0 && defenderRollEntry.getKey() >= attackerRollEntry.getKey())
                            .map(Map.Entry::getValue)
                            .map(defenderRollOccurrences -> attackerRollEntry.getValue() * defenderRollOccurrences)
                            .mapToLong(value -> value).sum())
                    .mapToLong(value -> value)
                    .sum() / totalSampleSpace;
        } else {
            Map<Integer, Long> attackerRollStatistics = StatisticsLogic.getExtraordinaryDifficultyToCountMap(attackerResults);
            return (double) StatisticsLogic.getDifficultyToCountMap(defenderResults)
                    .entrySet()
                    .stream()
                    .map(defenderRollEntry -> attackerRollStatistics.entrySet().stream()
                            .filter(attackerRollEntry -> attackerRollEntry.getKey() > 0 && attackerRollEntry.getKey() >= defenderRollEntry.getKey())
                            .map(Map.Entry::getValue)
                            .map(attackerRollOccurrences -> defenderRollEntry.getValue() * attackerRollOccurrences)
                            .mapToLong(value -> value).sum())
                    .mapToLong(value -> value)
                    .sum() / totalSampleSpace;
        }
    }
}
