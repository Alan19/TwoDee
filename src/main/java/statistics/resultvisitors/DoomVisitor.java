package statistics.resultvisitors;

import util.EmbedField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DoomVisitor implements ResultVisitor {

    private long sum = 0;
    private TreeMap<Integer, Long> doomMap = new TreeMap<>();

    /**
     * Populate doomMap using a Opportunity to Occurrences HashMap
     *
     * @param hashMap An Opportunity to Occurrences HashMap
     */
    @Override
    public void visit(Map<Integer, Long> hashMap) {
        hashMap.forEach((integer, aLong) -> doomMap.put(integer, aLong));
        sum = hashMap.values().stream().mapToLong(value -> value).sum();
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
