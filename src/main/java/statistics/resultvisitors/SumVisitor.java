package statistics.resultvisitors;

import util.EmbedField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SumVisitor implements ResultVisitor {
    private final TreeMap<Integer, Long> diceOutcomes;

    public SumVisitor() {
        diceOutcomes = new TreeMap<>();
    }

    /*
      Loops through TreeMap and increment a key based on the result
     */
    @Override
    public void visit(Map<Integer, Long> hashMap) {
        hashMap.forEach(diceOutcomes::put);
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
