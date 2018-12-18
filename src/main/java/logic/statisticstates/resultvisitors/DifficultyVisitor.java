package logic.statisticstates.resultvisitors;

import java.util.HashMap;

public class DifficultyVisitor {

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

    }
}
