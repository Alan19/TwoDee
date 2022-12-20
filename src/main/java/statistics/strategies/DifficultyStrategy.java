package statistics.strategies;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Triple;
import rolling.BuildablePoolResult;
import statistics.StatisticsLogic;
import util.EmbedField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class that visits the result to see which tiers of success was hit. Also used by DiceRoller to calculate the
 * highest tier of success that was hit.
 */
public class DifficultyStrategy implements ResultStrategy {

    public static Map<Integer, Triple<String, Integer, Integer>> getDifficultyMap() {
        return ImmutableMap.of(1, Triple.of("Easy", 3, 10),
                2, Triple.of("Average", 7, 14),
                3, Triple.of("Hard", 11, 18),
                4, Triple.of("Formidable", 15, 22),
                5, Triple.of("Heroic", 19, 26),
                6, Triple.of("Incredible", 23, 30),
                7, Triple.of("Ridiculous", 27, 34),
                8, Triple.of("Impossible", 31, 38));
    }

    @Override
    public List<EmbedField> executeStrategy(HashMap<BuildablePoolResult, Long> resultMap, long totalCombinations) {
        Map<Integer, Long> rollResultToOccurancesMap = StatisticsLogic.getRollResultToOccurancesMap(resultMap);
        String regularSuccessString = getDifficultyMap().values().stream()
                .map(difficultyEntry -> "%s: %s".formatted(difficultyEntry.getLeft(), getSuccessPercentage(rollResultToOccurancesMap,
                        difficultyEntry.getMiddle(),
                        totalCombinations)))
                .collect(Collectors.joining("\n"));
        String extraordinarySuccessString = getDifficultyMap().values().stream()
                .map(difficultyEntry -> "%s: %s".formatted(difficultyEntry.getLeft(), getSuccessPercentage(rollResultToOccurancesMap,
                        difficultyEntry.getRight(),
                        totalCombinations)))
                .collect(Collectors.joining("\n"));

        return List.of(new EmbedField("Chance to hit", regularSuccessString),
                new EmbedField("Chance to hit extraordinary", extraordinarySuccessString));
    }

    /**
     * Generates the probability of hitting a specific difficulty tier as a string
     *
     * @param rollResultToOccurrencesMap A map of roll result numbers to the number of occurrences of that result
     * @param difficultyTarget           The difficulty number to hit
     * @param totalCombinations          The total number of roll results
     * @return The probability of hitting a specific difficulty number as a string with a percent at the end
     */
    public String getSuccessPercentage(Map<Integer, Long> rollResultToOccurrencesMap, int difficultyTarget, long totalCombinations) {
        return generatePercentage(rollResultToOccurrencesMap.entrySet().stream()
                .filter(integerLongEntry -> integerLongEntry.getKey() >= difficultyTarget)
                .mapToLong(Map.Entry::getValue).reduce(0, Math::addExact), totalCombinations);
    }
}
