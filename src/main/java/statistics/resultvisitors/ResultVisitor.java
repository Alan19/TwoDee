package statistics.resultvisitors;

import logic.EmbedField;
import statistics.RollResultBuilder;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Each visitor can return an object that stores a Embed field title and description after all objects have been visited
 */
public interface ResultVisitor {

    void visit(RollResultBuilder result, Long occurrences);

    List<EmbedField> getEmbedField();

    default int getNumberOfResults(Map<Integer, Long> resultMap) {
        int sum = 0;
        for (Long result : resultMap.values()) {
            sum += result;
        }
        return sum;
    }

    default String generatePercentage(Long numberOfOccurrences, int numberOfResults) {
        DecimalFormat df = new DecimalFormat("0.#####");
        return df.format((double) numberOfOccurrences / numberOfResults * 100) + "%";
    }
}
