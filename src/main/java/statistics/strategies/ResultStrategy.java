package statistics.strategies;

import statistics.opposed.strategy.OpposedStrategy;
import statistics.StatisticsRollResult;
import util.EmbedField;

import java.util.List;
import java.util.Map;

/**
 * Each visitor can return an object that stores a Embed field title and description after all objects have been visited
 */
public interface ResultStrategy {
    List<EmbedField> executeStrategy(Map<StatisticsRollResult, Long> resultMap, long totalCombinations);

    default String generatePercentage(long numberOfOccurrences, long numberOfResults) {
        final double resultProbability = (double) numberOfOccurrences / numberOfResults;
        return OpposedStrategy.generatePercentage(resultProbability);
    }
}
