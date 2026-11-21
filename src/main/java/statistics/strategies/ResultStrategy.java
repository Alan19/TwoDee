package statistics.strategies;

import statistics.StatisticsRollResult;
import util.EmbedField;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Each visitor can return an object that stores a Embed field title and description after all objects have been visited
 */
public interface ResultStrategy {
    List<EmbedField> executeStrategy(Map<StatisticsRollResult, Long> resultMap, long totalCombinations);

    default String generatePercentage(long numberOfOccurrences, long numberOfResults) {
        final double resultProbability = (double) numberOfOccurrences / numberOfResults;
        return generatePercentage(resultProbability);
    }

    static String generatePercentage(double resultProbability) {
        final double resultPercentage = resultProbability * 100;
        String formatString = "0.#####" + (resultPercentage < 0.00001 && resultPercentage > 0 ? "E00" : "") + "%";
        DecimalFormat df = new DecimalFormat(formatString);
        return df.format(resultProbability);
    }

}
