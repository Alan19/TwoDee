package logic;

import network.AnydiceFetcher;
import network.Main;

import java.util.ArrayList;
import java.util.Arrays;

public class StatisticsHandler {

    private AnydiceFetcher fetcher;

    public StatisticsHandler(String message) {
        //Add all of the dice to the arraylist and remove command invocation
        ArrayList<String> diceList = new ArrayList<>(Arrays.asList(message.split(" ")));
        diceList.remove("~s");
        String command = Main.generateCommand(diceList);
        fetcher = new AnydiceFetcher(command);
    }

    public String getFetcherJSON() {
        return fetcher.getResponseJson();
    }
}
