package dicerolling;

import java.util.ArrayList;

/**
 * Class that processes a collection of strings and adds them to a pool
 * e.g. d8, pd6, -8, +3, kd4
 */
public class DiceParameterHandler {
    private ArrayList<Integer> plotDice;
    private ArrayList<Integer> regDice;
    private ArrayList<String> args;
    private ArrayList<Integer> flat;
    private ArrayList<Integer> keptDice;

    public DiceParameterHandler(ArrayList<String> args, ArrayList<Integer> regDice, ArrayList<Integer> plotDice, ArrayList<Integer> flat, ArrayList<Integer> keptDice) {
        this.args = args;
        this.regDice = regDice;
        this.plotDice = plotDice;
        this.flat = flat;
        this.keptDice = keptDice;
    }

    //Loop through command parameters and check for dice and add them to the appropriate dice type list
    public void addDiceToPools() {
        for (String arg : args) {
            String argCopy = arg;
            String numDice = "";

            //Find number of dice being rolled
            while (Character.isDigit(argCopy.charAt(0))) {
                numDice += argCopy.charAt(0);
                argCopy = argCopy.substring(1);
            }
            //Check for dice type
            if (argCopy.contains("pd")) {
                addToPool(argCopy, numDice, plotDice);
            } else if (argCopy.contains("kd")) {
                addToPool(argCopy, numDice, keptDice);
            } else if (argCopy.contains("d")) {
                addToPool(argCopy, numDice, regDice);
            } else if (argCopy.contains("+")) {
                addToPool(argCopy, "1", flat);
            } else if (argCopy.contains("-")) {
                addToPool(argCopy, "1", flat);
            }

        }
    }

    //Check if a parameter contains multiple dice and if there is, add multiple dice to the list
    private void addToPool(String argCopy, String numDice, ArrayList<Integer> pool) {
        //Remove all letters so only numbers remain to get the dice value
        int diceVal = Integer.parseInt(argCopy.replaceAll("[a-zA-Z]", ""));

        //If there are multiple dice being rolled, add all of them to the pool. Otherwise, only add one.
        if ("".equals(numDice)) {
            pool.add(diceVal);
        } else {
            for (int i = 0; i < Integer.parseInt(numDice); i++) {
                pool.add(diceVal);
            }
        }
    }
}