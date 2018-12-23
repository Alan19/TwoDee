package logic;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.entity.message.Message;

import java.util.SortedMap;
import java.util.TreeMap;

public class PlotPointEnhancementHelper {

    private TreeMap<Integer, String> oneToTwelveEmojiMap = new TreeMap<>();
    private String cancelEmoji = EmojiParser.parseToUnicode(":regional_indicator_x:");

    public PlotPointEnhancementHelper() {
        oneToTwelveEmojiMap.put(1, EmojiParser.parseToUnicode(":one:"));
        oneToTwelveEmojiMap.put(2, EmojiParser.parseToUnicode(":two:"));
        oneToTwelveEmojiMap.put(3, EmojiParser.parseToUnicode(":three:"));
        oneToTwelveEmojiMap.put(4, EmojiParser.parseToUnicode(":four:"));
        oneToTwelveEmojiMap.put(5, EmojiParser.parseToUnicode(":five:"));
        oneToTwelveEmojiMap.put(6, EmojiParser.parseToUnicode(":six:"));
        oneToTwelveEmojiMap.put(7, EmojiParser.parseToUnicode(":seven:"));
        oneToTwelveEmojiMap.put(8, EmojiParser.parseToUnicode(":eight:"));
        oneToTwelveEmojiMap.put(9, EmojiParser.parseToUnicode(":nine:"));
        oneToTwelveEmojiMap.put(10, EmojiParser.parseToUnicode(":keycap_ten:"));
        oneToTwelveEmojiMap.put(11, "keycap_11:525867366303793182");
        oneToTwelveEmojiMap.put(12, "keycap_12:525867383890509864");
    }

    public SortedMap<Integer, String> getOneToTwelveEmojiMap() {
        return oneToTwelveEmojiMap;
    }

    public void addPlotPointEnhancementEmojis(Message rollMessage) {
        for (String emoji : oneToTwelveEmojiMap.values()) {
            rollMessage.addReaction(emoji);
        }
        rollMessage.addReaction("\uD83C\uDDFD");
    }

    public String getCancelEmoji() {
        return cancelEmoji;
    }
}