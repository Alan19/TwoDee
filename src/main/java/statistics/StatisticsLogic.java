package statistics;

import io.vavr.control.Try;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import rolling.BuildablePoolResult;
import rolling.DicePoolBuilder;
import rolling.FastRollResult;
import statistics.strategies.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generates statistics for a dice pool by calculating the sample set of all possible outcomes. Does this by using a Map of RollResults to number of occurrences. For every die in the pool, and for every facet of that die, map each RollResult in the map to a copy of that object with that die result added.
 */
public class StatisticsLogic {
    public static final EmbedBuilder OVERLOAD_EMBED = new EmbedBuilder().setDescription("That's way too many dice for me to handle. Try using less dice.");

    private final Try<HashMap<BuildablePoolResult, Long>> results;

    public Try<HashMap<BuildablePoolResult, Long>> getResults() {
        return results;
    }

    public StatisticsLogic(DicePoolBuilder dicePool) {
        //Generate the getResults result to occurrence HashMap
        results = Try.of(() -> {
            FutureTask<HashMap<BuildablePoolResult, Long>> statisticsTask = new FutureTask<>(() -> generateResultsMap(dicePool));
            statisticsTask.run();
            return statisticsTask.get(30, TimeUnit.SECONDS);
        });
    }

    public static Map<Integer, Long> getRollResultToOccurancesMap(HashMap<BuildablePoolResult, Long> results) {
        final int[] resultStream = results.keySet().stream().mapToInt(BuildablePoolResult::getTotal).toArray();
        final int minRoll = Arrays.stream(resultStream).min().orElse(0);
        final int maxRoll = Arrays.stream(resultStream).max().orElse(0);

        //Generate the getResults to occurrence HashMap
        return IntStream.rangeClosed(minRoll, maxRoll)
                .boxed()
                .collect(Collectors.toMap(integer -> integer, integer -> results
                        .entrySet()
                        .stream()
                        .filter(rollResultBuilderLongEntry -> rollResultBuilderLongEntry.getKey().getTotal() == integer)
                        .mapToLong(Map.Entry::getValue)
                        .reduce(0, Math::addExact)));
    }

    /**
     * Generates a hashmap based on the result hashmap to get the number of occurrences for each potential doom point output on a roll
     * <p>
     * We use reduce instead of sum to catch long overflow
     *
     * @param results A hashmap containing the roll result mapped to the number of occurrences
     * @return A HashMap in the format of: number of doom generated to the number of occurrences
     */
    public static Map<Integer, Long> getRollToOpportunitiesMap(HashMap<BuildablePoolResult, Long> results) {
        final int[] opportunityArr = results.keySet().stream().mapToInt(BuildablePoolResult::getDoomGenerated).toArray();
        final int minOpportunities = Arrays.stream(opportunityArr).min().orElse(0);
        final int maxOpportunities = Arrays.stream(opportunityArr).max().orElse(0);
        return IntStream.rangeClosed(minOpportunities, maxOpportunities)
                .boxed()
                .collect(Collectors.toMap(integer -> integer, integer -> results
                        .entrySet()
                        .stream()
                        .filter(rollResultBuilderLongEntry -> rollResultBuilderLongEntry.getKey().getDoomGenerated() == integer)
                        .mapToLong(Map.Entry::getValue)
                        .reduce(0, Math::addExact)));
    }

    /**
     * Creates an embed based on the statistics
     *
     * @return The embed to be sent
     */
    public EmbedBuilder generateEmbed() {
        return results.map(this::createStats).getOrElse(OVERLOAD_EMBED);
    }

    private EmbedBuilder createStats(HashMap<BuildablePoolResult, Long> buildablePoolResultLongHashMap) {
        Long sampleSpace = buildablePoolResultLongHashMap.values().stream().reduce(0L, Math::addExact);
        EmbedBuilder statsEmbed = new EmbedBuilder();
        List<ResultStrategy> resultStrategies = List.of(
                new SumStrategy(),
                new DifficultyStrategy(),
                new DoomStrategy(),
                new OpportunityStrategy(),
                new StatisticsStrategy()
        );
        resultStrategies.stream()
                .flatMap(resultStrategy -> resultStrategy.executeStrategy(results.get(), sampleSpace).stream())
                .forEach(embedField -> statsEmbed.addInlineField(embedField.getTitle(), embedField.getContent()));
        return statsEmbed;
    }

    /**
     * Generates the result for the dice rolls by creating a Hashmap of unique getResults results to int. Dropped dice are not considered for equality.
     *
     * @return A Hashmap of all the possible results mapped to the number of times it occurs
     */
    private HashMap<BuildablePoolResult, Long> generateResultsMap(DicePoolBuilder dicePool) {

        HashMap<BuildablePoolResult, Long> rollResultOccurrences = new HashMap<>();
        //Create n getResults result objects for each face of the die
        rollResultOccurrences = processNormalDice(rollResultOccurrences, dicePool);
        rollResultOccurrences = processPlotDice(rollResultOccurrences, dicePool);
        rollResultOccurrences = processKeptDice(rollResultOccurrences, dicePool);
        rollResultOccurrences = processChaosDice(rollResultOccurrences, dicePool);
        rollResultOccurrences = processFlatBonus(rollResultOccurrences, dicePool);
        return rollResultOccurrences;
    }

    private HashMap<BuildablePoolResult, Long> processFlatBonus(HashMap<BuildablePoolResult, Long> rollResultOccurrences, DicePoolBuilder dicePool) {
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

    private static int getDiceValue(DicePoolBuilder dicePool, Integer diceFacets, int rolledValue) {
        return rolledValue == diceFacets && dicePool.isDevastating() ? rolledValue * 2 : rolledValue;
    }

    private HashMap<BuildablePoolResult, Long> processChaosDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences, DicePoolBuilder dicePool) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all chaos dice
        for (Integer chaosDice : dicePool.getChaosDice()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, chaosDice)
                        .map(operand -> operand * -1)
                        .mapToObj(i -> new FastRollResult(dicePool.getDiceKept()).addChaosDice(i))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            } else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, chaosDice)
                        .map(operand -> operand * -1)
                        .mapToObj(key::addChaosDice)
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

    private HashMap<BuildablePoolResult, Long> processKeptDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences, DicePoolBuilder dicePool) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all kept dice
        for (Integer keptDice : dicePool.getKeptDice()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, keptDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getDiceKept()).addKeptDice(getDiceValue(dicePool, keptDice, i)))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            } else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, keptDice)
                        .mapToObj(diceValue -> key.addKeptDice(getDiceValue(dicePool, keptDice, diceValue)))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

    private HashMap<BuildablePoolResult, Long> processPlotDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences, DicePoolBuilder dicePool) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all the kept dice
        for (Integer plotDice : Stream.concat(dicePool.getPlotDice().stream(), dicePool.getEnhancedDice().stream()).toList()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, plotDice)
                        .mapToObj(i -> new FastRollResult(dicePool.getDiceKept()).addPlotDice(getPlotDieValue(plotDice, i, dicePool.getPlotDice().size() > 1, dicePool.isDevastating())))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            } else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, plotDice)
                        .mapToObj(i -> key.addPlotDice(getPlotDieValue(plotDice, i, dicePool.getPlotDice().size() > 1, dicePool.isDevastating())))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

    /**
     * Gets the value of a rolled plot die
     *
     * @param plotDice    The number of facets in the plot die
     * @param rolledValue The number rolled
     * @param devastating If the value of the die should be doubled if it rolls max
     * @return The result of a plot die, which has a minimum of half its facets if there is only one plot die being rolled
     */
    private int getPlotDieValue(Integer plotDice, int rolledValue, boolean moreThanOnePlotDice, boolean devastating) {
        int finalValue = !moreThanOnePlotDice ? Math.max(rolledValue, plotDice / 2) : rolledValue;
        if (devastating && rolledValue == plotDice) {
            finalValue = finalValue * 2;
        }
        return finalValue;
    }

    private HashMap<BuildablePoolResult, Long> processNormalDice(HashMap<BuildablePoolResult, Long> rollResultOccurrences, DicePoolBuilder dicePool) {
        HashMap<BuildablePoolResult, Long> newMap = new HashMap<>(rollResultOccurrences);
        // Loop through all the kept dice
        for (Integer regularDice : dicePool.getRegularDice()) {
            HashMap<BuildablePoolResult, Long> tempMap = new HashMap<>();
            // Create n BuildablePoolResult Objects with each possible outcomes of the dice
            // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
            if (newMap.isEmpty()) {
                IntStream.rangeClosed(1, regularDice)
                        .mapToObj(rolledValue -> new FastRollResult(dicePool.getDiceKept()).addRegularDice(getDiceValue(dicePool, regularDice, rolledValue)))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + 1 : 1));
            } else {
                newMap.forEach((key, value) -> IntStream.rangeClosed(1, regularDice)
                        .mapToObj(diceValue -> key.addRegularDice(getDiceValue(dicePool, regularDice, diceValue)))
                        .forEach(fastRollResult -> tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + value : value)));
            }
            newMap = tempMap;
        }
        return newMap;
    }

}
