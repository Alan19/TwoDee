package statistics.opposed.strategy;

import statistics.StatisticsLogic;
import statistics2.StatisticsRollResult;

import java.util.Map;

public class SuccessStrategy implements OpposedStrategy {
    @Override
    public double getProbability(boolean attacker, Map<StatisticsRollResult, Long> attackerResults, Map<StatisticsRollResult, Long> defenderResults) {
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
