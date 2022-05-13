package configs;

import java.util.ArrayList;
import java.util.List;

public class DiscordSettings {
    private final List<Long> announcementChannels;
    private final String token;
    private final List<Long> storytellerRoles;

    public DiscordSettings() {
        announcementChannels = new ArrayList<>();
        token = "";
        storytellerRoles = new ArrayList<>();
    }

    public List<Long> getAnnouncementChannels() {
        return announcementChannels;
    }

    public String getToken() {
        return token;
    }

    public List<Long> getStorytellerRoles() {
        return storytellerRoles;
    }
}
