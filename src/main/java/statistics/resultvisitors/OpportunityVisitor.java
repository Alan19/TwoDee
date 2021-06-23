package statistics.resultvisitors;

import com.google.common.collect.ImmutableList;
import dicerolling.PoolResult;
import util.EmbedField;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpportunityVisitor implements ResultVisitor {
    private final Map<PoolResult, Long> results;
    private final Map<Integer, String> difficulties = new HashMap<>();

    public OpportunityVisitor(Map<PoolResult, Long> results) {
        this.results = results;
        difficulties.put(3, "Easy");
        difficulties.put(7, "Average");
        difficulties.put(11, "Hard");
        difficulties.put(15, "Formidable");
        difficulties.put(19, "Heroic");
        difficulties.put(23, "Incredible");
        difficulties.put(27, "Ridiculous");
        difficulties.put(31, "Impossible");
    }

    @Override
    public List<EmbedField> getEmbedField() {
        final EmbedField embedField = new EmbedField();
        embedField.setTitle("Chance of failure with opportunity");
        String output;
        long numberOfCombinations = results.values().stream().mapToLong(Long::longValue).sum();
        output = difficulties.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue() + ": " + generatePercentage(getNumberOfFailsWithOpportunities(entry), numberOfCombinations) + "\n")
                .collect(Collectors.joining());
        embedField.appendContent(output);
        return ImmutableList.of(embedField);
    }

    private long getNumberOfFailsWithOpportunities(Map.Entry<Integer, String> entry) {
        return results.entrySet().stream()
                .filter(rollResultLongEntry -> rollResultLongEntry.getKey().getTotal() < entry.getKey() && rollResultLongEntry.getKey().getDoomGenerated() > 0)
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    @Override
    public void visit(Map<Integer, Long> hashMap) {
        //We don't use an <Integer, Long> map
    }
}
