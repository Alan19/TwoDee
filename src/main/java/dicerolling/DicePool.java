package dicerolling;

import java.util.ArrayList;
import java.util.List;

public class DicePool {
    //Dice Types
    private List<Integer> regularDice = new ArrayList<>();
    private List<Integer> plotDice = new ArrayList<>();
    private List<Integer> keptDice = new ArrayList<>();
    private List<Integer> flatBonuses = new ArrayList<>();
    //Configs
    private int keepHowMany = 2;
    private boolean enableEnhancementEmojis = true; //TODO Switch to reading from configs
    private String difficulty = "";
    private int plotPointDiscount = 0;
    private boolean enableOpportunities = true;
    private int minFacets = 0;

    public List<Integer> getRegularDice() {
        return regularDice;
    }

    public void setRegularDice(List<Integer> regularDice) {
        this.regularDice = regularDice;
    }

    public List<Integer> getPlotDice() {
        return plotDice;
    }

    public List<Integer> getKeptDice() {
        return keptDice;
    }

    public List<Integer> getFlatBonuses() {
        return flatBonuses;
    }

    public int getNumberOfKeptDice() {
        return keepHowMany;
    }

    public DicePool setNumberOfKeptDice(int num) {
        keepHowMany = num;
        return this;
    }

    public boolean enableEnhancements() {
        return enableEnhancementEmojis;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public DicePool setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public int getPlotPointDiscount() {
        return plotPointDiscount;
    }

    public DicePool setPlotPointDiscount(int num) {
        plotPointDiscount = num;
        return this;
    }

    public boolean enableOpportunities() {
        return enableOpportunities;
    }

    public DicePool addDice(int dice) {
        if (dice > minFacets) {
            regularDice.add(dice);
        }
        return this;
    }

    public DicePool addPlotDice(int dice) {
        plotDice.add(dice);
        return this;
    }

    public DicePool addKeptDice(int dice) {
        keptDice.add(dice);
        return this;
    }

    public DicePool setOpportunities(boolean enable) {
        enableOpportunities = enable;
        return this;
    }

    public DicePool addDice(String diceType, int dice) {
        switch (diceType) {
            case "d":
                addDice(dice);
                break;
            case "pd":
                addPlotDice(dice);
                break;
            case "kd":
                addKeptDice(dice);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + diceType);
        }
        return this;
    }

    public DicePool addFlatBonus(int bonus) {
        flatBonuses.add(bonus);
        return this;
    }

    public DicePool enableEnhancement(boolean enable) {
        enableEnhancementEmojis = enable;
        return this;
    }

    public DicePool setMinFacets(int minFacets) {
        this.minFacets = minFacets;
        return this;
    }

    public int getPlotPointsSpent() {
        return plotDice.stream().mapToInt(plotDie -> (int) Math.ceil((float) plotDie / 2)).sum();
    }
}
