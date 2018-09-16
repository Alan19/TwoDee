package logic;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import sheets.PPManager;

import java.io.FileNotFoundException;

/**
 * This class adds plot points, subtracts plot points, and sets plot points for players. This class will also keep
 * track of doom points
 */
public class PlotPointManager {

    private PPManager ppManager = new PPManager();
    private UserInfo userInfo = new UserInfo();

    public PlotPointManager() throws FileNotFoundException {
    }

    //2 args : ~p [add|sub|addall|set] number
    //3 args: ~p name [add|sub|addall|set] number
//    @Command(aliases = {"~p", "~plot", "~plotpoints"}, description = "Manages plot points and doom points", usage = "~p <name> <[add|sub|addall|set]> [number]")
    private void processCommandType(String[] args, String message, MessageAuthor messageAuthor, TextChannel textChannel) {
        String commandType = "";
        String target = "";
        int number = 1;
        //Get user's plot points
        if (args.length == 1){
            ppManager.getPlotPoints(args[0]);

        }
        //No user specified
        else if (args.length == 2){
            target = messageAuthor.getIdAsString();
            commandType = args[0];
            number = Integer.parseInt(args[1]);
        }
        //User specified
        else {
            target = args[0];
            commandType = args[1];
            number = Integer.parseInt(args[2]);
        }
        switch (commandType){
            case "add":
                addPlotPoints(target, number);
                break;

            case "sub":
                addPlotPoints(target, number * -1);
                break;

            case "addall":
                addPlotPointsToAll(number);
                break;

            case "set":
                setPlotPoints(target, number);
                break;

            default:
                textChannel.sendMessage("Invalid command");
                break;
        }
    }

    private void setPlotPoints(String target, int number) {
        ppManager.setPlotPoints(target, number);
    }

    private void addPlotPointsToAll(int number) {
        for (String ID : userInfo.getUsers()) {
            ppManager.setPlotPoints(ID, ppManager.getPlotPoints(ID) + number);
        }
    }

    private void addPlotPoints(String target, int number) {
        ppManager.setPlotPoints(target, ppManager.getPlotPoints(target + number));
    }

}
