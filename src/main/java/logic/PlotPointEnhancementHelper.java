package logic;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
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

    /**
     * Helper method to remove the enhancement (number) emojis from a message
     *
     * @param message The message object whose emojis will be removed
     */
    public static void removeEnhancementEmojis(Message message) {
        PlotPointEnhancementHelper helper = new PlotPointEnhancementHelper();
        //Remove 1 to 10
        for (String emoji : helper.getOneToTwelveEmojiMap().values()) {
            message.removeReactionsByEmoji(emoji).join();
        }

        //Remove 11 and 12
        message.getServer().ifPresent(server -> message.removeReactionsByEmoji(server.getCustomEmojiById("525867366303793182").get(), server.getCustomEmojiById("525867383890509864").get()).join());

        //Remove Cancel Emoji
        message.removeReactionsByEmoji("\uD83C\uDDFD").join();
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

    public boolean isEmojiNumberEmoji(Emoji emoji) {
        if (emoji.isUnicodeEmoji() && emoji.asUnicodeEmoji().isPresent()) {
            return oneToTwelveEmojiMap.values().contains(emoji.asUnicodeEmoji().get());
        } else if (emoji.isKnownCustomEmoji() && emoji.asKnownCustomEmoji().isPresent()) {
            String trimmedEmoji = trimCustomEmoji(emoji.asKnownCustomEmoji().get());
            return oneToTwelveEmojiMap.values().contains(trimmedEmoji);
        }
        return false;
    }

    public boolean isEmojiCancelEmoji(Emoji emoji) {
        return emoji.equalsEmoji(":regional_indicator_x:");
    }

    public boolean isEmojiEnhancementEmoji(Emoji emoji) {
        return isEmojiCancelEmoji(emoji) || isEmojiNumberEmoji(emoji);
    }

    public String trimCustomEmoji(KnownCustomEmoji emoji) {
        String tag = emoji.asKnownCustomEmoji().get().getMentionTag();
        return tag.substring(2, tag.length() - 1);
    }

    public String getCancelEmoji() {
        return cancelEmoji;
    }
}