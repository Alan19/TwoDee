package logic;

import network.AnydiceFetcher;

import java.util.ArrayList;

public class StatisticsHandler {

    private AnydiceFetcher fetcher;

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
        String command = CommandGenerator.generateCommand(diceList, plotDice);
        fetcher = new AnydiceFetcher(command);
    }

    public String getFetcherJSON() {
        return fetcher.getResponseJson();
    }
}
