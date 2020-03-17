package statistics.resultvisitors;

import logic.EmbedField;

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
        DecimalFormat df = new DecimalFormat("0.#####");
        return df.format((double) numberOfOccurrences / numberOfResults * 100) + "%";
    }
}
