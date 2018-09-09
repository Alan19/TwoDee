package logic;

import discord.TwoDee;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class DiceRoller {

    private ArrayList<Integer> regDice = new ArrayList<>();
    private ArrayList<Integer> plotDice = new ArrayList<>();

    public DiceRoller(String content) {
        //Split up content
        ArrayList<String> args = new ArrayList<>(Arrays.asList(content.split(" ")));
        args.remove("~r");
        //Split dice into regular dice and plot dice
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

    public EmbedBuilder generateResults(MessageAuthor author) {
        ArrayList<Integer> diceResults = new ArrayList<>();
        ArrayList<Integer> pdResults = new ArrayList<>();
        Random random = new Random();

        //Roll dice
        for (Integer normalDice : regDice) {
            diceResults.add(random.nextInt(normalDice) + 1);
        }
        for (Integer pDice : plotDice) {
            pdResults.add(random.nextInt(pDice) + 1);
        }
        System.out.println("Regular dice: " + diceResults.toString());
        System.out.println("Plot dice: " + pdResults.toString());
        ArrayList<Integer> topTwo = new ArrayList<>();
        ArrayList<Integer> dropped = new ArrayList<>();
        int plotResult = 0;
        if (diceResults.size() == 1) {
            topTwo.add(diceResults.get(0));
        } else {
            //Sort ArrayList in descending order
            ArrayList<Integer> sortedResults = new ArrayList<>(diceResults);
            Collections.sort(sortedResults);
            Collections.reverse(sortedResults);
            for (int i = 0; i < sortedResults.size(); i++) {
                if (i < 2){
                    topTwo.add(sortedResults.get(i));
                }
                else {
                    dropped.add(sortedResults.get(i));
                }
            }
        }
        if (!pdResults.isEmpty()) {
            for (int pDice : pdResults) {
                plotResult += pDice;
            }
        }
        //Sum up total
        int total = topTwo.stream().mapToInt(Integer::intValue).sum() + plotResult;
        //Build embed
        return new EmbedBuilder()
                .setTitle(TwoDee.getRollTitleMessage())
                .setAuthor(author)
                .setColor(new Color(random.nextFloat() , random.nextFloat(), random.nextFloat()))
                .addField("Regular dice", replaceBrackets(diceResults.toString()), true)
                .addField("Picked", replaceBrackets(topTwo.toString()), true)
                .addField("Dropped", replaceBrackets(dropped.toString()), true)
                .addField("Plot dice", replaceBrackets(pdResults.toString()), true)
                .addField("Total", String.valueOf(total), true);
    }

    //Replaces brackets in the string. If the string is blank, returns "none" in italics
    private String replaceBrackets(String s){
        String newStr = s.replaceAll("\\[", "").replaceAll("\\]","");
        if (newStr.equals("")){
            return "*none*";
        }
        return newStr;
    }
}
