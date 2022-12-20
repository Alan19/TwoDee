package statistics.strategies;

import rolling.BuildablePoolResult;
import util.EmbedField;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

/**
 * Each visitor can return an object that stores a Embed field title and description after all objects have been visited
 */
public interface ResultStrategy {
    List<EmbedField> executeStrategy(HashMap<BuildablePoolResult, Long> resultMap, long totalCombinations);

    default String generatePercentage(long numberOfOccurrences, long numberOfResults) {
        final double resultProbability = (double) numberOfOccurrences / numberOfResults;
        final double resultPercentage = resultProbability * 100;
        String formatString = "0.#####" + (resultPercentage < 0.00001 && resultPercentage > 0 ? "E00" : "") + "%";
        DecimalFormat df = new DecimalFormat(formatString);
        return df.format(resultProbability);
    }
}
