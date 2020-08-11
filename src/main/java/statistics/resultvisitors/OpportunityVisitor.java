package statistics.resultvisitors;

import dicerolling.RollResult;
import logic.EmbedField;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OpportunityVisitor {
    private final Map<RollResult, Long> results;
    private final HashMap<Integer, String> difficulties = new HashMap<>();

    public OpportunityVisitor(Map<RollResult, Long> results) {
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

    public EmbedField generateField() {
        final EmbedField embedField = new EmbedField();
        embedField.setTitle("Chance of failure with opportunity");
        String output;
        long numberOfCombinations = results.values().stream().mapToLong(Long::longValue).sum();
        output = difficulties.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue() + ": " + generatePercentage(getNumberOfFailsWithOpportunities(entry), numberOfCombinations) + "\n")
                .collect(Collectors.joining());
        embedField.appendContent(output);
        return embedField;
    }

    private long getNumberOfFailsWithOpportunities(Map.Entry<Integer, String> entry) {
        return results.entrySet().stream()
                .filter(rollResultLongEntry -> rollResultLongEntry.getKey().getTotal() < entry.getKey() && rollResultLongEntry.getKey().getDoom() > 0)
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    private String generatePercentage(Long numberOfOccurrences, long numberOfResults) {
        final double resultProbability = (double) numberOfOccurrences / numberOfResults;
        final double resultPercentage = resultProbability * 100;
        String formatString = "0.#####" + (resultPercentage < 0.00001 && resultPercentage > 0 ? "E00" : "") + "%";
        DecimalFormat df = new DecimalFormat(formatString);
        return df.format(resultProbability);
    }

}
