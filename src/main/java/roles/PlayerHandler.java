package roles;

import configs.Settings;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlayerHandler {
    private static final PlayerHandler instance = new PlayerHandler();
    private final List<Player> players;

    private PlayerHandler() {
        players = Settings.getPlayerSettings();
    }

    public static PlayerHandler getInstance() {
        return instance;
    }

    public static Optional<Player> getPlayerFromUser(User user) {
        return getInstance().getPlayers()
                .stream()
                .filter(player -> player.getDiscordId() == user.getId())
                .findFirst();
    }

    public static Collection<Pair<User, Player>> getPlayersFromMentionable(Mentionable mentionable) {
        if (mentionable instanceof Role) {
            return ((Role) mentionable).getUsers()
                    .stream()
                    .map(user -> getPlayerFromUser(user)
                            .map(player -> Pair.of(user, player))
                    )
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
        }
        else if (mentionable instanceof User) {
            return getPlayerFromUser((User) mentionable)
                    .map(player -> Pair.of((User) mentionable, player))
                    .map(Collections::singleton)
                    .orElse(Collections.emptySet());
        }
        else {
            return Collections.emptyList();
        }
    }

    public List<Player> getPlayers() {
        return players;
    }
}
