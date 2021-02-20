package sheets;

import com.google.api.services.sheets.v4.model.ValueRange;
import logic.UserInfo;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * This class handles the getting and setting of the plot points from a player
 */
public class PlotPointManager {

    private PlotPointManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the plot points of a player using Google's provided write method and the player's document ID. Returns the
     * number of plot points the user has
     *
     * @param target The Discord ID of the user
     * @param number The new number of plot points
     * @return The new number of plot points
     */
    public static int setPlotPoints(String target, int number) {
        SheetsManager.writePlotPoints(number, new UserInfo().getDocID(target));
        return number;
    }

    //Grabs plot point values from SheetsQuickStart using a player's document ID
    public static int getPlotPoints(String target) {
        try {
            ValueRange range = SheetsManager.getPlotPointCell(new UserInfo().getDocID(target));
            List<List<Object>> values = range.getValues();
            List<Object> valueList = values.get(0);
            return Integer.parseInt(String.valueOf(valueList.get(0)));
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
