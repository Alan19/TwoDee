package logic;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.entity.message.Message;

import java.util.HashMap;

public class PlotPointEnhancementHelper {

    private HashMap<String, Integer> oneToTwelveEmojiMap = new HashMap<>();
    private String cancelEmoji = EmojiParser.parseToUnicode(":regional_indicator_x:");

    public PlotPointEnhancementHelper() {
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":one:"), 1);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":two:"), 2);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":three:"), 3);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":four:"), 4);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":five:"), 5);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":six:"), 6);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":seven:"), 7);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":eight:"), 8);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":nine:"), 9);
        oneToTwelveEmojiMap.put(EmojiParser.parseToUnicode(":keycap_ten:"), 10);
        oneToTwelveEmojiMap.put("keycap_11:525867366303793182", 11);
        oneToTwelveEmojiMap.put("keycap_12:525867383890509864", 12);
    }

    public HashMap<String, Integer> getOneToTwelveEmojiMap() {
        return oneToTwelveEmojiMap;
    }

    public void addPlotPointEnhancementEmojis(Message rollMessage) {
        for (String emoji : oneToTwelveEmojiMap.keySet()) {
            rollMessage.addReaction(emoji);
        }
    }

    public String getCancelEmoji() {
        return cancelEmoji;
    }
}