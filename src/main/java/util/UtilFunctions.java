package util;

import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;

import java.util.Locale;
import java.util.Optional;

public class UtilFunctions {
    public static Optional<Integer> tryParseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the display name of the user in a channel
     *
     * @param user    The user to check the nickname of
     * @param channel The channel to check
     * @return The display name of the user in a channel, or their name if it's not a server channel
     */
    public static String getUsernameInChannel(User user, Channel channel) {
        return channel.asServerTextChannel().map(serverTextChannel -> user.getDisplayName(serverTextChannel.getServer())).orElseGet(user::getName);
    }

    public static String getUsernameFromSlashEvent(InteractionBase event, User user) {
        return event.getChannel().map(channel -> getUsernameInChannel(user, channel)).orElse(user.getName());
    }

    /**
     * Checks if a string is within another string and ignores case
     *
     * @param s         The main string
     * @param substring The substring to be checked within the main string
     * @return If the substring is within the main string, ignoring casing
     */
    public static boolean containsIgnoreCase(String s, String substring) {
        return s.toLowerCase(Locale.ROOT).contains(substring);
    }
}
