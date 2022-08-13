package configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import roles.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Settings {
    public static final Settings instance = new Settings();
    private static final Logger LOGGER = LogManager.getLogger(Settings.class);
    private SettingsInstance settingsInstance;
    private Quotes quotes;

    private Settings() {
        try {
            settingsInstance = new Gson().fromJson(new BufferedReader(new FileReader("resources/settings.json")), new TypeToken<SettingsInstance>() {
            }.getType());
            quotes = new Gson().fromJson(new BufferedReader(new FileReader("resources/quotes.json")), new TypeToken<Quotes>() {
            }.getType());
        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to find settings file!");
            settingsInstance = new SettingsInstance();
            quotes = new Quotes();
        }
    }

    public static DoomSettings getDoom() {
        return getSettingsInstance().getDoom();
    }

    public static List<Player> getPlayerSettings() {
        return getSettingsInstance().getPlayers();
    }

    public static DiscordSettings getDiscordSettings() {
        return getSettingsInstance().getDiscordSettings();
    }

    public static Quotes getQuotes() {
        return instance.quotes;
    }

    private static SettingsInstance getSettingsInstance() {
        return instance.settingsInstance;
    }

    /**
     * Serializes the values of the settings and writes it to settings.json. Generally used to update doom pools.
     */
    public static void serializePersonalSettings() {
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter("resources/settings.json"));
            new GsonBuilder().setPrettyPrinting().create().toJson(Settings.getSettingsInstance(), writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void serializeQuotes() {
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter("resources/quotes.json"));
            new GsonBuilder().setPrettyPrinting().create().toJson(Settings.getQuotes(), writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addQuote(String quote) {
        final List<String> newQuotes = Arrays.stream(getQuotes().getRollQuotes()).collect(Collectors.toList());
        newQuotes.add(quote);
        getQuotes().setRollQuotes(newQuotes.toArray(new String[0]));
        Settings.serializeQuotes();
    }

    public static void removeQuote(String quote) {
        final List<String> newQuotes = Arrays.stream(getQuotes().getRollQuotes()).collect(Collectors.toList());
        newQuotes.removeIf(s -> s.equals(quote));
        getQuotes().setRollQuotes(newQuotes.toArray(new String[0]));
        Settings.serializeQuotes();

    }

    private static class SettingsInstance {
        private final DoomSettings doom;
        private final DiscordSettings discordSettings;
        private final List<Player> players;

        public SettingsInstance() {
            doom = new DoomSettings();
            discordSettings = new DiscordSettings();
            players = new ArrayList<>();
        }

        public DoomSettings getDoom() {
            return doom;
        }

        public DiscordSettings getDiscordSettings() {
            return discordSettings;
        }

        public List<Player> getPlayers() {
            return players;
        }
    }
}
