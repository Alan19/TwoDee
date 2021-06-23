package statistics.resultvisitors;

import util.EmbedField;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Each visitor can return an object that stores a Embed field title and description after all objects have been visited
 */
public interface ResultVisitor {

    void visit(Map<Integer, Long> hashMap);

    List<EmbedField> getEmbedField();

    default long getNumberOfResults(Map<Integer, Long> resultMap) {
        return resultMap.values().stream().mapToLong(result -> result).sum();
    }

    default String generatePercentage(Long numberOfOccurrences, long numberOfResults) {
        final double resultProbability = (double) numberOfOccurrences / numberOfResults;
        final double resultPercentage = resultProbability * 100;
        String formatString = "0.#####" + (resultPercentage < 0.00001 && resultPercentage > 0 ? "E00" : "") + "%";
        DecimalFormat df = new DecimalFormat(formatString);
        return df.format(resultProbability);
    }
}
