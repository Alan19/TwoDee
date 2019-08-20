package dicerolling;

import statistics.PoolOptions;

import java.util.List;

/**
 * Class that processes a collection of strings and adds them to a pool
 * e.g. d8, pd6, -8, +3, kd4
 */
public class DiceParameterHandler {
    private PoolOptions holder;
    private List<String> args;

    public DiceParameterHandler(List<String> args, PoolOptions poolOptions) {
        this.args = args;
        this.holder = poolOptions;
    }

    //Loop through command parameters and check for dice and add them to the appropriate dice type list
    public void addDiceToPools() {
        for (String arg : args) {
            String argCopy = arg;
            StringBuilder numDice = new StringBuilder();

            //Find number of dice being rolled
            while (Character.isDigit(argCopy.charAt(0))) {
                numDice.append(argCopy.charAt(0));
                argCopy = argCopy.substring(1);
            }
            //Check for dice type
            if (argCopy.contains("pd")) {
                addToPool(argCopy, numDice.toString(), PoolType.PLOT_DICE);
            } else if (argCopy.contains("kd")) {
                addToPool(argCopy, numDice.toString(), PoolType.KEPT_DICE);
            } else if (argCopy.contains("d")) {
                addToPool(argCopy, numDice.toString(), PoolType.DICE);
            } else if (argCopy.contains("+")) {
                addToPool(argCopy, "1", PoolType.FLAT_BONUS);
            } else if (argCopy.contains("-")) {
                addToPool(argCopy, "1", PoolType.FLAT_BONUS);
            } else if (argCopy.contains("**t")) {
                addToPool(argCopy, "1", PoolType.NUMBER_KEPT);
            }
        }
    }

    //Check if a parameter contains multiple dice and if there is, add multiple dice to the list
    private void addToPool(String argCopy, String numDice, PoolType pool) {
        //Remove all letters so only numbers remain to get the dice value
        int diceVal = Integer.parseInt(argCopy.replaceAll("[^0-9-]", ""));
        int intNumDice = numDice.equals("") ? 1 : Integer.parseInt(numDice);
        //If there are multiple dice being rolled, add all of them to the pool. Otherwise, only add one.
        for (int i = 0; i < intNumDice; i++) {
            if (pool == PoolType.FLAT_BONUS) {
                holder.addFlatBonus(diceVal);
            } else if (pool == PoolType.DICE) {
                holder.addDice(diceVal);
            } else if (pool == PoolType.KEPT_DICE) {
                holder.addKeptDice(diceVal);
            } else if (pool == PoolType.PLOT_DICE) {
                holder.addPlotDice(diceVal);
            } else if (pool == PoolType.NUMBER_KEPT) {
                holder.setTop(diceVal);
            }
        }
    }

    private enum PoolType {
        DICE, PLOT_DICE, KEPT_DICE, NUMBER_KEPT, FLAT_BONUS
    }
}