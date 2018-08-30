package logic;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class CommandGenerator {

    public static void main(String[] args) {
        if (args.length == 0){
            while (true){
                System.out.println("Enter your dice! Enter 'stop' to quit.");
                Scanner input = new Scanner(System.in);
                //Copy args and add 1 before dice rolls without a number
                ArrayList<String> diceList = new ArrayList<>();
                ArrayList<String> plotDice = new ArrayList<>();
                for (String dice: input.nextLine().split(" ")) {
                    if (dice.contains("pd")){
                        plotDice.add(dice);
                    }
                    else if (dice.contains("d")){
                        diceList.add(dice);
                    }
                }
                if (input.equals("stop")){
                    break;
                }
                diceList = new ArrayList<>(Arrays.asList(input.nextLine().split(" ")));
                generateCommand(diceList, plotDice);
            }
        }
        else {
            //Copy args and add 1 before dice rolls without a number
            ArrayList<String> diceList = new ArrayList<>();
            ArrayList<String> plotDice = new ArrayList<>();
            for (String dice: args) {
                if (dice.contains("pd")){
                    plotDice.add(dice.replace("p", ""));
                }
                else if (dice.contains("d")){
                    diceList.add(dice);
                }
            }
            diceList = new ArrayList<>(Arrays.asList(args));
            generateCommand(diceList, null);
        }
    }



    public static String generateCommand(ArrayList<String> diceList, ArrayList<String> plotDice) {
        //Check for dice that does not have a number before d and add 1 to represent one die
        for (int i = 0; i < diceList.size(); i++) {
            String die = diceList.get(i);
            if (!Character.isDigit(die.charAt(0))) {
                diceList.set(i, "1" + die);
            }
        }
        for (int i = 0; i < plotDice.size(); i++) {
            String die = plotDice.get(i);
            if (!Character.isDigit(die.charAt(0))) {
                plotDice.set(i, "1" + die.replace("p", ""));
            }
        }
        //Generate first line of function
        String line1 = "function: highest N:n of " + generateParameters(diceList) + "{";
        String line2 = "result: {1..N}@[sort {" + generateListLengthAsLetters(diceList.size()) + "}]\n}";
        String line3 = "output [highest 2 of " + generateDiceListAsString(diceList) + "]" + addPlotDie(plotDice);
        String resultCommand = line1 + "\n" + line2 + "\n" + line3;
        System.out.println(resultCommand);
        StringSelection stringSelection = new StringSelection(resultCommand);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        return resultCommand;
    }

    private static String addPlotDie(ArrayList<String> plotDice) {
        String plotDiceString = "";
        for (String pd: plotDice) {
            plotDiceString += "+ " + pd;
        }
        return plotDiceString;
    }

    //Generates the dice being rolled, separated by a space (1d6, 2d10, 3d8)
    private static String generateDiceListAsString(ArrayList<String> diceList) {
        return String.join(" ", diceList);
    }

    /*
    Generate the number of types of dice being rolled (the length of the ArrayList diceList) as a string. For example,
    it will return "A, B, C" if there are 3 types of dice being rolled (1d12, 2d10, 3d6)
     */
    private static String generateListLengthAsLetters(int size) {
        StringBuilder outputLetters = new StringBuilder();
        for (int i = 0; i < size; i++){
            outputLetters.append((char) (65 + i));
            if (i + 1 != size){
                outputLetters.append(", ");
            }
        }
        return outputLetters.toString();
    }

    //Generate the parameter list based on number of dice being rolled. For example, if there are 3 dice being rolled,
    //it will return "A:s B:s C:s"
    private static String generateParameters(ArrayList<String> dice) {
        StringBuilder parameters = new StringBuilder();
        for (int i = 0; i < dice.size(); i++){
            parameters.append((char) (65 + i)).append(":s ");
        }
        return parameters.toString();
    }

}
