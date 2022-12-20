package statistics.strategies;

import rolling.BuildablePoolResult;
import statistics.StatisticsLogic;
import util.EmbedField;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsStrategy implements ResultStrategy {
    @Override
    public List<EmbedField> executeStrategy(HashMap<BuildablePoolResult, Long> resultMap, long totalCombinations) {
        Map<Integer, Long> rollResultToOccurancesMap = StatisticsLogic.getRollResultToOccurancesMap(resultMap);
        final double mean = rollResultToOccurancesMap.entrySet().stream()
                .mapToDouble(value -> (double) value.getKey() * value.getValue() / totalCombinations)
                .sum();
        final double variance = rollResultToOccurancesMap.entrySet().stream()
                .mapToDouble(value -> Math.pow(value.getKey() - mean, 2) * value.getValue() / totalCombinations)
                .sum();
        double standardDeviation = Math.sqrt(variance);
        DecimalFormat df = new DecimalFormat("#.##");
        DecimalFormat roundToNearestNumber = new DecimalFormat("#");
        return List.of(new EmbedField("Mean", df.format(mean)),
                new EmbedField("Standard Deviation", df.format(standardDeviation)),
                new EmbedField("95% of the time, your results will be between", roundToNearestNumber.format(mean - standardDeviation * 2) + " and " + roundToNearestNumber.format(mean + standardDeviation * 2)));
    }
}
