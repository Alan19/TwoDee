package roles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class PlayerHandler {
    private static final PlayerHandler instance = new PlayerHandler();
    private List<Player> players;

    private PlayerHandler() {
        try {
            players = new Gson().fromJson(new BufferedReader(new FileReader("resources/parties.json")), TypeToken.getParameterized(ArrayList.class, Player.class).getType());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static PlayerHandler getInstance() {
        return instance;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
