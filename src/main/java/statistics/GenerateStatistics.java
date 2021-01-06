package statistics;

import dicerolling.DicePool;
import dicerolling.RollResult;
import logic.EmbedField;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GenerateStatistics implements StatisticsState {
    private final List<Integer> regularDice;
    private final List<Integer> plotDice;
    private final List<Integer> flatBonus;
    private final List<Integer> keptDice;
    private final DicePool dicePool;

    public GenerateStatistics(DicePool dicePool) {
        this.dicePool = dicePool;
        this.regularDice = dicePool.getRegularDice();
        this.plotDice = dicePool.getPlotDice();
        this.flatBonus = dicePool.getFlatBonuses();
        this.keptDice = dicePool.getKeptDice();
    }

    /**
     * Generates a HashMap for the number of occurrences of a result happens in the sample size of a pool
     *
     * @param context The StatisticsContext that an embed may be loaded into
     */
    @Override
    public void process(StatisticsContext context) {
        //Generate the roll result to occurrence HashMap
        HashMap<RollResult, Long> results = generateResultsHash();
        final int[] resultStream = results.keySet().stream().mapToInt(RollResult::getTotal).toArray();
        final int minRoll = Arrays.stream(resultStream).min().orElse(0);
        final int maxRoll = Arrays.stream(resultStream).max().orElse(0);

        //Generate the roll to occurrence HashMap
        final Map<Integer, Long> rollToOccurrences = IntStream.rangeClosed(minRoll, maxRoll)
                .boxed()
                .collect(Collectors.toMap(integer -> integer, integer -> results
                        .entrySet()
                        .stream()
                        .filter(rollResultBuilderLongEntry -> rollResultBuilderLongEntry.getKey().getTotal() == integer)
                        .mapToLong(Map.Entry::getValue)
                        .sum()));


        //Generate the opportunity to occurrence HashMap
        final int[] opportunityArr = results.keySet().stream().mapToInt(RollResult::getDoom).toArray();
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


        if (rollToOccurrences.values().stream().anyMatch(aLong -> aLong < 0) || rollToOpportunities.values().stream().anyMatch(aLong -> aLong < 0)) {
            context.setState(new GenerateOverloadMessage());
            return;
        }
        EmbedBuilder statsEmbed = generateEmbed(rollToOccurrences, rollToOpportunities, results);
        context.setEmbedBuilder(statsEmbed);
    }

    /**
     * Creates an embed based on the statistics
     *
     * @param rollToOccurrences   The HashMap that represents the roll mapped to the number of occurrences of the total
     * @param rollToOpportunities The HashMap that maps the number of opportunities to the number of occurrences
     * @param results             The HashMap that contains each possible combination of dice with the number of occurrences of that combination
     * @return The embed to be sent
     */
    private EmbedBuilder generateEmbed(Map<Integer, Long> rollToOccurrences, Map<Integer, Long> rollToOpportunities, HashMap<RollResult, Long> results) {
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
        OpportunityVisitor opportunityVisitor = new OpportunityVisitor(results);
        resultVisitors.add(opportunityVisitor);
        ResultVisitor statisticsVisitor = new StatisticsVisitor();
        statisticsVisitor.visit(rollToOccurrences);
        resultVisitors.add(statisticsVisitor);
        for (ResultVisitor visitor : resultVisitors) {
            for (EmbedField field : visitor.getEmbedField()) {
                statsEmbed.addInlineField(field.getTitle(), field.getContent());
            }
        }
        return statsEmbed;
    }

    /**
     * Generates the result for the dice rolls by creating a Hashmap of unique roll results to int. Dropped dice are not considered for equality.
     *
     * @return A Hashmap of all of the possible results mapped to the number of times it occurs
     */
    private HashMap<RollResult, Long> generateResultsHash() {
        HashMap<RollResult, Long> rollResultOccurrences = new HashMap<>();
        //Create n roll result objects for each face of the die
        rollResultOccurrences = processNormalDice(rollResultOccurrences);
        rollResultOccurrences = processPlotDice(rollResultOccurrences);
        rollResultOccurrences = processKeptDice(rollResultOccurrences);
        rollResultOccurrences = processFlatBonus(rollResultOccurrences);
        return rollResultOccurrences;
    }

    private HashMap<RollResult, Long> processFlatBonus(HashMap<RollResult, Long> rollResultOccurrences) {
        for (Integer bonus : flatBonus) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                RollResult newResult = new RollResult(dicePool.getNumberOfKeptDice(), true).addKeptResult(bonus);
                rollResultOccurrences.put(newResult, (long) 1);
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResult, Long> mappedResult = new HashMap<>();
                rollResultOccurrences.forEach((key, value) -> {
                    RollResult newResult = key.copy().addFlatBonus(bonus);
                    mappedResult.put(newResult, mappedResult.getOrDefault(newResult, (long) 0) + value);
                });
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResult, Long> processKeptDice(HashMap<RollResult, Long> rollResultOccurrences) {
        for (Integer dice : keptDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResult newResult = new RollResult(dicePool.getNumberOfKeptDice(), true).addKeptResult(i);
                    rollResultOccurrences.put(newResult, (long) 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResult, Long> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences.forEach((key, value) -> IntStream.rangeClosed(1, dice).mapToObj(i -> key.copy().addKeptResult(i)).forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, (long) 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResult, Long> processPlotDice(HashMap<RollResult, Long> rollResultOccurrences) {
        for (Integer dice : plotDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResult newResult = new RollResult(dicePool.getNumberOfKeptDice(), true).addPlotResult(plotDice.size() <= 1 ? Math.max(i, dice / 2) : i);
                    rollResultOccurrences.put(newResult, rollResultOccurrences.getOrDefault(newResult, (long) 0) + (long) 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResult, Long> mappedResult = new HashMap<>();
                //Map each roll result object into n roll result objects by adding a nice result of 1 through n to the result object
                rollResultOccurrences.forEach((key, value) -> IntStream.rangeClosed(1, dice).mapToObj(i -> key.copy().addPlotResult(plotDice.size() <= 1 ? Math.max(i, dice / 2) : i)).forEach(newResult -> mappedResult.put(newResult, mappedResult.getOrDefault(newResult, (long) 0) + value)));
                //Replace the original HashMap
                rollResultOccurrences = new HashMap<>(mappedResult);
            }
        }
        return rollResultOccurrences;
    }

    private HashMap<RollResult, Long> processNormalDice(HashMap<RollResult, Long> rollResultOccurrences) {
        for (Integer dice : regularDice) {
            //Directly insert if HashMap is empty
            if (rollResultOccurrences.isEmpty()) {
                //Create n RollResult objects with each possible outcome of the dice
                for (int i = 1; i <= dice; i++) {
                    RollResult newResult = new RollResult(dicePool.getNumberOfKeptDice(), true).addResult(i);
                    rollResultOccurrences.put(newResult, (long) 1);
                }
            }
            else {
                //Create new HashMap to copy values into
                HashMap<RollResult, Long> mappedResult = new HashMap<>();
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
