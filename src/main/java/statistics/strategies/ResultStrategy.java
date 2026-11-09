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

    /**
     * Gets the total sample space of the opposed check, which is equal to the product of all the dice in both pools
     *
     * @param attackerMap The Map of all rolls for the attacker
     * @param defenderMap The Map of all rolls for the defender
     * @return The sample space of the union of the sample spaces of the outcomes for the attack and defense pool
     * @throws ArithmeticException If the total sample space is larger than the long integer limit
     */
    default Long getTotalSampleSpace(Map<StatisticsRollResult, Long> attackerMap, Map<StatisticsRollResult, Long> defenderMap) throws ArithmeticException {
        Long attackerSampleSpace = attackerMap.values().stream().reduce(0L, Math::addExact);
        Long defenderSampleSpace = defenderMap.values().stream().reduce(0L, Math::addExact);

        return Math.multiplyExact(attackerSampleSpace, defenderSampleSpace);
    }
}
