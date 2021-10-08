package rolling;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import statistics.resultvisitors.DifficultyVisitor;

import java.util.Map;

public class SuccessCalculatorEmbed {
    private SuccessCalculatorEmbed() {
        throw new UnsupportedOperationException();
    }

    public static EmbedBuilder generateDifficultyEmbed(String difficulty, int total, MessageAuthor author) {
        final DifficultyVisitor difficultyVisitor = new DifficultyVisitor();
        final Map<Integer, String> difficultyMap = difficultyVisitor.getDifficultyMap();
        final BiMap<String, Integer> invertedDifficultyMap = HashBiMap.create(difficultyMap).inverse();
        String capitalizedDifficulty = difficulty.length() == 0 ? difficulty : difficulty.substring(0, 1).toUpperCase() + difficulty.substring(1).toLowerCase();
        EmbedBuilder enhancementMathEmbed = new EmbedBuilder().setAuthor(author);
        if (invertedDifficultyMap.containsKey(capitalizedDifficulty)) {
            return generateSelectedDifficultyEmbed(total, difficultyVisitor, invertedDifficultyMap, capitalizedDifficulty, enhancementMathEmbed);
        }
        else {
            return generateNextDifficultyCalcEmbed(total, difficultyVisitor, difficultyMap, enhancementMathEmbed);
        }
    }

    public static EmbedBuilder generateSelectedDifficultyEmbed(int total, DifficultyVisitor difficultyVisitor, BiMap<String, Integer> invertedDifficultyMap, String capitalizedDifficulty, EmbedBuilder enhancementMathEmbed) {
        final Integer difficultyLevel = invertedDifficultyMap.get(capitalizedDifficulty);
        int regularDifficulty = difficultyVisitor.generateStageDifficulty(difficultyLevel);
        int extraordinaryDifficulty = difficultyVisitor.generateStageExtraordinaryDifficulty(difficultyLevel);
        if (regularDifficulty > total) {
            enhancementMathEmbed.addField(capitalizedDifficulty, String.valueOf(regularDifficulty - total));
        }
        if (extraordinaryDifficulty > total) {
            enhancementMathEmbed.addField("Extraordinary " + capitalizedDifficulty, String.valueOf(extraordinaryDifficulty - total));
        }
        if (total >= extraordinaryDifficulty) {
            enhancementMathEmbed.setDescription("No need to add any plot points. You hit the extraordinary difficulty!");
        }
        else {
            enhancementMathEmbed.setTitle("Plot points needed to hit:");
        }
        return enhancementMathEmbed;
    }

    public static EmbedBuilder generateNextDifficultyCalcEmbed(int total, DifficultyVisitor difficultyVisitor, Map<Integer, String> difficultyMap, EmbedBuilder enhancementMathEmbed) {
        int regularDifficulty = 0;
        int extraordinaryDifficulty = 0;

        String regularDifficultyTier = "";
        String extraordinaryDifficultyTier = "";
        for (Map.Entry<Integer, String> integerStringEntry : difficultyMap.entrySet()) {
            final int nextStageDifficulty = difficultyVisitor.generateStageDifficulty(integerStringEntry.getKey());
            if (nextStageDifficulty > total) {
                regularDifficulty = nextStageDifficulty;
                regularDifficultyTier = integerStringEntry.getValue();
                break;
            }
        }
        for (Map.Entry<Integer, String> integerStringEntry : difficultyMap.entrySet()) {
            final int nextStageExtraordinaryDifficulty = difficultyVisitor.generateStageExtraordinaryDifficulty(integerStringEntry.getKey());
            if (nextStageExtraordinaryDifficulty > total) {
                extraordinaryDifficulty = nextStageExtraordinaryDifficulty;
                extraordinaryDifficultyTier = integerStringEntry.getValue();
                break;
            }
        }
        if (!regularDifficultyTier.equals("")) {
            enhancementMathEmbed.addField(regularDifficultyTier, String.valueOf(regularDifficulty - total));
        }
        if (!extraordinaryDifficultyTier.equals("")) {
            enhancementMathEmbed.addField(extraordinaryDifficultyTier, String.valueOf(extraordinaryDifficulty - total));
        }
        return enhancementMathEmbed;
    }
}