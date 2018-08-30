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
        StringBuilder result = generateIndividualStatistics();
        result.append(generateMeetingDifficulty());

        return result.toString();
    }

    private String generateMeetingDifficulty() {
        return null;
    }

    @NotNull
    private StringBuilder generateIndividualStatistics() {
        //Iterate through HashMap to generate message
        StringBuilder result = new StringBuilder();
        for (Object o : statisticsMap.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            DecimalFormat df = new DecimalFormat("0.00");
            String roundedChance = df.format(Double.valueOf(pair.getValue().toString()));
            result.append(pair.getKey()).append(": ").append(roundedChance).append("%\n");
        }
        return result;
    }
}
