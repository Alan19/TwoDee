package roles;

import configs.Settings;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.util.List;
import java.util.Optional;

public class Storytellers {
    private static final Storytellers instance = new Storytellers();
    private final List<Long> storytellerRoles;

    private Storytellers() {
        storytellerRoles = Settings.getDiscordSettings().getStorytellerRoles();
    }

    /**
     * Gets all of the storyteller role
     *
     * @param api The Discord API
     * @return A list of storyteller roles
     */
    public static List<Role> getStorytellerRoles(DiscordApi api) {
        return instance.storytellerRoles.stream().map(api::getRoleById).filter(Optional::isPresent).map(Optional::get).toList();
    }

    /**
     * Checks if the user is a storyteller
     *
     * @param user The user to check
     * @return Whether the user is the storyteller
     */
    public static boolean isUserStoryteller(User user) {
        return getStorytellerRoles(user.getApi()).stream().anyMatch(role -> role.getUsers().contains(user));
    }

}
