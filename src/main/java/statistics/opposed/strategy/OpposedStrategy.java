package statistics.opposed.strategy;

import rolling.BuildablePoolResult;

import java.text.DecimalFormat;
import java.util.HashMap;

public interface OpposedStrategy {
    static String generatePercentage(double resultProbability) {
        final double resultPercentage = resultProbability * 100;
        String formatString = "0.#####" + (resultPercentage < 0.00001 && resultPercentage > 0 ? "E00" : "") + "%";
        DecimalFormat df = new DecimalFormat(formatString);
        return df.format(resultProbability);
    }

    double getProbability(boolean attacker, HashMap<BuildablePoolResult, Long> attackerResults, HashMap<BuildablePoolResult, Long> defenderResults);

    /**
     * Gets the total sample space of the opposed check, which is equal to the product of all the dice in both pools
     *
     * @param attackerMap The Map of all rolls for the attacker
     * @param defenderMap The Map of all rolls for the defender
     * @return The sample space of the union of the sample spaces of the outcomes for the attack and defense pool
     * @throws ArithmeticException If the total sample space is larger than the long integer limit
     */
    default Long getTotalSampleSpace(HashMap<BuildablePoolResult, Long> attackerMap, HashMap<BuildablePoolResult, Long> defenderMap) throws ArithmeticException {
        Long attackerSampleSpace = attackerMap.values().stream().reduce(0L, Math::addExact);
        Long defenderSampleSpace = defenderMap.values().stream().reduce(0L, Math::addExact);

        return Math.multiplyExact(attackerSampleSpace, defenderSampleSpace);
    }

}
