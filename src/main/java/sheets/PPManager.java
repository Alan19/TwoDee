package sheets;

import com.google.api.services.sheets.v4.model.ValueRange;
import logic.UserInfo;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class PPManager implements IPlotPointMethods{

    //Sets the plot points of a player using Google's provided write method and the player's document ID
    @Override
    public int setPlotPoints(String target, int number) {
        SheetsQuickstart.writeSomething(number, new UserInfo().getDocID(target));
        return number;
    }

    //Grabs plot point values from SheetsQuickStart using a player's document ID
    @Override
    public int getPlotPoints(String target) {
        try {
            ValueRange range = SheetsQuickstart.getPlotPointCell(new UserInfo().getDocID(target));
            List<List<Object>> values = range.getValues();
            List<Object> valueList = values.get(0);
            return Integer.parseInt(String.valueOf(valueList.get(0)));
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
