package statistics.resultvisitors;

import statistics.DiceResult;
import logic.EmbedField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DoomVisitor implements ResultVisitor{

    private int sum = 0;
    private TreeMap<Integer, Integer> doomMap = new TreeMap<>();

    @Override
    public void visit(DiceResult result) {
        int doom = result.getDoom();
        if (doomMap.containsKey(doom)){
            doomMap.put(doom, doomMap.get(doom) + 1);
        }
        else {
            doomMap.put(doom, 1);
        }
        sum++;
    }

    @Override
    public List<EmbedField> getEmbedField() {
        ArrayList<EmbedField> embedFields = new ArrayList<>();
        EmbedField doomField = new EmbedField();
        doomField.setTitle("Chance to generate doom");
        for (Map.Entry<Integer, Integer> entry: doomMap.entrySet()){
            doomField.appendContent(entry.getKey() + ": " + generatePercentage(entry.getValue(), sum) + "\n");
        }
        embedFields.add(doomField);
        return embedFields;
    }
}
