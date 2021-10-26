package util;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.component.LowLevelComponent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComponentUtils {
    private static final ComponentUtils instance = new ComponentUtils();
    private final Map<Integer, String> difficultyHelperMap;

    private ComponentUtils() {
        difficultyHelperMap = new HashMap<>();
        difficultyHelperMap.put(3, "Easy");
        difficultyHelperMap.put(7, "Average");
        difficultyHelperMap.put(10, "Extraordinary Easy");
        difficultyHelperMap.put(11, "Hard");
        difficultyHelperMap.put(14, "Extraordinary Average");
        difficultyHelperMap.put(15, "Formidable");
        difficultyHelperMap.put(18, "Extraordinary Hard");
        difficultyHelperMap.put(19, "Heroic");
        difficultyHelperMap.put(22, "Extraordinary Formidable");
        difficultyHelperMap.put(23, "Incredible");
        difficultyHelperMap.put(26, "Extraordinary Heroic");
        difficultyHelperMap.put(27, "Ridiculous");
        difficultyHelperMap.put(30, "Extraordinary Incredible");
        difficultyHelperMap.put(31, "Impossible");
        difficultyHelperMap.put(34, "Extraordinary Ridiculous");
        difficultyHelperMap.put(38, "Extraordinary Impossible");
    }

    public static Map<Integer, String> getDifficultyHelperMap() {
        return instance.difficultyHelperMap;
    }

    public static HighLevelComponent[] createRollComponentRows(boolean addReroll, boolean addEnhancement, int total) {
        List<HighLevelComponent> rows = new ArrayList<>();
        if (addEnhancement) {
            List<LowLevelComponent> enhancementRow = IntStream.range(0, 4)
                    .mapToObj(i -> Button.primary(String.valueOf(i + 1), getLabel(total, i + 1)))
                    .collect(Collectors.toList());
            rows.add(ActionRow.of(enhancementRow));
        }
        if (addReroll) {
            rows.add(ActionRow.of(Button.danger("reroll", "Reroll")));
        }
        if (addEnhancement || addReroll) {
            rows.add(ActionRow.of(Button.success("accept", "Accept")));
        }

        return rows.toArray(new HighLevelComponent[0]);
    }

    /**
     * Computes the label for an enhance button, which consists of a +(number) and a difficulty if enhancing by that number would reach it
     *
     * @param total The result of the roll
     * @param count The amount of plot points to use
     * @return The name of the label
     */
    private static String getLabel(int total, int count) {
        return String.format("+%d%s", count, Optional.ofNullable(getDifficultyHelperMap().get(total + count)).map(s -> " (" + s + ")").orElse(""));
    }
}
