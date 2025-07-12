package statistics.opposed.strategy;

import com.google.common.collect.Range;
import org.apache.commons.lang3.tuple.Pair;
import rolling.BuildablePoolResult;
import statistics.StatisticsLogic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuccessStrategy implements OpposedStrategy {
    public static final Pair<Integer, Range<Integer>> noneRange = Pair.of(0, Range.lessThan(3));
    public static final Pair<Integer, Range<Integer>> easyRange = Pair.of(1, Range.closedOpen(3, 7));
    public static final Pair<Integer, Range<Integer>> averageRange = Pair.of(2, Range.closedOpen(7, 11));
    public static final Pair<Integer, Range<Integer>> hardRange = Pair.of(3, Range.closedOpen(11, 15));
    public static final Pair<Integer, Range<Integer>> formidableRange = Pair.of(4, Range.closedOpen(15, 19));
    public static final Pair<Integer, Range<Integer>> heroicRange = Pair.of(5, Range.closedOpen(19, 23));
    public static final Pair<Integer, Range<Integer>> incredibleRange = Pair.of(6, Range.closedOpen(23, 27));
    public static final Pair<Integer, Range<Integer>> ridiculousRange = Pair.of(7, Range.closedOpen(27, 31));
    public static final Pair<Integer, Range<Integer>> impossibleRange = Pair.of(8, Range.atLeast(31));
    public static final List<Pair<Integer, Range<Integer>>> difficultyRanges = List.of(impossibleRange, ridiculousRange, incredibleRange, heroicRange, formidableRange, hardRange, averageRange, easyRange, noneRange);


    @Override
    public double getProbability(boolean attacker, HashMap<BuildablePoolResult, Long> attackerResults, HashMap<BuildablePoolResult, Long> defenderResults) {
        long totalSampleSpace = getTotalSampleSpace(attackerResults, defenderResults);
        Map<Integer, Long> defenderRollStatistics = StatisticsLogic.getDifficultyToCountMap(defenderResults);
        Map<Integer, Long> attackerRollStatistics = StatisticsLogic.getDifficultyToCountMap(attackerResults);
        long successCount = attackerRollStatistics
                .entrySet()
                .stream()
                .map(attackerRollTierEntry -> defenderRollStatistics.entrySet().stream()
                        .filter(defenderRollEntry -> attackerRollTierEntry.getKey() > 0 && attackerRollTierEntry.getKey() >= defenderRollEntry.getKey())
                        .map(Map.Entry::getValue)
                        .map(defenderRollTierOccurrences -> attackerRollTierEntry.getValue() * defenderRollTierOccurrences)
                        .mapToLong(value -> value).sum())
                .mapToLong(value -> value)
                .sum();

        if (attacker) {
            return (double) successCount / totalSampleSpace;
        }
        return 1 - (double) successCount / totalSampleSpace;
    }
}
