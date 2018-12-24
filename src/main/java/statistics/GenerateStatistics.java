package statistics;

import logic.EmbedField;
import statistics.resultvisitors.DifficultyVisitor;
import statistics.resultvisitors.DoomVisitor;
import statistics.resultvisitors.ResultVisitor;
import statistics.resultvisitors.SumVisitor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.ArrayList;
import java.util.List;

public class GenerateStatistics implements StatisticsState {
    private ArrayList<Integer> regularDice;
    private ArrayList<Integer> plotDice;
    private List<DiceResult> resultList = new ArrayList<>();

    public GenerateStatistics(ArrayList<Integer> regularDice, ArrayList<Integer> plotDice) {
        this.regularDice = regularDice;
        this.plotDice = plotDice;
    }

    @Override
    public void process(StatisticsContext context) {
        generateResults(regularDice, plotDice);
        ArrayList<ResultVisitor> resultVisitors = new ArrayList<>();
        resultVisitors.add(new SumVisitor());
        resultVisitors.add(new DifficultyVisitor());
        resultVisitors.add(new DoomVisitor());
        for (DiceResult result : resultList) {
            for (ResultVisitor visitor : resultVisitors) {
                visitor.visit(result);
            }
        }
        EmbedBuilder statsEmbed = new EmbedBuilder();
        for (ResultVisitor visitor : resultVisitors) {
            for (EmbedField field: visitor.getEmbedField()) {
                statsEmbed.addInlineField(field.getTitle(), field.getContent());
            }
        }
        context.setEmbedBuilder(statsEmbed);
    }

    //Prep method for generateResults to copy the dice list to prevent it from being modified
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice){
        if (!diceList.isEmpty()){
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            generateResults(diceListCopy, plotDice, new DiceResult());
        }
        else {
            generatePDResults(plotDice, new DiceResult());
        }
    }

    //Recursive method to generate an ArrayList of results
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, DiceResult result){
        if (diceList.isEmpty()){
            generatePDResults(plotDice, result);
        }
        else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            int diceNum = diceListCopy.remove(0);
            for (int i = 1; i <= diceNum; i++){
                DiceResult resultCopy = result.copy();
                resultCopy.addDiceToResult(i);
                generateResults(diceListCopy, plotDice, resultCopy);
            }
        }
    }

    //Recursive method for handling plot dice
    private void generatePDResults(ArrayList<Integer> plotDice, DiceResult result){
        if (plotDice.isEmpty()){
            resultList.add(result);
        }
        else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(plotDice);
            int diceNum = diceListCopy.remove(0);
            for (int i = diceNum / 2; i <= diceNum; i++){
                DiceResult resultCopy = result.copy();
                resultCopy.addPlotDice(i);
                generatePDResults(diceListCopy, resultCopy);
            }
        }
    }
}
