package logic;

import java.util.ArrayList;

public class DiceResult {

    private ArrayList<Integer> dice;

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
            dice.sort((o1, o2) -> {
                if (o1 < o2){
                    return 1;
                }
                else if (o1 > o2){
                    return -1;
                }
                else {
                    return 0;
                }
            });
            return dice.get(0) + dice.get(1);
        }
    }

    public int addPlotDice(int pd){
        return getResult() + pd;
    }
}
