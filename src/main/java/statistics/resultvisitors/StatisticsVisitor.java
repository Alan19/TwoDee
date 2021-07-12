package statistics.resultvisitors;

import util.EmbedField;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsVisitor implements ResultVisitor {

    private double mean;
    private double standardDeviation;

    public StatisticsVisitor() {
    }

    /**
     * Populates the SummaryStatistics object
     *
     * @param hashMap A Roll Result to Number of Occurrences HashMap
     */
    @Override
    public void visit(Map<Integer, Long> hashMap) {
        final long sum = hashMap.values().stream().mapToLong(value -> value).sum();
        mean = hashMap.entrySet().stream().mapToDouble(value -> (double) value.getKey() * value.getValue() / sum).sum();
        final double variance = hashMap.entrySet().stream().mapToDouble(value -> Math.pow(value.getKey() - mean, 2) * value.getValue() / sum).sum();
        standardDeviation = Math.sqrt(variance);
    }

    @Override
    public List<EmbedField> getEmbedField() {
        ArrayList<EmbedField> embeds = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("#.##");
        DecimalFormat roundToNearestNumber = new DecimalFormat("#");
        embeds.add(new EmbedField("Mean", df.format(mean)));
        embeds.add(new EmbedField("Standard Deviation", df.format(standardDeviation)));
        embeds.add(new EmbedField("95% of the time, your roll will be between", roundToNearestNumber.format(mean - standardDeviation * 2) + " and " + roundToNearestNumber.format(mean + standardDeviation * 2)));
        return embeds;
    }
}
