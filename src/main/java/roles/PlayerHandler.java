package roles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerHandler {
    private static final PlayerHandler instance = new PlayerHandler();
    private List<Player> players;

    private PlayerHandler() {
        try {
            players = new Gson().fromJson(new BufferedReader(new FileReader("resources/players.json")), TypeToken.getParameterized(ArrayList.class, Player.class).getType());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            players = new ArrayList<>();
        }
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
                    .flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty))
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
