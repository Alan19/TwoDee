package statistics.resultvisitors;

import logic.EmbedField;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import statistics.RollResultBuilder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

public class StatisticsVisitor implements ResultVisitor {

    private SummaryStatistics summary;

    public StatisticsVisitor() {
        summary = new SummaryStatistics();
    }

    @Override
    public void visit(RollResultBuilder result, Long occurrences) {
        LongStream.range(0, occurrences).forEach(i -> summary.addValue(result.getResult()));
    }

    @Override
    public List<EmbedField> getEmbedField() {
        ArrayList<EmbedField> embeds = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("#.##");
        DecimalFormat roundToNearestNumber = new DecimalFormat("#");
        embeds.add(new EmbedField("Mean", df.format(summary.getMean())));
        embeds.add(new EmbedField("Standard Deviation", df.format(summary.getStandardDeviation())));
        embeds.add(new EmbedField("95% of the time, your roll will be between", roundToNearestNumber.format(summary.getMean() - summary.getStandardDeviation() * 2) + " and " + roundToNearestNumber.format(summary.getMean() + summary.getStandardDeviation() * 2)));
        return embeds;
    }
}
