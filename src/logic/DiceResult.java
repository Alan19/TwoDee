package logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class DiceResult {

    private ArrayList<Integer> dice;
    private int result;

    public DiceResult(){
        dice = new ArrayList<>();
    }

    public void addDiceToResult(int num){
        dice.add(num);
    }

    public int getResult(){
        if (dice.size() == 1){
            return dice.get(0);
        }
        else {
            //Sorts dice and reverse
            dice.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return o1.compareTo(o2);
                }
            });
        }
        Collections.reverse(dice);
        return dice.get(0) + dice.get(1);
    }

    private DiceResult(ArrayList<Integer> diceList){
        dice = diceList;
    }

    public int addPlotDice(int pd){
        return getResult() + pd;
    }

    public DiceResult copy(){
        return new DiceResult(new ArrayList<>(dice));
    }
}
