package statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DiceResult {

    private ArrayList<Integer> dice;
    private ArrayList<Integer> plotDice;
    private ArrayList<Integer> keptDice;
    private ArrayList<Integer> flatBonus;

    public DiceResult(){
        dice = new ArrayList<>();
        plotDice = new ArrayList<>();
        keptDice = new ArrayList<>();
        flatBonus = new ArrayList<>();
    }

    private DiceResult(ArrayList<Integer> diceList, ArrayList<Integer> pdList, ArrayList<Integer> keptList, ArrayList<Integer> flatList) {
        dice = diceList;
        plotDice = pdList;
        keptDice = keptList;
        flatBonus = flatList;
    }

    public void addDiceToResult(int num){
        dice.add(num);
    }

    public int getResult(){
        int sum = 0;
        if (dice.size() == 1){
            sum += dice.get(0);
        }
        else {
            //Sorts dice and reverse to get descending order
            dice.sort(Comparator.naturalOrder());
            Collections.reverse(dice);
            for (int i = 0; i < dice.size() && i < 2; i++){
                sum += dice.get(i);
            }
        }
        for (int pd : plotDice) {
            sum += pd;
        }
        for (int kd : keptDice) {
            sum += kd;
        }
        for (int f : flatBonus) {
            sum += f;
        }
        return sum;
    }

    public void addPlotDice(int pd){
        plotDice.add(pd);
    }

    public DiceResult copy(){
        return new DiceResult(new ArrayList<>(dice), new ArrayList<>(plotDice), new ArrayList<>(keptDice), new ArrayList<>(flatBonus));
    }

    public void addKeptDice(int kd) {
        keptDice.add(kd);
    }

    public void addFlatBonues(int flat) {
        flatBonus.add(flat);
    }

    public int getDoom(){
        int doomCount = 0;
        for (int roll : dice) {
            if (roll == 1){
                doomCount += 1;
            }
        }
        for (int roll : keptDice) {
            if (roll == 1) {
                doomCount += 1;
            }
        }
        return doomCount;
    }
}
