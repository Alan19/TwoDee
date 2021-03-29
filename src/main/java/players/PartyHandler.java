package players;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

public class PartyHandler {
    private static final PartyHandler instance = new PartyHandler();
    private PartyConfig party;

    private PartyHandler() {
        try {
            party = new Gson().fromJson(new BufferedReader(new FileReader("resources/parties.json")), PartyConfig.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<Party> getParties() {
        return instance.party.getParties();
    }

    static class PartyConfig {
        private List<Party> parties;

        public List<Party> getParties() {
            return parties;
        }

        public void setParties(List<Party> parties) {
            this.parties = parties;
        }

    }
}
