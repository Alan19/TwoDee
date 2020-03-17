package statistics;

import logic.EmbedField;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GenerateStatistics implements StatisticsState {
    private ArrayList<Integer> regularDice;
    private ArrayList<Integer> plotDice;
    private ArrayList<Integer> flatBonus;
    private ArrayList<Integer> keptDice;
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
        HashMap<RollResultBuilder, Long> results = generateResultsHash();
        final int[] resultStream = results.keySet().stream().mapToInt(RollResultBuilder::getResult).toArray();
        final int minRoll = Arrays.stream(resultStream).min().orElse(0);
        final int maxRoll = Arrays.stream(resultStream).max().orElse(0);
        final Map<Integer, Long> rollToOccurrences = IntStream.rangeClosed(minRoll, maxRoll)
                .boxed()
                .collect(Collectors.toMap(integer -> integer, integer -> results
                        .entrySet()
                        .stream()
                        .filter(rollResultBuilderLongEntry -> rollResultBuilderLongEntry.getKey().getResult() == integer)
                        .mapToLong(Map.Entry::getValue)
                        .sum()));

        final int[] opportunityArr = results.keySet().stream().mapToInt(RollResultBuilder::getDoom).toArray();
        final int minOpportunities = Arrays.stream(opportunityArr).min().orElse(0);
        final int maxOpportunities = Arrays.stream(opportunityArr).max().orElse(0);
        final Map<Integer, Long> rollToOpportunities = IntStream.rangeClosed(minOpportunities, maxOpportunities)
                .boxed()
                .collect(Collectors.toMap(integer -> integer, integer -> results
                        .entrySet()
                        .stream()
                        .filter(rollResultBuilderLongEntry -> rollResultBuilderLongEntry.getKey().getDoom() == integer)
                        .mapToLong(Map.Entry::getValue)
                        .sum()));

        EmbedBuilder statsEmbed = new EmbedBuilder();
        ArrayList<ResultVisitor> resultVisitors = new ArrayList<>();
        ResultVisitor sumVisitor = new SumVisitor();
        sumVisitor.visit(rollToOccurrences);
        resultVisitors.add(sumVisitor);
        ResultVisitor difficultyVisitor = new DifficultyVisitor();
        difficultyVisitor.visit(rollToOccurrences);
        resultVisitors.add(difficultyVisitor);
        ResultVisitor doomVisitor = new DoomVisitor();
        doomVisitor.visit(rollToOpportunities);
        resultVisitors.add(doomVisitor);
        ResultVisitor statisticsVisitor = new StatisticsVisitor();
        statisticsVisitor.visit(rollToOccurrences);
        resultVisitors.add(statisticsVisitor);
        for (ResultVisitor visitor : resultVisitors) {
            for (EmbedField field : visitor.getEmbedField()) {
                statsEmbed.addInlineField(field.getTitle(), field.getContent());
            }
        }
        context.setEmbedBuilder(statsEmbed);
    }

    /**
     * Generates the result for the dice rolls by creating a Hashmap of unique roll results to int. Dropped dice are not considered for equality.
     *
     * @return A Hashmap of all of the possible results mapped to the number of times it occurs
     */
    private HashMap<RollResultBuilder, Long> generateResultsHash() {
        HashMap<RollResultBuilder, Long> rollResultOccurrences = new HashMap<>();
        //Create n roll result objects for each face of the die
        rollResultOccurrences = processNormalDice(rollResultOccurrences);
        rollResultOccurrences = processPlotDice(rollResultOccurrences);
        rollResultOccurrences = processKeptDice(rollResultOccurrences);
        rollResultOccurrences = processFlatBonus(rollResultOccurrences);
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Long> processFlatBonus(HashMap<RollResultBuilder, Long> rollResultOccurrences) {
        for (Integer bonus : flatBonus) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addKeptResult(bonus);
                rollResultOccurrences.put(newResult, (long) 1);
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Long> mappedResult = new HashMap<>();
                rollResultOccurrences.forEach((key, value) -> {
                    RollResultBuilder newResult = key.copy().addFlatBonus(bonus);
                    mappedResult.put(newResult, mappedResult.getOrDefault(newResult, (long) 0) + value);
                });
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Long> processKeptDice(HashMap<RollResultBuilder, Long> rollResultOccurrences) {
        for (Integer dice : keptDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addKeptResult(i);
                    rollResultOccurrences.put(newResult, (long) 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Long> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences.forEach((key, value) -> IntStream.rangeClosed(1, dice).mapToObj(i -> key.copy().addKeptResult(i)).forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, (long) 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Long> processPlotDice(HashMap<RollResultBuilder, Long> rollResultOccurrences) {
        for (Integer dice : plotDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addPlotResult(Math.max(i, dice / 2));
                    rollResultOccurrences.put(newResult, rollResultOccurrences.getOrDefault(newResult, (long) 0) + (long) 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Long> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences.forEach((key, value) -> IntStream.rangeClosed(1, dice).mapToObj(i -> key.copy().addPlotResult(Math.max(i, dice / 2))).forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, (long) 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResultBuilder, Long> processNormalDice(HashMap<RollResultBuilder, Long> rollResultOccurrences) {
        for (Integer dice : regularDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResultBuilder newResult = new RollResultBuilder(poolOptions.getTop()).addResult(i);
                    rollResultOccurrences.put(newResult, (long) 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResultBuilder, Long> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences
                        //Map each object in rollResultOccurrences to n DiceResult objects
                        .forEach((key, value) -> IntStream.rangeClosed(1, dice)
                                .mapToObj(i -> key.copy().addResult(i))
                                //Put each new object into mappedResult
                                .forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, (long) 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

}
