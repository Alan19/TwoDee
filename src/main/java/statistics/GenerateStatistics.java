package statistics;

import dicerolling.DicePool;
import dicerolling.FastRollResult;
import dicerolling.PoolResult;
import util.EmbedField;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates statistics for a dice pool by calculating the sample set of all possible outcomes. Does this by using a Map of RollResults to number of occurrences. For every die in the pool, and for every facet of that die, map each RollResult in the map to a copy of that object with that die result added.
 */
public class GenerateStatistics implements StatisticsState {
    private final DicePool dicePool;

    public GenerateStatistics(DicePool dicePool) {
        this.dicePool = dicePool;
    }

    /**
     * Generates a HashMap for the number of occurrences of a result happens in the sample size of a pool
     *
     * @param context The StatisticsContext that an embed may be loaded into
     */
    @Override
    public void process(StatisticsContext context) {
        //Generate the roll result to occurrence HashMap
        HashMap<PoolResult, Long> results = generateResultsMap();
        final int[] resultStream = results.keySet().stream().mapToInt(PoolResult::getTotal).toArray();
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
        final int[] opportunityArr = results.keySet().stream().mapToInt(PoolResult::getDoomGenerated).toArray();
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
            context.setState(new GenerateOverloadMessage());
            return;
        }
        EmbedBuilder statsEmbed = generateEmbed(rollToOccurrences, rollToOpportunities, results);
        context.setEmbedBuilder(statsEmbed);
    }

    // TODO Rework / remove the visitor pattern

    /**
     * Creates an embed based on the statistics
     *
     * @param rollToOccurrences   The HashMap that represents the roll mapped to the number of occurrences of the total
     * @param rollToOpportunities The HashMap that maps the number of opportunities to the number of occurrences
     * @param results             The HashMap that contains each possible combination of dice with the number of occurrences of that combination
     * @return The embed to be sent
     */
    private EmbedBuilder generateEmbed(Map<Integer, Long> rollToOccurrences, Map<Integer, Long> rollToOpportunities, HashMap<PoolResult, Long> results) {
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
    private HashMap<PoolResult, Long> generateResultsMap() {
        HashMap<PoolResult, Long> rollResultOccurrences = new HashMap<>();
        //Create n roll result objects for each face of the die
        rollResultOccurrences = processNormalDice(rollResultOccurrences);
        rollResultOccurrences = processPlotDice(rollResultOccurrences);
        rollResultOccurrences = processKeptDice(rollResultOccurrences);
        rollResultOccurrences = processChaosDice(rollResultOccurrences);
        rollResultOccurrences = processFlatBonus(rollResultOccurrences);
        return rollResultOccurrences;
    }

    private HashMap<PoolResult, Long> processFlatBonus(HashMap<PoolResult, Long> rollResultOccurrences) {
        HashMap<PoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Sum up all flat bonuses
        final int flatBonus = dicePool.getFlatBonuses().stream().mapToInt(value -> value).sum();
        HashMap<PoolResult, Long> tempMap = new HashMap<>();
        for (Map.Entry<PoolResult, Long> entry : newMap.entrySet()) {
            PoolResult key = entry.getKey();
            Long value = entry.getValue();
            tempMap.put(key.addFlatBonus(flatBonus), value);
        }
        return tempMap;
    }

    private HashMap<PoolResult, Long> processKeptDice(HashMap<PoolResult, Long> rollResultOccurrences) {
        HashMap<PoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer keptDice : dicePool.getKeptDice()) {
            HashMap<PoolResult, Long> tempMap = new HashMap<>();
            // Create n PoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, keptDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getNumberOfKeptDice()).addKeptDice(i))
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

    private HashMap<PoolResult, Long> processChaosDice(HashMap<PoolResult, Long> rollResultOccurrences) {
        HashMap<PoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer keptDice : dicePool.getChaosDice()) {
            HashMap<PoolResult, Long> tempMap = new HashMap<>();
            // Create n PoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, keptDice)
                        .map(operand -> operand * -1)
                        .mapToObj(i -> new FastRollResult(dicePool.getNumberOfKeptDice()).addKeptDice(i))
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

    private HashMap<PoolResult, Long> processPlotDice(HashMap<PoolResult, Long> rollResultOccurrences) {
        HashMap<PoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer plotDice : dicePool.getPlotDice()) {
            HashMap<PoolResult, Long> tempMap = new HashMap<>();
            // Create n PoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, plotDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getNumberOfKeptDice()).addPlotDice(getPlotDieValue(plotDice, i)))
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

    private HashMap<PoolResult, Long> processNormalDice(HashMap<PoolResult, Long> rollResultOccurrences) {
        HashMap<PoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all of the kept dice
        for (Integer regularDice : dicePool.getRegularDice()) {
            HashMap<PoolResult, Long> tempMap = new HashMap<>();
            // Create n PoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, regularDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getNumberOfKeptDice()).addRegularDice(i))
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
