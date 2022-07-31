package util;

import org.javacord.api.entity.Icon;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ApplicationCommandEvent;
import org.javacord.api.interaction.Interaction;
import org.javacord.api.interaction.InteractionBase;
import pw.mihou.velen.interfaces.hybrid.event.VelenGeneralEvent;
import pw.mihou.velen.interfaces.hybrid.objects.VelenOption;
import pw.mihou.velen.interfaces.hybrid.objects.subcommands.VelenSubcommand;

import java.util.*;

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

    public static String getUsernameFromInteraction(InteractionBase event, User user) {
        return event.getChannel().map(channel -> getUsernameInChannel(user, channel)).orElse(user.getName());
    }

    public static Optional<VelenSubcommand> getSubcommandInHybridCommand(boolean isMessage, VelenOption[] args) {
        return Arrays.stream(args)
                .skip(isMessage ? 1 : 0)
                .findFirst()
                .map(VelenOption::asSubcommand);
    }

    /**
     * Retrieves the avatar for a user in the current channel
     *
     * @param event The event containing the server option
     * @param user  The user
     * @return The server / channel specific avatar for the provided user, defaults to the normal avatar if event is not in a server
     */
    public static Icon getLocalAvatar(VelenGeneralEvent event, User user) {
        return event.getServer().map(user::getEffectiveAvatar).orElseGet(user::getAvatar);
    }

    public static Icon getLocalAvatar(ApplicationCommandEvent event, User user) {
        return event.getInteraction().getServer().map(user::getEffectiveAvatar).orElseGet(user::getAvatar);
    }

    public static Icon getLocalAvatar(Interaction interaction, User user) {
        return interaction.getServer().map(user::getEffectiveAvatar).orElseGet(user::getAvatar);
    }

    public static EmbedBuilder addUserToFooter(VelenGeneralEvent event, User user, EmbedBuilder output) {
        return output.setFooter("Requested by " + UtilFunctions.getUsernameInChannel(user, event.getChannel()), getLocalAvatar(event, user));
    }
}
