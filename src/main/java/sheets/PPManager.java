package sheets;

import com.google.api.services.sheets.v4.model.ValueRange;
import logic.UserInfo;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class PPManager implements IPlotPointMethods{
    @Override
    public int setPlotPoints(String target, int number) {
        SheetsQuickstart.writeSomething(number, new UserInfo().getDocID(target));
        return number;
    }

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

    public static void main(String[] args) {
        PPManager ppManager = new PPManager();
        int lance_p = ppManager.getPlotPoints("140973544891744256");
        System.out.println(lance_p);
        int alanNewP = ppManager.setPlotPoints("140973544891744256", 14);
        System.out.println(alanNewP);
    }
}
