package logic.statisticstates.resultvisitors;

import logic.DiceResult;
import logic.EmbedField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifficultyVisitor implements ResultVisitor {

    private Map<Integer, String> difficultyMap = new HashMap<>();
    private Map<Integer, Integer> hitSuccess = new HashMap<>();
    private Map<Integer, Integer> hitExtraordinarySuccess = new HashMap<>();
    private int totalCombinations = 0;

    /**
     * Function to generate difficulty value based on difficulty level
     * @param level The difficulty level. Must be greater than 0
     * @return The difficulty that needs to be rolled to make that difficulty level
     */
    private int generateStageDifficulty(int level){
        return 3 + (level - 1) * 4;
    }

    /**
     * Function to generate difficulty extraordinary success value based on difficulty level
     * @param level The difficulty level. Must be greater than 0
     * @return The difficulty that needs to be rolled to make that difficulty level
     */
    private int generateStageExtraordinaryDifficulty(int level){
        return 10 + (level - 1) * 4;
    }

    public DifficultyVisitor(){
        difficultyMap.put(1, "Easy");
        difficultyMap.put(2, "Average");
        difficultyMap.put(3, "Hard");
        difficultyMap.put(4, "Formidable");
        difficultyMap.put(5, "Heroic");
        difficultyMap.put(6, "Incredible");
        difficultyMap.put(7, "Ridiculous");
        difficultyMap.put(8, "Impossible");

        for (int i = 1; i <= 8; i++){
            hitSuccess.put(i, 0);
            hitExtraordinarySuccess.put(i, 0);
        }
    }

    @Override
    public void visit(DiceResult result) {
        int resultVal = result.getResult();
        for (Integer level : difficultyMap.keySet()) {
            if (resultVal >= generateStageDifficulty(level)){
                hitSuccess.put(level, hitSuccess.get(level) + 1);
            }
            if (resultVal >= generateStageExtraordinaryDifficulty(level)){
                hitExtraordinarySuccess.put(level, hitExtraordinarySuccess.get(level) + 1);
            }
        }
        totalCombinations++;
    }

    @Override
    public List<EmbedField> getEmbedField() {
        ArrayList<EmbedField> embedFields = new ArrayList<>();
        //Make field for normal success
        EmbedField regDifficultyField = new EmbedField();
        regDifficultyField.setTitle("Chance to hit");
        for (Map.Entry<Integer, Integer> difficulty: hitSuccess.entrySet()) {
            regDifficultyField.appendContent(difficultyMap.get(difficulty.getKey()) + ": " + generatePercentage(difficulty.getValue(), totalCombinations) + "\n");
        }
        embedFields.add(regDifficultyField);

        EmbedField extraordinaryDifficultyField = new EmbedField();
        extraordinaryDifficultyField.setTitle("Chance to hit extraordinary");
        for (Map.Entry<Integer, Integer> extraOrdinaryDifficulty: hitExtraordinarySuccess.entrySet()) {
            extraordinaryDifficultyField.appendContent(difficultyMap.get(extraOrdinaryDifficulty.getKey()) + ": " + generatePercentage(extraOrdinaryDifficulty.getValue(), totalCombinations) + "\n");
        }
        embedFields.add(extraordinaryDifficultyField);
        return embedFields;
    }
}
