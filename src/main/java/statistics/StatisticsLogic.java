package statistics;

import com.google.common.math.LongMath;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates statistics for a dice pool by calculating the sample set of all possible outcomes. Does this by using a Map of RollResults to number of occurrences. For every die in the pool, and for every facet of that die, map each RollResult in the map to a copy of that object with that die result added.
 */
public record StatisticsLogic(Try<Map<StatisticsRollResult, Long>> results) {

    public static Map<Integer, Long> getRollResultToOccurancesMap(Map<StatisticsRollResult, Long> results) {
        Map<Integer, Long> resultMap = new HashMap<>();
        results.forEach((statisticsRollResult, aLong) -> resultMap.merge(statisticsRollResult.getTotal(), aLong, LongMath::checkedAdd));
        return resultMap;
    }

    /**
     * Generates a hashmap based on the result hashmap to get the number of occurrences for each potential doom point output on a roll
     * <p>
     * We use reduce instead of sum to catch long overflow
     *
     * @param results A hashmap containing the roll result mapped to the number of occurrences
     * @return A HashMap in the format of: number of doom generated to the number of occurrences
     */
    public static Map<Integer, Long> getRollToOpportunitiesMap(Map<StatisticsRollResult, Long> results) {
        Map<Integer, Long> resultMap = new HashMap<>();
        results.forEach((statisticsRollResult, aLong) -> resultMap.merge(statisticsRollResult.getDoomGenerated(), aLong, LongMath::checkedAdd));
        return resultMap;
    }

}
