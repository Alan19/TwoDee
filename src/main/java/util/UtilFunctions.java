package util;

import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

    public static <T> CompletableFuture<List<T>> appendFutureOptionalToCompletableFutureList(CompletableFuture<List<T>> listCompletableFuture, CompletableFuture<Optional<T>> element) {
        return listCompletableFuture.thenCombine(element, (ts, t) -> {
            t.ifPresent(ts::add);
            return ts;
        });
    }

    public static <T> CompletableFuture<List<T>> appendElementToCompletableFutureList(CompletableFuture<List<T>> listCompletableFuture, T element) {
        return listCompletableFuture.thenApply(ts -> {
            ts.add(element);
            return ts;
        });
    }

    public static <T> CompletableFuture<List<T>> appendFutureToCompletableFutureList(CompletableFuture<List<T>> listCompletableFuture, CompletableFuture<T> element) {
        return listCompletableFuture.thenCombine(element, (ts, t) -> {
            ts.add(t);
            return ts;
        });
    }

    public static String getUsernameFromSlashEvent(SlashCommandInteraction event, User user) {
        return event.getChannel().map(channel -> getUsernameInChannel(user, channel)).orElse(user.getName());
    }
}
