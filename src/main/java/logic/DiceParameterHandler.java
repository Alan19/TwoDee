package logic;

import java.util.ArrayList;

public class DiceParameterHandler {
    private ArrayList<Integer> plotDice;
    private ArrayList<Integer> regDice;
    private ArrayList<String> args;

    public DiceParameterHandler(ArrayList<String> args, ArrayList<Integer> regDice, ArrayList<Integer> plotDice) {
        this.args = args;
        this.regDice = regDice;
        this.plotDice = plotDice;
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
            } else if (argCopy.contains("d")) {
                addToPool(argCopy, numDice, regDice);
            }
        }
    }

    //Check if a parameter contains multiple dice and if there is, add multiple dice to the list
    private void addToPool(String argCopy, String numDice, ArrayList<Integer> pool) {
        //Remove all letters so only numbers remain to get the dice value
        int diceVal = Integer.parseInt(argCopy.replaceAll("[a-zA-Z]", ""));

        //If there are multiple dice being rolled, add all of them to the pool. Otherwise, only add one.
        if (numDice.equals("")) {
            pool.add(diceVal);
        } else {
            for (int i = 0; i < Integer.parseInt(numDice); i++) {
                pool.add(diceVal);
            }
        }
    }
}