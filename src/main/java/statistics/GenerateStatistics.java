package statistics;

import dicerolling.BuildablePoolResult;
import dicerolling.DicePoolBuilder;
import dicerolling.FastRollResult;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.*;
import util.EmbedField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generates statistics for a dice pool by calculating the sample set of all possible outcomes. Does this by using a Map of RollResults to number of occurrences. For every die in the pool, and for every facet of that die, map each RollResult in the map to a copy of that object with that die result added.
 */
public class GenerateStatistics {
    private final DicePoolBuilder dicePool;
    private final EmbedBuilder result;

    public GenerateStatistics(DicePoolBuilder dicePool) {
        final long totalCombos = Stream.of(dicePool.getRegularDice(), dicePool.getRegularDice(), dicePool.getEnhancedDice(), dicePool.getChaosDice())
                .flatMapToLong(integers -> integers.stream().mapToLong(value -> value))
                .reduce(1, (left, right) -> left * right);
        final EmbedBuilder overloadEmbed = new EmbedBuilder().setDescription("That's way too many dice for me to handle. Try using less dice.");
        this.dicePool = dicePool;
        if (totalCombos < 0) {
            this.result = overloadEmbed;
            return;
        }
        //Generate the getResults result to occurrence HashMap
        HashMap<BuildablePoolResult, Long> results = generateResultsMap();
        final int[] resultStream = results.keySet().stream().mapToInt(BuildablePoolResult::getTotal).toArray();
        final int minRoll = Arrays.stream(resultStream).min().orElse(0);
        final int maxRoll = Arrays.stream(resultStream).max().orElse(0);

        //Generate the getResults to occurrence HashMap
        final Map<Integer, Long> rollToOccurrences = IntStream.rangeClosed(minRoll, maxRoll)
                .boxed()
                .collect(Collectors.toMap(integer -> integer, integer -> results
                        .entrySet()
                        .stream()
                        .filter(rollResultBuilderLongEntry -> rollResultBuilderLongEntry.getKey().getTotal() == integer)
                        .mapToLong(Map.Entry::getValue)
                        .sum()));


        //Generate the opportunity to occurrence HashMap
        final int[] opportunityArr = results.keySet().stream().mapToInt(BuildablePoolResult::getDoomGenerated).toArray();
        final int minOpportunities = Arrays.stream(opportunityArr).min().orElse(0);
        final int maxOpportunities = Arrays.stream(opportunityArr).max().orElse(0);
        final Map<Integer, Long> rollToOpportunities = IntStream.rangeClosed(minOpportunities, maxOpportunities)
                .boxed()
                .collect(Collectors.toMap(integer -> integer, integer -> results
                        .entrySet()
                        .stream()
                        .filter(rollResultBuilderLongEntry -> rollResultBuilderLongEntry.getKey().getDoomGenerated() == integer)
                        .mapToLong(Map.Entry::getValue)
                        .sum()));

        if (rollToOccurrences.values().stream().anyMatch(aLong -> aLong < 0) || rollToOpportunities.values().stream().anyMatch(aLong -> aLong < 0)) {
            this.result = overloadEmbed;
        }
        else {
            this.result = generateEmbed(rollToOccurrences, rollToOpportunities, results);
        }
    }

    public EmbedBuilder getResult() {
        return result;
    }

    // TODO Rework / remove the visitor pattern

    /**
     * Creates an embed based on the statistics
     *
     * @param rollToOccurrences   The HashMap that represents the getResults mapped to the number of occurrences of the total
     * @param rollToOpportunities The HashMap that maps the number of opportunities to the number of occurrences
     * @param results             The HashMap that contains each possible combination of dice with the number of occurrences of that combination
     * @return The embed to be sent
     */
    private EmbedBuilder generateEmbed(Map<Integer, Long> rollToOccurrences, Map<Integer, Long> rollToOpportunities, HashMap<BuildablePoolResult, Long> results) {
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
     * Generates the result for the dice rolls by creating a Hashmap of unique getResults results to int. Dropped dice are not considered for equality.
     *
     * @return A Hashmap of all of the possible results mapped to the number of times it occurs
     */
    private HashMap<BuildablePoolResult, Long> generateResultsMap() {
        HashMap<BuildablePoolResult, Long> rollResultOccurrences = new HashMap<>();
        //Create n getResults result objects for each face of the die
        rollResultOccurrences = processNormalDice(rollResultOccurrences);
        rollResultOccurrences = processPlotDice(rollResultOccurrences);
        rollResultOccurrences = processKeptDice(rollResultOccurrences);
        rollResultOccurrences = processChaosDice(rollResultOccurrences);
        rollResultOccurrences = processFlatBonus(rollResultOccurrences);
        return rollResultOccurrences;
    }

    private HashMap<BuildablePoolResult, Long> processFlatBonus(HashMap<BuildablePoolResult, Long> rollResultOccurrences) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Sum up all flat bonuses
        final int flatBonus = dicePool.getFlatBonuses().stream().mapToInt(value -> value).sum();
        HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
        for (Map.Entry<BuildablePoolResult, Long> entry : newMap.entrySet()) {
            BuildablePoolResult key = entry.getKey();
            Long value = entry.getValue();
            tempMap.put(key.addFlatBonus(flatBonus), value);
        }
        return tempMap;
    }

    private HashMap<BuildablePoolResult, Long> processKeptDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer keptDice : dicePool.getKeptDice()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, keptDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getDiceKept()).addKeptDice(i))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            }
            else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, keptDice)
                        .mapToObj(key::addKeptDice)
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

    private HashMap<BuildablePoolResult, Long> processChaosDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer keptDice : dicePool.getChaosDice()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, keptDice)
                        .map(operand -> operand * -1)
                        .mapToObj(i -> new FastRollResult(dicePool.getDiceKept()).addKeptDice(i))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            }
            else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, keptDice)
                        .map(operand -> operand * -1)
                        .mapToObj(key::addKeptDice)
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

    private HashMap<BuildablePoolResult, Long> processPlotDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer plotDice : dicePool.getPlotDice()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, plotDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getDiceKept()).addPlotDice(getPlotDieValue(plotDice, i)))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            }
            else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, plotDice)
                        .mapToObj(i -> key.addPlotDice(getPlotDieValue(plotDice, i)))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

    /**
     * Gets the value of a rolled plot die
     *
     * @param plotDice The number of facets in the plot die
     * @param i        The number rolled
     * @return The result of a plot die, which has a minimum of half its facets if there is only one plot die being rolled
     */
    private int getPlotDieValue(Integer plotDice, int i) {
        return dicePool.getPlotDice().size() > 1 ? i : Math.max(i, plotDice / 2);
    }

    private HashMap<BuildablePoolResult, Long> processNormalDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer regularDice : dicePool.getRegularDice()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, regularDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getDiceKept()).addRegularDice(i))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            }
            else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, regularDice)
                        .mapToObj(key::addRegularDice)
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

}
