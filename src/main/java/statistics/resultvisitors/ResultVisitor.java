package statistics.resultvisitors;

import statistics.DiceResult;
import logic.EmbedField;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Each visitor can return an object that stores a Embed field title and description after all objects have been visited
 */
public interface ResultVisitor {
    void visit(DiceResult result);
    List<EmbedField> getEmbedField();

    default int getNumberOfResults(Map<Integer, Integer> resultMap){
        int sum = 0;
        for (Integer result : resultMap.values()) {
            sum += result;
        }
        return sum;
    }

    default String generatePercentage(Integer numberOfOccurrences, int numberOfResults) {
        DecimalFormat df = new DecimalFormat("0.#####");
        return df.format((double) numberOfOccurrences / numberOfResults * 100) + "%";
    }
}
