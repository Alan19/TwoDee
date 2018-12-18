package logic.statisticstates.resultvisitors;

import logic.DiceResult;

import java.util.TreeMap;

public class SumVisitor implements ResultVisitor{
    TreeMap<Integer, Integer> diceOutcomes;

    public SumVisitor(){
        diceOutcomes = new TreeMap<>();
    }

    @Override
    /*
      Loops through TreeMap and increment a key based on the result
     */
    public void visit(DiceResult result) {
        int rollResult = result.getResult();
        if (diceOutcomes.containsKey(rollResult)){
            diceOutcomes.put(rollResult, diceOutcomes.get(rollResult) + 1);
        }
        else {
            diceOutcomes.put(rollResult, 1);
        }
    }
}
