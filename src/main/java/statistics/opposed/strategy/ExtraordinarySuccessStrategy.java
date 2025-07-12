package statistics.opposed.strategy;

import com.google.common.collect.Range;
import org.apache.commons.lang3.tuple.Pair;
import rolling.BuildablePoolResult;
import statistics.StatisticsLogic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtraordinarySuccessStrategy implements OpposedStrategy {
    private static final Pair<Integer, Range<Integer>> noneRange = Pair.of(0, Range.lessThan(10));
    private static final Pair<Integer, Range<Integer>> easyRange = Pair.of(1, Range.closedOpen(10, 14));
    private static final Pair<Integer, Range<Integer>> averageRange = Pair.of(2, Range.closedOpen(14, 18));
    private static final Pair<Integer, Range<Integer>> hardRange = Pair.of(3, Range.closedOpen(18, 22));
    private static final Pair<Integer, Range<Integer>> formidableRange = Pair.of(4, Range.closedOpen(22, 26));
    private static final Pair<Integer, Range<Integer>> heroicRange = Pair.of(5, Range.closedOpen(26, 30));
    private static final Pair<Integer, Range<Integer>> incredibleRange = Pair.of(6, Range.closedOpen(30, 34));
    private static final Pair<Integer, Range<Integer>> ridiculousRange = Pair.of(7, Range.closedOpen(34, 38));
    private static final Pair<Integer, Range<Integer>> impossibleRange = Pair.of(8, Range.atLeast(38));
    public static final List<Pair<Integer, Range<Integer>>> extraordinaryDifficultyRanges = Arrays.asList(impossibleRange, ridiculousRange, incredibleRange, heroicRange, formidableRange, hardRange, averageRange, easyRange, noneRange);

    @Override
    public double getProbability(boolean attacker, HashMap<BuildablePoolResult, Long> attackerResults, HashMap<BuildablePoolResult, Long> defenderResults) {
        long totalSampleSpace = getTotalSampleSpace(attackerResults, defenderResults);
        if (attacker) {
            Map<Integer, Long> defenderRollStatistics = StatisticsLogic.getDifficultyToCountMap(defenderResults);
            return (double) StatisticsLogic.getExtraordinaryDifficultyToCountMap(attackerResults)
                    .entrySet()
                    .stream()
                    .map(attackerRollEntry -> defenderRollStatistics.entrySet().stream()
                            .filter(defenderRollEntry -> attackerRollEntry.getKey() > 0 && defenderRollEntry.getKey() <= attackerRollEntry.getKey())
                            .map(Map.Entry::getValue)
                            .map(defenderRollOccurrences -> attackerRollEntry.getValue() * defenderRollOccurrences)
                            .mapToLong(value -> value).sum())
                    .mapToLong(value -> value)
                    .sum() / totalSampleSpace;
        } else {
            Map<Integer, Long> attackerRollStatistics = StatisticsLogic.getDifficultyToCountMap(attackerResults);
            return (double) StatisticsLogic.getExtraordinaryDifficultyToCountMap(defenderResults)
                    .entrySet()
                    .stream()
                    .map(defenderRollEntry -> attackerRollStatistics.entrySet().stream()
                            .filter(attackerRollEntry -> defenderRollEntry.getKey() > 0 && attackerRollEntry.getKey() <= defenderRollEntry.getKey())
                            .map(Map.Entry::getValue)
                            .map(attackerRollOccurrences -> defenderRollEntry.getValue() * attackerRollOccurrences)
                            .mapToLong(value -> value).sum())
                    .mapToLong(value -> value)
                    .sum() / totalSampleSpace;
        }
    }
}
