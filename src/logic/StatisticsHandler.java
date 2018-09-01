package logic;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StatisticsHandler {

    private ArrayList<DiceResult> resultList = new ArrayList<>();
    private HashMap<Object, Object> statisticsMap;

    public StatisticsHandler(String message) {
        //Add all of the dice to the arraylists based on dice type
        ArrayList<Integer> diceList = new ArrayList<>();
        ArrayList<Integer> plotDice = new ArrayList<>();
        for (String dice: message.split(" ")) {
            if (dice.contains("pd")){
                plotDice.add(Integer.parseInt(dice.replaceAll("[a-zA-Z]", "")));
            }
            else if (dice.contains("d")){
                String test = dice.replaceAll("[a-zA-Z]", "");
                diceList.add(Integer.parseInt(test));
            }
        }
        generateResults(diceList);
        
        HashMap<Integer, Integer> resultHash = generateStatisticsTable();
        HashMap<Integer, Double> statisticsMap = generateProbabilityHash(diceList, resultHash);


    }

    private HashMap<Integer,Double> generateProbabilityHash(ArrayList<Integer> diceList, HashMap<Integer, Integer> resultHash) {
        int totalCombos = 1;
        for (int combo : diceList) {
            totalCombos *= combo;
        }
        HashMap<Integer, Double> probHash = new HashMap<>();
        for (Object o : resultHash.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            probHash.put((Integer) pair.getKey(), (Integer)pair.getValue() / (double)totalCombos);
        }
        return probHash;
    }

    public static void main(String[] args) {
        new StatisticsHandler("d10 d12 d12");
    }

    private HashMap<Integer, Integer> generateStatisticsTable() {
        //Add information to a <Integer, Double> hashmap
        HashMap<Integer, Integer> resultHash = new HashMap<>();
        for (DiceResult result: resultList) {
            int key = result.getResult();
            if (!resultHash.containsKey(key)){
                resultHash.put(key, 1);
            }
            else {
                resultHash.put(key, resultHash.get(key) + 1);
            }
        }
        return resultHash;
    }

    //Prep method for generateResults to copy the dice list to prevent it from being modified
    public void generateResults(ArrayList<Integer> diceList){
        ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            generateResults(diceListCopy, new DiceResult());
//        System.out.println(resultList.get(182).getResult());
    }

    //Recursive method to generate an ArrayList of results
    public void generateResults(ArrayList<Integer> diceList, DiceResult result){
        if (diceList.isEmpty()){
            resultList.add(result);
        }
        else {
            ArrayList<Integer> diceListCopy = new ArrayList<>(diceList);
            int diceNum = diceListCopy.remove(0);
            for (int i = 1; i <= diceNum; i++){
                DiceResult resultCopy = result.copy();
                resultCopy.addDiceToResult(i);
                generateResults(diceListCopy, resultCopy);
            }
        }
    }

    public String generateStatistics(){
        String result = generateIndividualStatistics();
        String difficulties = generateMeetingDifficulty();

        return result + "\n" + difficulties;
    }

    //Loop through HashMap, check for keys greater than difficulty, and sum it up.
    private String generateMeetingDifficulty() {
        StringBuilder result = new StringBuilder();
        DecimalFormat df = new DecimalFormat("0.00");
        String[] difficultyNames = {"Easy", "Average", "Hard", "Formidable", "Heroic", "Incredible", "Ridiculous", "Impossible"};
        int[] difficulty = {3, 7, 11, 15, 19, 23, 27, 31};
        double[] prob = new double[8];
        String[] probString = new String[8];
        for (int i = 0; i < difficulty.length; i++) {
            prob[i] = 0.0;
            for (Object o : statisticsMap.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                if (difficulty[i] <= (Integer) pair.getKey()){
                    prob[i] += (double) pair.getValue();
                }
            }
            probString[i] = df.format(prob[i]);
        }
        for (int i = 0; i < difficultyNames.length; i++){
            result.append(difficultyNames[i]).append(": ").append(probString[i]).append("%\n");
        }
        return result.toString();
    }

    @NotNull
    private String generateIndividualStatistics() {
        //Iterate through HashMap to generate message
        StringBuilder result = new StringBuilder();
        for (Object o : statisticsMap.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            DecimalFormat df = new DecimalFormat("0.00");
            String roundedChance = df.format(Double.valueOf(pair.getValue().toString()));
            result.append(pair.getKey()).append(": ").append(roundedChance).append("%\n");
        }
        return result.toString();
    }
}
