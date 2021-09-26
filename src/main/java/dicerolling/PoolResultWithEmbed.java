package dicerolling;

import com.google.common.collect.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.Arrays;

public interface PoolResultWithEmbed extends PoolResult {
    EmbedBuilder getResultEmbed();

    /**
     * Generates a string that contains the tier and extraordinary tier hit by the getResults
     *
     * @return The tiers and extraordinary tier hit by the getResults, if any
     */
    default String getTierHit() {
        Pair<String, Range<Integer>> noneRange = Pair.of("None", Range.lessThan(3));
        Pair<String, Range<Integer>> easyRange = Pair.of("Easy", Range.closedOpen(3, 7));
        Pair<String, Range<Integer>> averageRange = Pair.of("Average", Range.closedOpen(7, 11));
        Pair<String, Range<Integer>> hardRange = Pair.of("Hard", Range.closedOpen(11, 15));
        Pair<String, Range<Integer>> formidableRange = Pair.of("Formidable", Range.closedOpen(15, 19));
        Pair<String, Range<Integer>> heroicRange = Pair.of("Heroic", Range.closedOpen(19, 23));
        Pair<String, Range<Integer>> incredibleRange = Pair.of("Incredible", Range.closedOpen(23, 27));
        Pair<String, Range<Integer>> ridiculousRange = Pair.of("Ridiculous", Range.closedOpen(27, 31));
        Pair<String, Range<Integer>> impossibleRange = Pair.of("Impossible", Range.atLeast(31));
        Pair<String, Range<Integer>>[] difficultyRanges = new Pair[]{impossibleRange, ridiculousRange, incredibleRange, heroicRange, formidableRange, hardRange, averageRange, easyRange, noneRange};
        int total = getTotal();
        StringBuilder difficultyString = new StringBuilder();
        Arrays.stream(difficultyRanges)
                .filter(difficultyRange -> difficultyRange.getRight().contains(total))
                .findFirst()
                .ifPresent(difficultyRange -> difficultyString.append(difficultyRange.getLeft()).append(""));
        Arrays.stream(difficultyRanges, 0, difficultyRanges.length - 1)
                .filter(stringRangePair -> stringRangePair.getRight().contains(total - 7))
                .findFirst().ifPresent(stringRangePair -> difficultyString.append(", Extraordinary ").append(stringRangePair.getLeft()));
        return difficultyString.toString();
    }

    int getTotal();
}
