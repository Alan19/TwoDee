package statistics.resultvisitors;

import logic.EmbedField;
import statistics.RollResultBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DoomVisitor implements ResultVisitor {

    private int sum = 0;
    private TreeMap<Integer, Long> doomMap = new TreeMap<>();

    @Override
    public void visit(RollResultBuilder result, Long occurrences) {
        int doom = result.getDoom();
        doomMap.put(doom, doomMap.getOrDefault(doom, (long) 0) + occurrences);
        sum += occurrences;
    }

    @Override
    public List<EmbedField> getEmbedField() {
        ArrayList<EmbedField> embedFields = new ArrayList<>();
        EmbedField doomField = new EmbedField();
        doomField.setTitle("Chance to generate doom");
        for (Map.Entry<Integer, Long> entry : doomMap.entrySet()) {
            doomField.appendContent(entry.getKey() + ": " + generatePercentage(entry.getValue(), sum) + "\n");
        }
        embedFields.add(doomField);
        return embedFields;
    }
}
