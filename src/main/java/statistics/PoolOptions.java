package statistics;

import java.util.ArrayList;

public class PoolOptions {
    //Add all of the dice to the ArrayLists based on dice type
    private ArrayList<Integer> regularDice = new ArrayList<>();
    private ArrayList<Integer> plotDice = new ArrayList<>();
    private ArrayList<Integer> flatBonus = new ArrayList<>();
    private ArrayList<Integer> keptDice = new ArrayList<>();
    private int top = 2;

    public ArrayList<Integer> getRegularDice() {
        return regularDice;
    }

    public ArrayList<Integer> getPlotDice() {
        return plotDice;
    }

    public ArrayList<Integer> getFlatBonus() {
        return flatBonus;
    }

    public ArrayList<Integer> getKeptDice() {
        return keptDice;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public void addDice(int dice) {
        regularDice.add(dice);
    }

    public void addPlotDice(int dice) {
        plotDice.add(dice);
    }

    public void addKeptDice(int dice) {
        keptDice.add(dice);
    }

    public void addFlatBonus(int dice) {
        flatBonus.add(dice);
    }

    public boolean validPool() {
        if (regularDice.isEmpty() && plotDice.isEmpty() && keptDice.isEmpty()) {
            return false;
        }
        return true;
    }
}
