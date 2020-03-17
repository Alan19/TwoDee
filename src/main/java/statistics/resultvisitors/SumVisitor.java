package statistics.resultvisitors;

import logic.EmbedField;
import statistics.RollResultBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SumVisitor implements ResultVisitor {
    private TreeMap<Integer, Long> diceOutcomes;

    public SumVisitor(){
        diceOutcomes = new TreeMap<>();
    }

    /*
      Loops through TreeMap and increment a key based on the result
     */
    @Override
    public void visit(RollResultBuilder result, Long occurrences) {
        int rollResult = result.getResult();
        diceOutcomes.put(rollResult, (diceOutcomes.getOrDefault(rollResult, (long) 0) + occurrences));
    }

    @Override
    public List<EmbedField> getEmbedField() {
        EmbedField embedField = new EmbedField();
        embedField.setTitle("Chance to roll a:");
        for (Map.Entry<Integer, Long> outcome : diceOutcomes.entrySet()) {
            embedField.appendContent(outcome.getKey() + ": " + generatePercentage(outcome.getValue(), getNumberOfResults(diceOutcomes)) + "\n");
        }
        ArrayList<EmbedField> output = new ArrayList<>();
        output.add(embedField);
        return output;
    }
}
