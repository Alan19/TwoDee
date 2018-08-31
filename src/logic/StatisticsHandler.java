package logic;

import network.AnydiceFetcher;
import org.jetbrains.annotations.NotNull;
import org.json.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StatisticsHandler {

    private AnydiceFetcher fetcher;
    private HashMap<Integer, Double> statisticsMap;

    public StatisticsHandler(String message) {
        //Add all of the dice to the arraylists based on dice type
        ArrayList<String> diceList = new ArrayList<>();
        ArrayList<String> plotDice = new ArrayList<>();
        for (String dice: message.split(" ")) {
            if (dice.contains("pd")){
                plotDice.add(dice);
            }
            else if (dice.contains("d")){
                diceList.add(dice);
            }
        }
        //Generate and fetch results
        String command = CommandGenerator.generateCommand(diceList, plotDice);
        fetcher = new AnydiceFetcher(command);

        //Interpret results
        JSONObject statisticJSON = new JSONObject(fetcher.getResponseJson());
        //data is the array of arrays of roll probabilities
        JSONArray data =  statisticJSON.getJSONObject("distributions").getJSONArray("data").getJSONArray(0);

        //Add information to a <Integer, Double> hashmap
        statisticsMap = new HashMap<>();
        for (int i = 0; i < data.length(); i++){
            JSONArray rollNum = data.getJSONArray(i);
            //Cast integers differently from float percentages
            if (rollNum.get(1) instanceof Double){
                statisticsMap.put((Integer) rollNum.get(0), (Double) rollNum.get(1));
            }
            else {
                statisticsMap.put((Integer) rollNum.get(0), new Double(String.valueOf(rollNum.get(1))));
            }
        }
    }

    public String getFetcherJSON() {
        return fetcher.getResponseJson();
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
