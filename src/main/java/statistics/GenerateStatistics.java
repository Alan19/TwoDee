package statistics;

import dicerolling.DicePool;
import dicerolling.FastRollResult;
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
        HashMap<FastRollResult, Long> results = generateResultsHash();
        final int[] resultStream = results.keySet().stream().mapToInt(FastRollResult::getTotal).toArray();
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
        final int[] opportunityArr = results.keySet().stream().mapToInt(FastRollResult::getDoomGenerated).toArray();
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

    /**
     * Creates an embed based on the statistics
     *
     * @param rollToOccurrences   The HashMap that represents the roll mapped to the number of occurrences of the total
     * @param rollToOpportunities The HashMap that maps the number of opportunities to the number of occurrences
     * @param results             The HashMap that contains each possible combination of dice with the number of occurrences of that combination
     * @return The embed to be sent
     */
    private EmbedBuilder generateEmbed(Map<Integer, Long> rollToOccurrences, Map<Integer, Long> rollToOpportunities, HashMap<FastRollResult, Long> results) {
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
    private HashMap<FastRollResult, Long> generateResultsHash() {
        HashMap<FastRollResult, Long> rollResultOccurrences = new HashMap<>();
        rollResultOccurrences.put(new FastRollResult(), 1L);
        //Create n roll result objects for each face of the die
        rollResultOccurrences = processNormalDice(rollResultOccurrences);
        rollResultOccurrences = processPlotDice(rollResultOccurrences);
        rollResultOccurrences = processKeptDice(rollResultOccurrences);
        rollResultOccurrences = processFlatBonus(rollResultOccurrences);
        return rollResultOccurrences;
    }

    private HashMap<FastRollResult, Long> processFlatBonus(HashMap<FastRollResult, Long> rollResultOccurrences) {
        if (!dicePool.getFlatBonuses().isEmpty()) {
            HashMap<FastRollResult, Long> newMap = new HashMap<>(rollResultOccurrences);
            // Loop through all of the kept dice
            for (Integer flatBonus : dicePool.getFlatBonuses()) {
                HashMap<FastRollResult, Long> tempMap = new HashMap<>();
                // Create n FastRollResultObjects with each possible outcomes of the dice
                for (Map.Entry<FastRollResult, Long> entry : rollResultOccurrences.entrySet()) {
                    // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
                    FastRollResult fastRollResult = entry.getKey().addFlatBonus(flatBonus);
                    tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + entry.getValue() : entry.getValue());
                }
                newMap = tempMap;
            }
            return newMap;
        }
        else {
            return rollResultOccurrences;
        }
    }

    private HashMap<FastRollResult, Long> processKeptDice(HashMap<FastRollResult, Long> rollResultOccurrences) {
        if (!dicePool.getKeptDice().isEmpty()) {
            HashMap<FastRollResult, Long> newMap = new HashMap<>(rollResultOccurrences);
            // Loop through all of the kept dice
            for (Integer keptDice : dicePool.getKeptDice()) {
                HashMap<FastRollResult, Long> tempMap = new HashMap<>();
                // Create n FastRollResultObjects with each possible outcomes of the dice
                for (Map.Entry<FastRollResult, Long> entry : rollResultOccurrences.entrySet()) {
                    for (int i = 1; i <= keptDice; i++) {
                        // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
                        FastRollResult fastRollResult = entry.getKey().addKeptDice(i);
                        newMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + entry.getValue() : entry.getValue());
                    }
                }
                newMap = tempMap;
            }
            return newMap;
        }
        else {
            return rollResultOccurrences;
        }
    }

    private HashMap<FastRollResult, Long> processPlotDice(HashMap<FastRollResult, Long> rollResultOccurrences) {
        if (!dicePool.getPlotDice().isEmpty()) {
            HashMap<FastRollResult, Long> newMap = new HashMap<>(rollResultOccurrences);
            // Loop through all of the plot dice
            for (Integer plotDice : dicePool.getPlotDice()) {
                HashMap<FastRollResult, Long> tempMap = new HashMap<>();
                // Create n FastRollResultObjects with each possible outcomes of the dice
                for (Map.Entry<FastRollResult, Long> entry : rollResultOccurrences.entrySet()) {
                    for (int i = 1; i <= plotDice; i++) {
                        // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
                        final int rolledValue = dicePool.getPlotDice().size() > 1 ? i : Math.max(i, plotDice / 2);
                        FastRollResult fastRollResult = entry.getKey().addPlotDice(rolledValue);
                        tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + entry.getValue() : entry.getValue());
                    }
                }
                newMap = tempMap;
            }
            return newMap;
        }
        else {
            return rollResultOccurrences;
        }
    }

    private HashMap<FastRollResult, Long> processNormalDice(HashMap<FastRollResult, Long> rollResultOccurrences) {
        if (!dicePool.getRegularDice().isEmpty()) {
            HashMap<FastRollResult, Long> newMap = new HashMap<>(rollResultOccurrences);
            // Loop through all of the kept dice
            for (Integer regularDice : dicePool.getRegularDice()) {
                HashMap<FastRollResult, Long> tempMap = new HashMap<>();
                for (Map.Entry<FastRollResult, Long> entry : newMap.entrySet()) {
                    // Create n FastRollResultObjects with each possible outcomes of the dice
                    for (int i = 1; i <= regularDice; i++) {
                        // Add the occurrences to the new map if that result already exists in the new HashMap, else set the value of that result as the number of occurrences
                        FastRollResult fastRollResult = entry.getKey().addRegularDice(i);
                        tempMap.compute(fastRollResult, (result, occurrenceCount) -> occurrenceCount != null ? occurrenceCount + entry.getValue() : entry.getValue());
                    }
                }
                newMap = tempMap;

            }
            return newMap;
        }
        else {
            return rollResultOccurrences;
        }
    }

}
