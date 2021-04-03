package players;

import com.google.gson.Gson;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PartyHandler {
    private static final PartyHandler instance = new PartyHandler();
    private PartyConfig parties;

    private PartyHandler() {
        try {
            parties = new Gson().fromJson(new BufferedReader(new FileReader("resources/parties.json")), PartyConfig.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a list of users in a party
     *
     * @param partyName The name of the party
     * @param api       The Discord API
     * @return A list of User objects that represents the characters in a party
     */
    public static List<User> getPartyMembers(String partyName, DiscordApi api) {
        List<User> foundUsers = new ArrayList<>();
        for (Party party : instance.parties.getParties()) {
            if (party.getName().equals(partyName)) {
                for (PartyMember partyMember : party.getMembers()) {
                    long discordId = partyMember.getDiscordId();
                    CompletableFuture<User> userCompletableFuture = api.getUserById(discordId);
                    userCompletableFuture.thenAccept(foundUsers::add);
                }
                break;
            }
        }
        return foundUsers;
    }

    public static List<Party> getParties() {
        return instance.parties.getParties();
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
