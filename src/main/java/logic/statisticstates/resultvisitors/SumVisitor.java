package logic.statisticstates.resultvisitors;

import logic.DiceResult;
import logic.EmbedField;

import java.util.*;

public class SumVisitor implements ResultVisitor {
    private TreeMap<Integer, Integer> diceOutcomes;

    public SumVisitor(){
        diceOutcomes = new TreeMap<>();
    }

    /*
      Loops through TreeMap and increment a key based on the result
     */
    @Override
    public void visit(DiceResult result) {
        int rollResult = result.getResult();
        if (diceOutcomes.containsKey(rollResult)){
            diceOutcomes.put(rollResult, diceOutcomes.get(rollResult) + 1);
        }
        else {
            diceOutcomes.put(rollResult, 1);
        }
    }

    @Override
    public List<EmbedField> getEmbedField() {
        EmbedField embedField = new EmbedField();
        embedField.setTitle("Chance to roll a:");
        for (Map.Entry<Integer, Integer> outcome : diceOutcomes.entrySet()) {
            embedField.appendContent(outcome.getKey() + ": " + generatePercentage(outcome.getValue(), getNumberOfResults(diceOutcomes)) + "\n");
        }
        ArrayList<EmbedField> output = new ArrayList<>();
        output.add(embedField);
        return output;
    }
}
