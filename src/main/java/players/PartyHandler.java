package players;

import com.google.gson.Gson;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    public static List<CompletableFuture<User>> getPartyMembers(String partyName, DiscordApi api) {
        List<CompletableFuture<User>> completableFutures;
        completableFutures = instance.parties.getParties().stream()
                .filter(party -> party.getName().equals(partyName))
                .findFirst()
                .map(party -> party.getMembers().stream().mapToLong(PartyMember::getDiscordId).mapToObj(api::getUserById).collect(Collectors.toList()))
                .orElse(new ArrayList<>());
        return completableFutures;
    }

    public static List<Party> getParties() {
        return instance.parties.getParties();
    }

    public static Optional<Party> getPartyByName(String name) {
        return getParties().stream().filter(party -> party.getName().equals(name)).findFirst();
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
