package statistics;

import dicerolling.DiceParameterHandler;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A class that checks if the amount of dice being used is too much for the bot to generate statistics in a reasonable
 * amount of time
 */
public class ScanDice implements StatisticsState {
    private String message;
    private int keptDice = 2;

    public ScanDice(String message) {
        this.message = message;
    }

    @Override
    public void process(StatisticsContext context) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(message.split(" ")));
        PoolOptions poolOptions = new PoolOptions();
        DiceParameterHandler diceParameterHandler = new DiceParameterHandler(args, poolOptions);
        diceParameterHandler.addDiceToPools();

        if (!poolOptions.validPool()) {
            context.setState(new GenerateNoDiceMessage());
        }
        else if (getTotalCombos(poolOptions) > Math.pow(12, 17)) {
            context.setState(new GenerateOverloadMessage());
        }
        else {
            context.setState(new GenerateStatistics(poolOptions));
        }
    }

    //Get the total number of combinations by finding the product of all of the number of faces in all of the dice
    private int getTotalCombos(PoolOptions poolOptions) {
        int totalCombos = 1;
        ArrayList<Integer> regularDice = poolOptions.getRegularDice();
        for (int combo : regularDice) {
            totalCombos *= combo;
        }

        /*Plot dice have a minimum value of the die size / 2
        //This means that you need to divide it by two and add one to get the number of combinations from it if the
        value is greater than 2 */
        for (int pdCombo : poolOptions.getPlotDice()) {
            if (pdCombo > 2) {
                totalCombos *= pdCombo / 2 + 1;
            }
        }

        //Kept die is treated the same as normal dice
        for (int combo : poolOptions.getKeptDice()) {
            totalCombos *= combo;
        }
        return totalCombos;
    }
}
