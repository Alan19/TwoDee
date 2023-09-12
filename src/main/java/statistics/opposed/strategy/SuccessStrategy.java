package statistics.opposed.strategy;

import rolling.BuildablePoolResult;
import statistics.StatisticsLogic;

import java.util.HashMap;
import java.util.Map;

public class SuccessStrategy implements OpposedStrategy {
    @Override
    public double getProbability(boolean attacker, HashMap<BuildablePoolResult, Long> attackerResults, HashMap<BuildablePoolResult, Long> defenderResults) {
        long totalSampleSpace = getTotalSampleSpace(attackerResults, defenderResults);
        Map<Integer, Long> defenderRollStatistics = StatisticsLogic.getRollResultToOccurancesMap(defenderResults);
        long successCount = StatisticsLogic.getRollResultToOccurancesMap(attackerResults)
                .entrySet()
                .stream()
                .map(attackerRollEntry -> defenderRollStatistics.entrySet().stream()
                        .filter(defenderRollEntry -> defenderRollEntry.getKey() <= attackerRollEntry.getKey())
                        .map(Map.Entry::getValue)
                        .map(defenderRollOccurences -> attackerRollEntry.getValue() * defenderRollOccurences)
                        .mapToLong(value -> value).sum())
                .mapToLong(value -> value)
                .sum();

        if (attacker) {
            return (double) successCount / totalSampleSpace;
        }
        return 1 - (double) successCount / totalSampleSpace;
    }
}
