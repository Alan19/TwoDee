package roles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.javacord.api.entity.user.User;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public List<Player> getPlayers() {
        return players;
    }
}
