package logic;

import java.util.ArrayList;
import java.util.Arrays;

public class DiceRoller {

    ArrayList<Integer> regDice = new ArrayList<>();
    ArrayList<Integer> plotDice = new ArrayList<>();

    public DiceRoller(String content) {
        //Split up content
        ArrayList<String> args = new ArrayList<>(Arrays.asList(content.split(" ")));
        args.remove("~r");
        //Split dice into regular dice and plot dice
        for (String arg: args) {
            String argCopy = arg;
            String numDice = "";

            //Find number of dice being rolled
            while (Character.isDigit(argCopy.charAt(0))){
                numDice += argCopy.charAt(0);
                argCopy = argCopy.substring(1);
            }
            //Check for dice type
            if (argCopy.contains("pd")){
                addToPool(argCopy, numDice, plotDice);
            }
            else if (argCopy.contains("d")){
                addToPool(argCopy, numDice, regDice);
            }
        }
    }

    private void addToPool(String argCopy, String numDice, ArrayList<Integer> pool) {
        //Remove all letters so only numbers remain to get the dice value
        int diceVal = Integer.parseInt(argCopy.replaceAll("[a-zA-Z]", ""));

        //If there are multiple dice being rolled, add all of them to the pool. Otherwise, only add one.
        if (numDice.equals("")){
            pool.add(diceVal);
        }
        else {
            for (int i = 0; i < Integer.parseInt(numDice); i++){
                pool.add(diceVal);
            }
        }
    }

    public String generateResults(){
        ArrayList<Integer> diceResults = new ArrayList<>();
        ArrayList<Integer> pdResults = new ArrayList<>();
        for (Integer normalDice : regDice) {
            diceResults.add((int) Math.ceil(Math.random() * normalDice));
        }
        for (Integer pDice : plotDice) {
            pdResults.add((int) Math.ceil(Math.random() * pDice));
        }
        System.out.println(diceResults.toString());
        System.out.println(pdResults.toString());
        return "";
    }

    public static void main(String[] args) {
        DiceRoller diceRoller = new DiceRoller("1d12 2d10 3pd6");
        diceRoller.generateResults();
    }
}
