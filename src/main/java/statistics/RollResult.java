package statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RollResult {

    private ArrayList<Integer> dice;
    private ArrayList<Integer> plotDice;
    private ArrayList<Integer> keptDice;
    private ArrayList<Integer> flatBonus;
    private int keepHowMany;

    public RollResult(int keep) {
        dice = new ArrayList<>();
        plotDice = new ArrayList<>();
        keptDice = new ArrayList<>();
        flatBonus = new ArrayList<>();
        keepHowMany = keep;
    }

    private RollResult(ArrayList<Integer> diceList, ArrayList<Integer> pdList, ArrayList<Integer> keptList, ArrayList<Integer> flatList, int keep) {
        dice = diceList;
        plotDice = pdList;
        keptDice = keptList;
        flatBonus = flatList;
        keepHowMany = keep;
    }

    public int getFlatBonus() {
        return flatBonus.stream().mapToInt(bonus -> bonus).sum();
    }

    public ArrayList<Integer> getKept() {
        ArrayList<Integer> sortedResults = new ArrayList<>(dice);
        ArrayList<Integer> kept = new ArrayList<>();
        sortedResults.sort(Comparator.naturalOrder());
        Collections.reverse(sortedResults);
        for (int i = 0; i < Math.min(sortedResults.size(), keepHowMany); i++) {
            kept.add(sortedResults.get(i));
        }
        return kept;
    }

    public ArrayList<Integer> getDropped() {
        ArrayList<Integer> sortedResults = new ArrayList<>(dice);
        ArrayList<Integer> dropped = new ArrayList<>();
        sortedResults.sort(Comparator.naturalOrder());
        Collections.reverse(sortedResults);
        for (int i = Math.min(sortedResults.size(), keepHowMany); i < sortedResults.size(); i++) {
            dropped.add(sortedResults.get(i));
        }
        return dropped;
    }

    public ArrayList<Integer> getDice() {
        return dice;
    }

    public ArrayList<Integer> getPlotDice() {
        return plotDice;
    }

    public ArrayList<Integer> getKeptDice() {
        return keptDice;
    }

    public void addDiceToResult(int num){
        dice.add(num);
    }

    public int getResult(){
        //Sorts dice and reverse to get descending order
        int sum = 0;
        ArrayList<Integer> sortedResults = new ArrayList<>(dice);
        sortedResults.sort(Comparator.naturalOrder());
        Collections.reverse(sortedResults);
        for (int i = 0; i < Math.min(sortedResults.size(), keepHowMany); i++) {
            sum += sortedResults.get(i);
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

    public RollResult copy() {
        return new RollResult(new ArrayList<>(dice), new ArrayList<>(plotDice), new ArrayList<>(keptDice), new ArrayList<>(flatBonus), keepHowMany);
    }

    public void addKeptDice(int kd) {
        keptDice.add(kd);
    }

    public void addFlatBonuses(int flat) {
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
