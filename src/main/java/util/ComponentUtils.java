package util;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.component.LowLevelComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComponentUtils {

    public static HighLevelComponent[] createRollComponentRows(boolean addReroll, boolean addEnhancement) {
        List<HighLevelComponent> rows = new ArrayList<>();
        if (addEnhancement) {
            List<LowLevelComponent> enhancementRow = IntStream.range(0, 4)
                    .mapToObj(i -> Button.primary(String.valueOf(i + 1), "+" + (i + 1)))
                    .collect(Collectors.toList());
            enhancementRow.add(Button.success("confirm", EmojiParser.parseToUnicode(":heavy_check_mark:")));
            rows.add(ActionRow.of(enhancementRow));
        }
        if (addReroll) {
            rows.add(ActionRow.of(Button.danger("reroll", EmojiParser.parseToUnicode(":repeat:"))));
        }

        return rows.toArray(new HighLevelComponent[0]);
    }
}
