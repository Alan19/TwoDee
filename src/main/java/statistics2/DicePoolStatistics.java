package statistics2;

import rolling.DiceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DicePoolStatistics {
    private Map<StatisticsRollResult, Long> results = new HashMap<>();

    public DicePoolStatistics(DicePool dicePool) {
        HashMap<DiceType, List<Integer>> dicePoolMap = dicePool.getDicePoolMap();

        StatisticsRollResult seed = new StatisticsRollResult().setFlatBonus(dicePool.getFlatBonus().stream().mapToInt(integer -> integer).sum());
        results.put(seed, 1L);
        dicePoolMap.forEach((type, dieFacetsList) -> {
            for (Integer dieFacets : dieFacetsList) {
                List<Integer> sampleSpace = type.getSampleSpace(dieFacets);
                // Special handling for plot dice
                // TODO Maybe move this into StatisticsRollResult.java?
                if ((type == DiceType.PLOT_DIE || type == DiceType.ENHANCED_DIE) && dieFacetsList.size() == 1) {
                    sampleSpace = sampleSpace.stream().map(result -> Math.max(dieFacets / 2, result)).toList();
                }
                results = iterateResults(sampleSpace, type);
            }
        });
        Map<Integer, Long> resultToOccurrences = new TreeMap<>();
        results.forEach((statisticsRollResult, aLong) -> resultToOccurrences.merge(statisticsRollResult.getTotal(), aLong, Long::sum));
        resultToOccurrences.forEach((integer, aLong) -> System.out.println(integer + ": " + String.format("%.5f", (double) aLong / results.values().stream().mapToLong(value -> value).sum() * 100) + "%"));
    }

    private Map<StatisticsRollResult, Long> iterateResults(List<Integer> sampleSpace, DiceType type) {
        Map<StatisticsRollResult, Long> tempResults = new HashMap<>();
        results.forEach((key, value) -> {
            for (int i : sampleSpace) {
                StatisticsRollResult updatedResult = key.addDie(i, type);
                boolean containsKey = tempResults.containsKey(updatedResult);
                tempResults.put(updatedResult, containsKey ? Long.valueOf(value + tempResults.get(updatedResult)) : value);
            }
        });
        return tempResults;
    }

    public Map<StatisticsRollResult, Long> getResults() {
        return results;
    }
}
