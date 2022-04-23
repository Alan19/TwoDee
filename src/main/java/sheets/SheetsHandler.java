package sheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.vavr.API;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import roles.Player;
import roles.PlayerHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SheetsHandler {
    private static final String APPLICATION_NAME = "Skill Lookup";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "resources/credentials.json";
    private static final SheetsHandler instance = new SheetsHandler();
    private Sheets service;

    /**
     * Constructor to initialize the service field
     */
    private SheetsHandler() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("Unable to access spreadsheets!");
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    /**
     * Retrieves all skills on a player
     * <p>
     * Throws an error if the spreadsheet could not be accessed, or when trying to access the spreadsheet of an unregistered user
     *
     * @param user The user to lookup
     * @return The skills of a user as a HashMap of skill names to the value of the skill in a valid dice format (3d12, d6, etc.)
     */
    public static Try<Map<String, String>> getSkills(User user) {
        return Try.of(() -> getSpreadsheetForPartyMember(user).orElseThrow(() -> new NoSuchElementException(MessageFormat.format("User `{0}` is not registered in `players.json`!", user.getName()))))
                .map(API.unchecked(s -> instance.service.spreadsheets().values().get(s, "AllEverything").execute()))
                .map(SheetsHandler::getSkillMap)
                .recover(NoSuchElementException.class, new HashMap<>());
    }

    /**
     * Transforms a value range into a HashMap containing the skills and their dice equivalents
     *
     * @param valueRange The value range containing a user's skills
     * @return A Map that contains skills as the key and the dice as the value
     */
    private static Map<String, String> getSkillMap(ValueRange valueRange) {
        Map<String, String> skills = new HashMap<>();
        valueRange.getValues().stream()
                .filter(objects -> objects.size() == 2 && ((String) objects.get(1)).matches("([1-9]\\d*)?[kcp]?d[1-9]\\d?"))
                .forEach(objects -> skills.put(((String) objects.get(0)).toLowerCase().replaceAll("\\s", ""), ((String) (objects.get(1)))));
        return skills;
    }

    /**
     * Helper function to get the range for a user's plot points
     *
     * @param user The user to look up
     * @return The ValueRange for the user, wrapped in an optional, or Optional.empty() if the user does not have a linked character sheet
     */
    private static Optional<ValueRange> getPlotPointRange(User user) {
        final Optional<String> spreadsheetForUser = getSpreadsheetForPartyMember(user);
        if (spreadsheetForUser.isPresent()) {
            try {
                return Optional.of(instance.service.spreadsheets().values().get(spreadsheetForUser.get(), "PlotPoints").execute());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    /**
     * Attempts to retrieve the number of plot points a user has
     * <p>
     * TODO Switch this to use CompletableFuture
     *
     * @param user The user to look up
     * @return The number of plot points a user has in an optional, or empty if the user does not have a linked character sheet
     */
    public static Optional<Integer> getPlotPoints(User user) {
        return getPlotPointRange(user).map(valueRange -> Integer.parseInt((String) valueRange.getValues().get(0).get(0)));
    }

    /**
     * Sets the plot point count for a user
     *
     * @param user  The user whose plot point count is being modified
     * @param count The new number of plot points for the user
     * @return A completable future that will return the new number of plot points once it is completed
     */
    public static CompletableFuture<Optional<Integer>> setPlotPoints(User user, int count) {
        return CompletableFuture.supplyAsync(() -> {
            final Optional<ValueRange> userPlotPointRangeOptional = getPlotPointRange(user);
            final Optional<String> partyMemberSpreadsheetID = getSpreadsheetForPartyMember(user);
            if (partyMemberSpreadsheetID.isPresent() && userPlotPointRangeOptional.isPresent()) {
                final ValueRange userPlotPointRange = userPlotPointRangeOptional.get();
                userPlotPointRange.setValues(Collections.singletonList(Collections.singletonList(String.valueOf(count))));
                try {
                    final UpdateValuesResponse plotPointsCellUpdateRequest = instance.service.spreadsheets()
                            .values()
                            .update(partyMemberSpreadsheetID.get(), userPlotPointRange.getRange(), userPlotPointRange)
                            .setIncludeValuesInResponse(true)
                            .setValueInputOption("RAW")
                            .execute();
                    final int updatedPlotPointCount = Integer.parseInt((String) plotPointsCellUpdateRequest.getUpdatedData().getValues().get(0).get(0));
                    return Optional.of(updatedPlotPointCount);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Optional.empty();
            }
            return Optional.empty();
        });
    }

    /**
     * A functon used to throw an exception. Used in completable futures to allow exceptions to be handled in exceptonally
     *
     * @throws T The exception to throw
     */
    @SuppressWarnings("all")
    public static <R, T extends Throwable> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }


    /**
     * Gets the current bleed value of a player
     *
     * @param user The user to check the bleed value of of
     * @return An optional containing the bleed value of the user, or Optional.empty() if the cell cannot be retrieved
     */
    public static Optional<Integer> getPlayerBleed(User user) {
        final Optional<String> spreadsheetForUser = getSpreadsheetForPartyMember(user);
        if (spreadsheetForUser.isPresent()) {
            try {
                return Optional.of(instance.service.spreadsheets().values().get(spreadsheetForUser.get(), "PlotPointBleed").execute()).map(valueRange -> Integer.parseInt((String) valueRange.getValues().get(0).get(0)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the spreadsheet ID for a user
     *
     * @param user The user to look up
     * @return The spreadsheet ID for the character sheet of the user, or empty if the user does not have a linked character sheet
     */
    private static Optional<String> getSpreadsheetForPartyMember(User user) {
        return PlayerHandler.getInstance().getPlayers().stream()
                .filter(player -> user.getId() == player.getDiscordId())
                .map(Player::getSheetId)
                .findFirst();
    }

    /**
     * Attempts to retrieve the pool for a Facets attribute roll (initiative, vitality, willpower) for the specified character.
     *
     * @param saveType The type of save to retrieve.
     * @param user     The user attached to the character sheet.
     * @return A dice pool in the format of `d4 d6 d8`, or an exception if there is an issue with retrieving the pool.
     */
    public static Try<String> getSavePool(String saveType, User user) {
        final Optional<String> spreadsheetForUser = getSpreadsheetForPartyMember(user);
        if (spreadsheetForUser.isPresent()) {
            return Try.of(() -> instance.service.spreadsheets()
                            .values()
                            .get(spreadsheetForUser.get(), saveType)
                            .execute())
                    .map(valueRange -> ((String) (valueRange.getValues().get(0).get(0))).replace(" + ", " "));

        }
        return Try.failure(new NoSuchFieldError(MessageFormat.format("Unable to find spreadsheet for user {0}", user.getName())));
    }

    /**
     * Attempts to retrieve the pool for a Facets saved roll for the specified character.
     *
     * @param poolName The name of the pool to retrieve. The pool name should have all spaces removed.
     * @param user     The user attached to the character sheet
     * @return A dice pool in the format of `d4 d6 d8`, or an exception if there is an issue with retrieving the pool.
     */
    public static Try<String> getSavedPool(String poolName, User user) {
        return getSavedPools(user).flatMapTry(pairs -> Try.of(() -> pairs.stream()
                .filter(pair -> pair.getLeft().equalsIgnoreCase(poolName))
                .findFirst()
                .map(Pair::getRight)
                .orElseThrow(() -> new NoSuchElementException(MessageFormat.format("Unable to find {0} in list of saved pools!", poolName)))));
    }

    public static Try<List<Pair<String, String>>> getSavedPools(User user) {
        final Optional<String> spreadsheetID = getSpreadsheetForPartyMember(user);
        if (spreadsheetID.isPresent()) {
            return Try.of(() -> instance.service.spreadsheets().values()
                            .get(spreadsheetID.get(), "DicePools")
                            .execute())
                    .map(valueRange -> valueRange.getValues().stream()
                            .filter(objects -> objects.size() == 2)
                            .map(objects -> Pair.of(((String) (objects.get(0))).replaceAll("\\s", ""), ((String) (objects.get(1))).replace(" + ", " ")))
                            .collect(Collectors.toList()));
        }
        return Try.failure(new NoSuchFieldError(MessageFormat.format("Unable to find spreadsheet for user {0}", user.getName())));
    }

    public static Try<List<SlashCommandOptionChoice>> getSavedPoolChoices(User user) {
        final Optional<String> spreadsheetID = getSpreadsheetForPartyMember(user);
        if (spreadsheetID.isPresent()) {
            return Try.of(() -> instance.service.spreadsheets().values()
                            .get(spreadsheetID.get(), "DicePools")
                            .execute())
                    .map(valueRange -> valueRange.getValues().stream()
                            .filter(objects -> objects.size() == 2)
                            .map(objects -> SlashCommandOptionChoice.create(((String) (objects.get(0))), ((String) (objects.get(0))).replaceAll("\\s", "")))
                            .collect(Collectors.toList()));
        }
        return Try.failure(new NoSuchFieldError(MessageFormat.format("Unable to find spreadsheet for user {0}", user.getName())));
    }

    /**
     * Returns the list of languages for a user
     *
     * @param player The player to search
     * @return A list of languages as strings
     */
    public static Try<Collection<String>> getLanguages(Player player) {
        return Try.of(() -> getRange(player.getSheetId(), "Languages"))
                .map(valueRange -> (String) valueRange.getValues().get(0).get(0))
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet())
                );
    }

    /**
     * Returns a range for the given spreadsheet
     *
     * @param spreadsheetID The ID of the spreadsheet
     * @param range         The range to return
     * @return A ValueRange that contains the information of the given range
     */
    @SuppressWarnings("SameParameterValue")
    private static ValueRange getRange(String spreadsheetID, String range) throws IOException {
        return instance.service.spreadsheets().values()
                .get(spreadsheetID, range)
                .execute();
    }
}
