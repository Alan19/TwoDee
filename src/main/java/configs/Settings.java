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
import java.util.Optional;
import java.util.stream.Collectors;

public class Settings {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    public static final Settings instance = new Settings();
    private static final Logger LOGGER = LogManager.getLogger(Settings.class);
    private SettingsInstance settingsInstance;
    private Quotes quotes;

    private Settings() {
        String settingsFileLocation = Optional.ofNullable(System.getenv("SETTINGS_FILE"))
                .orElse("resources/settings.json");
        try (
                FileReader fileReader = new FileReader(settingsFileLocation);
                BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            settingsInstance = GSON.fromJson(
                    bufferedReader,
                    new TypeToken<SettingsInstance>() {
                    }.getType()
            );
        } catch (IOException e) {
            LOGGER.error("Unable to find settings file!");
            settingsInstance = new SettingsInstance();
        }
        String quotesFileLocation = Optional.ofNullable(System.getenv("QUOTES_FILE"))
                .orElse("resources/quotes.json");
        try (
                FileReader fileReader = new FileReader(quotesFileLocation);
                BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            quotes = GSON.fromJson(
                    bufferedReader,
                    new TypeToken<Quotes>() {
                    }.getType()
            );
        } catch (IOException e) {
            LOGGER.error("Unable to find quotes file!");
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

    public static RemoteDataSettings getRemoteDataSettings() {
        return getSettingsInstance().getRemoteDataSettings();
    }

    private static SettingsInstance getSettingsInstance() {
        return instance.settingsInstance;
    }

    /**
     * Serializes the values of the settings and writes it to settings.json. Generally used to update doom pools.
     */
    public static void serializePersonalSettings() {
        String settingsFileLocation = Optional.ofNullable(System.getenv("SETTINGS_FILE"))
                .orElse("resources/settings.json");
        try (
                FileWriter fileWriter = new FileWriter(settingsFileLocation);
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            GSON.toJson(Settings.getSettingsInstance(), writer);
        } catch (IOException e) {
            LOGGER.error("Unable to serialize settings file!", e);
        }
    }

    protected static void serializeQuotes() {
        String quotesFileLocation = Optional.ofNullable(System.getenv("QUOTES_FILE"))
                .orElse("resources/quotes.json");
        try (
                FileWriter fileWriter = new FileWriter(quotesFileLocation);
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            GSON.toJson(Settings.getQuotes(), writer);
        } catch (IOException e) {
            LOGGER.error("Unable to serialize quotes file!", e);
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
        private final RemoteDataSettings remoteDataSettings;
        private final List<Player> players;

        public SettingsInstance() {
            doom = new DoomSettings();
            discordSettings = new DiscordSettings();
            players = new ArrayList<>();
            this.remoteDataSettings = new RemoteDataSettings();
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

        public RemoteDataSettings getRemoteDataSettings() {
            return remoteDataSettings;
        }
    }
}
