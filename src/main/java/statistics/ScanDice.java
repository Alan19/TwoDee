package statistics;

import logic.DiceParameterHandler;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A class that checks if the amount of dice being used is too much for the bot to generate statistics in a reasonable
 * amount of time
 */
public class ScanDice implements StatisticsState {
    private String message;

    public ScanDice(String message) {
        this.message = message;
    }

    @Override
    public void process(StatisticsContext context) {
        //Add all of the dice to the ArrayLists based on dice type
        ArrayList<Integer> regularDice = new ArrayList<>();
        ArrayList<Integer> plotDice = new ArrayList<>();
        ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
        DiceParameterHandler diceParameterHandler = new DiceParameterHandler(args, regularDice, plotDice);
        diceParameterHandler.addDiceToPools();

        if (regularDice.isEmpty() && plotDice.isEmpty()){
            context.setState(new GenerateNoDiceMessage());
        }
        else if (getTotalCombos(regularDice, plotDice) > Math.pow(12, 6)){
            context.setState(new GenerateOverloadMessage());
        }
        else {
            context.setState(new GenerateStatistics(regularDice, plotDice));
        }
    }

    //Get the total number of combinations by finding the product of all of the number of faces in all of the dice
    private int getTotalCombos(ArrayList<Integer> diceList, ArrayList<Integer> plotDice) {
        int totalCombos = 1;
        for (int combo : diceList) {
            totalCombos *= combo;
        }

        /*Plot dice have a minimum value of the die size / 2
        //This means that you need to divide it by two and add one to get the number of combinations from it if the
        value is greater than 2 */
        for (int pdCombo : plotDice) {
            if (pdCombo > 2){
                totalCombos *= pdCombo / 2 + 1 ;
            }
        }
        return totalCombos;
    }
}
