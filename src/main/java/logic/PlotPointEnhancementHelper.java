package logic;

import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlotPointEnhancementHelper {

    private static final TreeMap<Integer, String> oneToTwelveEmojiMap = new TreeMap<>();
    public static final String CANCEL_EMOJI = EmojiParser.parseToUnicode(":regional_indicator_x:");

    static {
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

    private PlotPointEnhancementHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Helper method to remove the enhancement (number) emojis from a message
     *
     * @param message The message object whose emojis will be removed
     * @return A completable future when all emojis get removed
     */
    public static CompletableFuture<Void> removeEnhancementEmojis(Message message) {
        //Store all of the futures for allOf
        //Remove 1 to 10
        ArrayList<CompletableFuture<Void>> completableFutures = PlotPointEnhancementHelper.getOneToTwelveEmojiMap()
                .values()
                .stream()
                .map(message::removeReactionsByEmoji)
                .collect(Collectors.toCollection(ArrayList::new));

        //Remove 11 and 12 and add to ArrayList by transforming them into CompletableFutures
        message.getServer().map(server -> message.removeReactionsByEmoji(server.getCustomEmojiById("525867366303793182").get(), server.getCustomEmojiById("525867383890509864").get())).ifPresent(completableFutures::add);

        //Remove Cancel Emoji
        CompletableFuture<Void> cancelEmojiFuture = message.removeReactionsByEmoji("\uD83C\uDDFD");
        completableFutures.add(cancelEmojiFuture);

        CompletableFuture[] castedFutures = new CompletableFuture[completableFutures.size()];
        castedFutures = completableFutures.toArray(castedFutures);
        return CompletableFuture.allOf(castedFutures);
    }

    public static SortedMap<Integer, String> getOneToTwelveEmojiMap() {
        return oneToTwelveEmojiMap;
    }

    public static void addPlotPointEnhancementEmojis(Message rollMessage) {
        for (String emoji : oneToTwelveEmojiMap.values()) {
            rollMessage.addReaction(emoji);
        }
        rollMessage.addReaction("\uD83C\uDDFD");
    }

    public static boolean isEmojiNumberEmoji(Emoji emoji) {
        if (emoji.isUnicodeEmoji() && emoji.asUnicodeEmoji().isPresent()) {
            return oneToTwelveEmojiMap.values().contains(emoji.asUnicodeEmoji().get());
        }
        else if (emoji.isKnownCustomEmoji() && emoji.asKnownCustomEmoji().isPresent()) {
            String trimmedEmoji = trimCustomEmoji(emoji.asKnownCustomEmoji().get());
            return oneToTwelveEmojiMap.values().contains(trimmedEmoji);
        }
        return false;
    }

    public static boolean isEmojiCancelEmoji(Emoji emoji) {
        return emoji.equalsEmoji(":regional_indicator_x:");
    }

    public static boolean isEmojiEnhancementEmoji(Emoji emoji) {
        return isEmojiCancelEmoji(emoji) || isEmojiNumberEmoji(emoji);
    }

    public static String trimCustomEmoji(KnownCustomEmoji emoji) {
        String tag = emoji.asKnownCustomEmoji().get().getMentionTag();
        return tag.substring(2, tag.length() - 1);
    }

}