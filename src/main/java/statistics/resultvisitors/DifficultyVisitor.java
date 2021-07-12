package statistics.resultvisitors;

import util.EmbedField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that visits the result to see which tiers of success was hit. Also used by DiceRoller to calculate the
 * highest tier of success that was hit.
 */
public class DifficultyVisitor implements ResultVisitor {

    private Map<Integer, String> difficultyMap = new HashMap<>();
    private Map<Integer, Long> hitSuccess = new HashMap<>();
    private Map<Integer, Long> hitExtraordinarySuccess = new HashMap<>();
    private long totalCombinations = 0;

    public DifficultyVisitor() {
        difficultyMap.put(1, "Easy");
        difficultyMap.put(2, "Average");
        difficultyMap.put(3, "Hard");
        difficultyMap.put(4, "Formidable");
        difficultyMap.put(5, "Heroic");
        difficultyMap.put(6, "Incredible");
        difficultyMap.put(7, "Ridiculous");
        difficultyMap.put(8, "Impossible");

        for (int i = 1; i <= 8; i++) {
            hitSuccess.put(i, (long) 0);
            hitExtraordinarySuccess.put(i, (long) 0);
        }
    }

    /**
     * Function to generate difficulty value based on difficulty level
     *
     * @param level The difficulty level. Must be greater than 0
     * @return The difficulty that needs to be rolled to make that difficulty level
     */
    public int generateStageDifficulty(int level) {
        return 3 + (level - 1) * 4;
    }

    /**
     * Function to generate difficulty extraordinary success value based on difficulty level
     *
     * @param level The difficulty level. Must be greater than 0
     * @return The difficulty that needs to be rolled to make that difficulty level
     */
    public int generateStageExtraordinaryDifficulty(int level) {
        return 10 + (level - 1) * 4;
    }

    public Map<Integer, String> getDifficultyMap() {
        return difficultyMap;
    }

    @Override
    public void visit(Map<Integer, Long> hashMap) {
        for (Integer difficulty : difficultyMap.keySet()) {
            hitSuccess.put(difficulty, hashMap.entrySet().stream().filter(integerLongEntry -> integerLongEntry.getKey() >= generateStageDifficulty(difficulty)).mapToLong(Map.Entry::getValue).sum());
        }
        for (Integer difficulty : difficultyMap.keySet()) {
            hitExtraordinarySuccess.put(difficulty, hashMap.entrySet().stream().filter(integerLongEntry -> integerLongEntry.getKey() >= generateStageExtraordinaryDifficulty(difficulty)).mapToLong(Map.Entry::getValue).sum());
        }

        totalCombinations = hashMap.values().stream().mapToLong(aLong -> aLong).sum();
    }

    @Override
    public List<EmbedField> getEmbedField() {
        ArrayList<EmbedField> embedFields = new ArrayList<>();
        //Make field for normal success
        EmbedField regDifficultyField = new EmbedField();
        regDifficultyField.setTitle("Chance to hit");
        for (Map.Entry<Integer, Long> difficulty : hitSuccess.entrySet()) {
            regDifficultyField.appendContent(difficultyMap.get(difficulty.getKey()) + ": " + generatePercentage(difficulty.getValue(), totalCombinations) + "\n");
        }
        embedFields.add(regDifficultyField);

        EmbedField extraordinaryDifficultyField = new EmbedField();
        extraordinaryDifficultyField.setTitle("Chance to hit extraordinary");
        for (Map.Entry<Integer, Long> extraOrdinaryDifficulty : hitExtraordinarySuccess.entrySet()) {
            extraordinaryDifficultyField.appendContent(difficultyMap.get(extraOrdinaryDifficulty.getKey()) + ": " + generatePercentage(extraOrdinaryDifficulty.getValue(), totalCombinations) + "\n");
        }
        embedFields.add(extraordinaryDifficultyField);
        return embedFields;
    }
}
