package util;

import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.objects.subcommands.VelenSubcommand;

import java.util.*;
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

    /**
     * Gets all users mentioned by a mentionable, a list of users if it's a role and a single user if it's a user ping
     *
     * @param mentionable A mentionable that could either be a user or a role
     * @return A list of users
     */
    public static Collection<User> getUsersForMentionable(Mentionable mentionable) {
        if (mentionable instanceof Role) {
            return new ArrayList<>(((Role) mentionable).getUsers());
        }
        else {
            return Collections.singleton((User) mentionable);
        }
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

    public static Optional<VelenSubcommand> getSubcommandInHybridCommand(boolean isMessage, VelenOption[] args) {
        return Arrays.stream(args)
                .skip(isMessage ? 1 : 0)
                .findFirst()
                .map(VelenOption::asSubcommand);
    }
}
