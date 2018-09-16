package logic;

import discord.TwoDee;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.*;

public class DiceRoller {

    private ArrayList<Integer> regDice = new ArrayList<>();
    private ArrayList<Integer> plotDice = new ArrayList<>();

    public int getDoom() {
        return doom;
    }

    //The amount of doom generated by this dice roll
    private int doom;

    public DiceRoller(String content) {
        //Split up content
        ArrayList<String> args = new ArrayList<>(Arrays.asList(content.split(" ")));
        args.remove("~r");
        //Split dice into regular dice and plot dice
        DiceParameterHandler diceParameterHandler = new DiceParameterHandler(args, regDice, plotDice);
        diceParameterHandler.addDiceToPools();
    }

    public EmbedBuilder generateResults(MessageAuthor author) {
        ArrayList<Integer> diceResults = new ArrayList<>();
        ArrayList<Integer> pdResults = new ArrayList<>();
        Random random = new Random();
        //Roll the dice
        rollDice(diceResults, pdResults, random);

        //Get top two and dropped dice
        ArrayList<Integer> topTwo = new ArrayList<>();
        ArrayList<Integer> dropped = new ArrayList<>();
        getTopTwo(diceResults, topTwo, dropped);
        int plotResult = getPlotResult(pdResults);
        //Sum up total
        int total = getTotal(topTwo, plotResult);
        //Build embed
        return buildResultEmbed(author, diceResults, pdResults, random, topTwo, dropped, total);
    }

    private int getPlotResult(ArrayList<Integer> pdResults) {
        int plotResult = 0;
        if (!pdResults.isEmpty()) {
            for (int pDice : pdResults) {
                plotResult += pDice;
            }
        }
        return plotResult;
    }

    private EmbedBuilder buildResultEmbed(MessageAuthor author, ArrayList<Integer> diceResults, ArrayList<Integer> pdResults, Random random, ArrayList<Integer> topTwo, ArrayList<Integer> dropped, int total) {
        return new EmbedBuilder()
                .setTitle(TwoDee.getRollTitleMessage())
                .setAuthor(author)
                .setColor(new Color(random.nextFloat(), random.nextFloat(), random.nextFloat()))
                .addField("Regular dice", formatResults(diceResults, author), true)
                .addField("Picked", replaceBrackets(topTwo.toString()), true)
                .addField("Dropped", replaceBrackets(dropped.toString()), true)
                .addField("Plot dice", replaceBrackets(pdResults.toString()), true)
                .addField("Total", String.valueOf(total), true);
    }

    //Bold 1s to show total doom generated. Runs doom increasing method afterwards.
    private String formatResults(ArrayList<Integer> s, MessageAuthor author) {
        String resultString = "";
        if (s.size() > 1){
            for (int i = 0; i < s.size() - 1; i++){
                if (s.get(i) == 1){
                    resultString += "**1**, ";
                }
                else {
                    resultString += s.get(i) + ", ";
                }
            }
            if (s.get(s.size() - 1) == 1){
                resultString += "**1**";
            }
            else {
                resultString += s.get(s.size() - 1);
            }
        }
        else if (s.size() == 1){
            if (s.get(0) == 1){
                resultString += "**1**";
            }
            else {
                resultString += s.get(0);
            }
        }
        else {
            return "*none*";
        }
        return resultString;
    }

    // Username is stored as <@!140973544891744256>
    public EmbedBuilder addPlotPoints(MessageAuthor author, DiscordApi api) {
        PlotPointHandler handler = new PlotPointHandler("~p <@!" + author.getIdAsString() + "> add 1", author, api);
        if (doom != 0){
            return handler.processCommandType();
        }
        else {
            return null;
        }
    }

    public EmbedBuilder addDoom(int doomVal){
        DoomWriter doomWriter = new DoomWriter();
        return doomWriter.addDoom(doomVal);
    }

    private int getTotal(ArrayList<Integer> topTwo, int plotResult) {
        return topTwo.stream().mapToInt(Integer::intValue).sum() + plotResult;
    }

    private void getTopTwo(ArrayList<Integer> diceResults, ArrayList<Integer> topTwo, ArrayList<Integer> dropped) {
        if (diceResults.size() == 1) {
            topTwo.add(diceResults.get(0));
        } else {
            //Sort ArrayList in descending order
            ArrayList<Integer> sortedResults = new ArrayList<>(diceResults);
            Collections.sort(sortedResults);
            Collections.reverse(sortedResults);
            for (int i = 0; i < sortedResults.size(); i++) {
                if (i < 2) {
                    topTwo.add(sortedResults.get(i));
                } else {
                    dropped.add(sortedResults.get(i));
                }
            }
        }
    }

    //Roll all of the dice. Plot dice have a minimum value of its maximum roll/2
    private void rollDice(ArrayList<Integer> diceResults, ArrayList<Integer> pdResults, Random random) {
        //Roll dice
        for (Integer normalDice : regDice) {
            int diceVal = random.nextInt(normalDice) + 1;
            diceResults.add(diceVal);
            if (diceVal == 1){
                doom++;
            }
        }
        //A plot die's minimum value is its number of faces / 2
        for (Integer pDice : plotDice) {
            int pValue = random.nextInt(pDice) + 1;
            if (pValue < pDice / 2){
                pValue = pDice / 2;
            }
            pdResults.add(pValue);
        }
    }

    //Replaces brackets in the string. If the string is blank, returns "none" in italics
    private String replaceBrackets(String s) {
        String newStr = s.replaceAll("\\[", "").replaceAll("\\]", "");
        if (newStr.equals("")) {
            return "*none*";
        }
        return newStr;
    }

}
