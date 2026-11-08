package statistics2;

import com.google.common.math.LongMath;
import rolling.DiceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DicePoolStatistics {
    private Map<StatisticsRollResult, Long> results = new HashMap<>();

    public DicePoolStatistics(DicePool dicePool) {
        Map<DiceType, List<Integer>> dicePoolMap = dicePool.getDicePoolMap();

        StatisticsRollResult seed = new StatisticsRollResult().setFlatBonus(dicePool.getFlatBonusSum());
        results.put(seed, 1L);
        dicePoolMap.forEach((type, dieFacetsList) -> {
            for (Integer dieFacets : dieFacetsList) {
                List<Integer> sampleSpace = type.getSampleSpace(dieFacets);
                // Special handling for plot dice
                if ((type == DiceType.PLOT_DIE || type == DiceType.ENHANCED_DIE) && dieFacetsList.size() == 1) {
                    sampleSpace = sampleSpace.stream().map(result -> Math.max(dieFacets / 2, result)).toList();
                }
                results = iterateResults(sampleSpace, type);
            }
        });
        Map<Integer, Long> resultToOccurrences = new TreeMap<>();
        results.forEach((statisticsRollResult, aLong) -> resultToOccurrences.merge(statisticsRollResult.getTotal(), aLong, LongMath::checkedAdd));
        resultToOccurrences.forEach((integer, aLong) -> System.out.println(integer + ": " + String.format("%.5f", (double) aLong / results.values().stream().mapToLong(value -> value).sum() * 100) + "%"));
    }

    private Map<StatisticsRollResult, Long> iterateResults(List<Integer> sampleSpace, DiceType type) {
        Map<StatisticsRollResult, Long> tempResults = new HashMap<>();
        results.forEach((key, value) -> {
            for (int i : sampleSpace) {
                StatisticsRollResult updatedResult = key.addDie(i, type);
                tempResults.merge(updatedResult, value, LongMath::checkedAdd);
            }
        });
        return tempResults;
    }

    public Map<StatisticsRollResult, Long> getResults() {
        return results;
    }
}
