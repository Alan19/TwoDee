package statistics;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

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
//        generateResults(regularDice, plotDice, keptDice, flatBonus);
        HashMap<RollResultBuilder, Integer> results = generateResultsHash();
        HashMap<Integer, Integer> occurrences = new HashMap<>();
        int total = results.values().stream().mapToInt(value -> value).sum();
        for (Map.Entry<RollResultBuilder, Integer> entry : results.entrySet()) {
            int rollResult = entry.getKey().getResult();
            if (occurrences.containsKey(rollResult)) {
                occurrences.put(rollResult, entry.getValue() + occurrences.get(rollResult));
            }
            else {
                occurrences.put(rollResult, entry.getValue());
            }
        }
        HashMap<Integer, Double> computedProb = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : occurrences.entrySet()) {
            computedProb.put(entry.getKey(), (double) entry.getValue() / total);
        }
        System.out.println(occurrences);
        System.out.println(computedProb);
//        EmbedBuilder statsEmbed = new EmbedBuilder();
//        for (ResultVisitor visitor : resultVisitors) {
//            for (EmbedField field : visitor.getEmbedField()) {
//                statsEmbed.addInlineField(field.getTitle(), field.getContent());
//            }
//        }
//        context.setEmbedBuilder(statsEmbed);
        context.setEmbedBuilder(new EmbedBuilder());
    }

    /**
     * Generates the result for the dice rolls by creating a Hashmap of unique roll results to int. Dropped dice are not considered for equality.
     *
     * @return A Hashmap of all of the possible results mapped to the number of times it occurs
     */
    private HashMap<RollResultBuilder, Integer> generateResultsHash() {
        HashMap<RollResultBuilder, Integer> rollResultOccurrences = new HashMap<>();
        //Create n roll result objects for each face of the die
        rollResultOccurrences = processNormalDice(rollResultOccurrences);
        rollResultOccurrences = processPlotDice(rollResultOccurrences);
        rollResultOccurrences = processKeptDice(rollResultOccurrences);
        rollResultOccurrences = processFlatBonus(rollResultOccurrences);
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Integer> processFlatBonus(HashMap<RollResultBuilder, Integer> rollResultOccurrences) {
        for (Integer bonus : flatBonus) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addKeptResult(bonus);
                rollResultOccurrences.put(newResult, 1);
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Integer> mappedResult = new HashMap<>();
                rollResultOccurrences.forEach((key, value) -> {
                    RollResultBuilder newResult = key.copy().addFlatBonus(bonus);
                    mappedResult.put(newResult, mappedResult.getOrDefault(newResult, 0) + value);
                });
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Integer> processKeptDice(HashMap<RollResultBuilder, Integer> rollResultOccurrences) {
        for (Integer dice : keptDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addKeptResult(i);
                    rollResultOccurrences.put(newResult, 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Integer> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences.forEach((key, value) -> IntStream.rangeClosed(1, dice).mapToObj(i -> key.copy().addKeptResult(i)).forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Integer> processPlotDice(HashMap<RollResultBuilder, Integer> rollResultOccurrences) {
        for (Integer dice : plotDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addPlotResult(Math.max(i, dice / 2));
                    rollResultOccurrences.put(newResult, 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Integer> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences.forEach((key, value) -> IntStream.rangeClosed(1, dice).mapToObj(i -> key.copy().addPlotResult(Math.max(i, dice / 2))).forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Integer> processNormalDice(HashMap<RollResultBuilder, Integer> rollResultOccurrences) {
        for (Integer dice : regularDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addResult(i);
                    rollResultOccurrences.put(newResult, 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Integer> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences
                        //Map each object in rollResultOccurrences to n DiceResult objects
                        .forEach((key, value) -> IntStream.rangeClosed(1, dice)
                                .mapToObj(i -> key.copy().addResult(i))
                                //Put each new object into mappedResult
                                .forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    //Prep method for generateResults to copy the dice list to prevent it from being modified
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus) {
        if (!diceList.isEmpty()) {
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            generateResults(diceListCopy, plotDice, keptDice, flatBonus, new RollResult(poolOptions.getTop()));
        }
        else {
            generatePDResults(plotDice, keptDice, flatBonus, new RollResult(poolOptions.getTop()));
        }
    }

    //Recursive method to generate an ArrayList of results
    private void generateResults(ArrayList<Integer> diceList, ArrayList<Integer> plotDice, ArrayList<Integer> keptDice, ArrayList<Integer> flatBonus, RollResult result) {
        if (diceList.isEmpty()) {
            generatePDResults(plotDice, keptDice, flatBonus, result);
        }
        else {
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
        }
        else {
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
        }
        else {
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
        }
        else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(flatBonus);
            int diceNum = diceListCopy.remove(0);
            RollResult resultCopy = result.copy();
            resultCopy.addFlatBonuses(diceNum);
            generateFlatResults(diceListCopy, resultCopy);
        }
    }
}
