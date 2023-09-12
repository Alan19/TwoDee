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
            Map<Integer, Long> defenderRollStatistics = StatisticsLogic.getRollResultToOccurancesMap(defenderResults);
            return (double) StatisticsLogic.getRollResultToOccurancesMap(attackerResults)
                    .entrySet()
                    .stream()
                    .map(attackerRollEntry -> defenderRollStatistics.entrySet().stream()
                            .filter(defenderRollEntry -> defenderRollEntry.getKey() >= attackerRollEntry.getKey() + 7)
                            .map(Map.Entry::getValue)
                            .map(defenderRollOccurrences -> attackerRollEntry.getValue() * defenderRollOccurrences)
                            .mapToLong(value -> value).sum())
                    .mapToLong(value -> value)
                    .sum() / totalSampleSpace;
        } else {
            Map<Integer, Long> attackerRollStatistics = StatisticsLogic.getRollResultToOccurancesMap(attackerResults);
            return (double) StatisticsLogic.getRollResultToOccurancesMap(defenderResults)
                    .entrySet()
                    .stream()
                    .map(defenderRollEntry -> attackerRollStatistics.entrySet().stream()
                            .filter(attackerRollEntry -> attackerRollEntry.getKey() >= defenderRollEntry.getKey() + 7)
                            .map(Map.Entry::getValue)
                            .map(attackerRollOccurrences -> defenderRollEntry.getValue() * attackerRollOccurrences)
                            .mapToLong(value -> value).sum())
                    .mapToLong(value -> value)
                    .sum() / totalSampleSpace;
        }

    }
}
