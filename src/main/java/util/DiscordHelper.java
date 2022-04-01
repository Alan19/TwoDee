package util;

import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class DiscordHelper {
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

    public static Collection<String> getUsernamesFor(Mentionable mentionable, Channel channel) {
        if (mentionable instanceof Role) {
            return ((Role) mentionable).getUsers()
                    .stream()
                    .map(user -> getUsernameInChannel(user, channel))
                    .collect(Collectors.toSet());
        }
        else if (mentionable instanceof User) {
            return Collections.singleton(getUsernameInChannel((User) mentionable, channel));
        }
        else {
            return Collections.emptyList();
        }
    }

    public static String getUsernameFromInteraction(InteractionBase event, User user) {
        return event.getChannel().map(channel -> getUsernameInChannel(user, channel)).orElse(user.getName());
    }
}
