package statistics;

import logic.EmbedField;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.*;

import java.util.ArrayList;
import java.util.List;

public class GenerateStatistics implements StatisticsState {
    private ArrayList<Integer> regularDice;
    private ArrayList<Integer> plotDice;
    private ArrayList<Integer> flatBonus;
    private ArrayList<Integer> keptDice;
    private List<DiceResult> resultList = new ArrayList<>();
    private PoolOptions poolOptions;

    public GenerateStatistics(PoolOptions poolOptions) {
        this.poolOptions = poolOptions;
        this.regularDice = poolOptions.getRegularDice();
        this.plotDice = poolOptions.getPlotDice();
        this.flatBonus = poolOptions.getFlatBonus();
        this.keptDice = poolOptions.getKeptDice();
    }

    @Override
    public void process(StatisticsContext context) {
        generateResults(regularDice, plotDice, keptDice, flatBonus);
        ArrayList<ResultVisitor> resultVisitors = new ArrayList<>();
        resultVisitors.add(new SumVisitor());
        resultVisitors.add(new DifficultyVisitor());
        resultVisitors.add(new DoomVisitor());
        resultVisitors.add(new StatisticsVisitor());
        for (DiceResult result : resultList) {
            for (ResultVisitor visitor : resultVisitors) {
                visitor.visit(result);
            }
        }
        EmbedBuilder statsEmbed = new EmbedBuilder();
        for (ResultVisitor visitor : resultVisitors) {
            for (EmbedField field : visitor.getEmbedField()) {
                statsEmbed.addInlineField(field.getTitle(), field.getContent());
            }
        }
        context.setEmbedBuilder(statsEmbed);
    }

    //Prep method for generateResults to copy the dice list to prevent it from being modified
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus) {
        if (!diceList.isEmpty()) {
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            generateResults(diceListCopy, plotDice, keptDice, flatBonus, new DiceResult(poolOptions.getTop()));
        } else {
            generatePDResults(plotDice, keptDice, flatBonus, new DiceResult(poolOptions.getTop()));
        }
    }

    //Recursive method to generate an ArrayList of results
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus, DiceResult result) {
        if (diceList.isEmpty()) {
            generatePDResults(plotDice, keptDice, flatBonus, result);
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            int diceNum = diceListCopy.remove(0);
            for (int i = 1; i <= diceNum; i++) {
                DiceResult resultCopy = result.copy();
                resultCopy.addDiceToResult(i);
                generateResults(diceListCopy, plotDice, keptDice, flatBonus, resultCopy);
            }
        }
    }

    //Recursive method for handling plot dice
    private void generatePDResults(ArrayList<Integer> plotDice, ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus, DiceResult result) {
        if (plotDice.isEmpty()) {
            generateKDResults(keptDice, flatBonus, result);
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(plotDice);
            int diceNum = diceListCopy.remove(0);
            for (int i = diceNum / 2; i <= diceNum; i++) {
                DiceResult resultCopy = result.copy();
                resultCopy.addPlotDice(i);
                generatePDResults(diceListCopy, keptDice, flatBonus, resultCopy);
            }
        }
    }

    //Recursive method for handling kept dice
    private void generateKDResults(ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus, DiceResult result) {
        if (keptDice.isEmpty()) {
            generateFlatResults(flatBonus, result);
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(keptDice);
            int diceNum = diceListCopy.remove(0);
            for (int i = 1; i <= diceNum; i++) {
                DiceResult resultCopy = result.copy();
                resultCopy.addKeptDice(i);
                generateKDResults(diceListCopy, flatBonus, resultCopy);
            }
        }
    }

    //Recursive method for handling flat bonuses
    private void generateFlatResults(ArrayList<Integer> flatBonus, DiceResult result) {
        if (flatBonus.isEmpty()) {
            resultList.add(result);
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(flatBonus);
            int diceNum = diceListCopy.remove(0);
            DiceResult resultCopy = result.copy();
            resultCopy.addFlatBonues(diceNum);
            generateFlatResults(diceListCopy, resultCopy);
        }
    }
}
