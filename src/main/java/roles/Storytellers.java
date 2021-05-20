package roles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Storytellers {
    private static final Storytellers instance = new Storytellers();
    private List<Long> storytellerRoles;

    private Storytellers() {
        try {
            storytellerRoles = new Gson().fromJson(new BufferedReader(new FileReader("resources/storytellers.json")), TypeToken.getParameterized(ArrayList.class, Long.class).getType());
        } catch (FileNotFoundException e) {
            storytellerRoles = new ArrayList<>();
        }
    }

    /**
     * Gets all of the storyteller role
     *
     * @param api The Discord API
     * @return A list of storyteller roles
     */
    public static List<Role> getStorytellerRoles(DiscordApi api) {
        return instance.storytellerRoles.stream().map(api::getRoleById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
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

    /**
     * Checks if a message author is a storyteller
     * <p>
     * Handy for not having to chain isPresent() and .get()
     *
     * @param author The author to check
     * @return Whether the author is the storyteller
     */
    public static boolean isMessageAuthorStoryteller(MessageAuthor author) {
        return author.asUser().isPresent() && isUserStoryteller(author.asUser().get());
    }
}
