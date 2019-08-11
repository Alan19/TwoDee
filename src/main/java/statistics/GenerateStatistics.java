package statistics;

import logic.EmbedField;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.*;

import java.util.ArrayList;

public class GenerateStatistics implements StatisticsState {
    private ArrayList<Integer> regularDice;
    private ArrayList<Integer> plotDice;
    private ArrayList<Integer> flatBonus;
    private ArrayList<Integer> keptDice;
    private PoolOptions poolOptions;
    private ArrayList<ResultVisitor> resultVisitors;

    public GenerateStatistics(PoolOptions poolOptions) {
        this.poolOptions = poolOptions;
        this.regularDice = poolOptions.getRegularDice();
        this.plotDice = poolOptions.getPlotDice();
        this.flatBonus = poolOptions.getFlatBonus();
        this.keptDice = poolOptions.getKeptDice();

        resultVisitors = new ArrayList<>();
        resultVisitors.add(new SumVisitor());
        resultVisitors.add(new DifficultyVisitor());
        resultVisitors.add(new DoomVisitor());
        resultVisitors.add(new StatisticsVisitor());
    }

    @Override
    public void process(StatisticsContext context) {
        generateResults(regularDice, plotDice, keptDice, flatBonus);

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
            generateResults(diceListCopy, plotDice, keptDice, flatBonus, new RollResult(poolOptions.getTop()));
        } else {
            generatePDResults(plotDice, keptDice, flatBonus, new RollResult(poolOptions.getTop()));
        }
    }

    //Recursive method to generate an ArrayList of results
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus, RollResult result) {
        if (diceList.isEmpty()) {
            generatePDResults(plotDice, keptDice, flatBonus, result);
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            int diceNum = diceListCopy.remove(0);
            for (int i = 1; i <= diceNum; i++) {
                RollResult resultCopy = result.copy();
                resultCopy.addDiceToResult(i);
                generateResults(diceListCopy, plotDice, keptDice, flatBonus, resultCopy);
            }
        }
    }

    //Recursive method for handling plot dice
    private void generatePDResults(ArrayList<Integer> plotDice, ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus, RollResult result) {
        if (plotDice.isEmpty()) {
            generateKDResults(keptDice, flatBonus, result);
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(plotDice);
            int diceNum = diceListCopy.remove(0);
            for (int i = diceNum / 2; i <= diceNum; i++) {
                RollResult resultCopy = result.copy();
                resultCopy.addPlotDice(i);
                generatePDResults(diceListCopy, keptDice, flatBonus, resultCopy);
            }
        }
    }

    //Recursive method for handling kept dice
    private void generateKDResults(ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus, RollResult result) {
        if (keptDice.isEmpty()) {
            generateFlatResults(flatBonus, result);
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(keptDice);
            int diceNum = diceListCopy.remove(0);
            for (int i = 1; i <= diceNum; i++) {
                RollResult resultCopy = result.copy();
                resultCopy.addKeptDice(i);
                generateKDResults(diceListCopy, flatBonus, resultCopy);
            }
        }
    }

    //Recursive method for handling flat bonuses
    private void generateFlatResults(ArrayList<Integer> flatBonus, RollResult result) {
        if (flatBonus.isEmpty()) {
            for (ResultVisitor visitor : resultVisitors) {
                visitor.visit(result);
            }
        } else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(flatBonus);
            int diceNum = diceListCopy.remove(0);
            RollResult resultCopy = result.copy();
            resultCopy.addFlatBonuses(diceNum);
            generateFlatResults(diceListCopy, resultCopy);
        }
    }
}
