package sheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.javacord.api.entity.user.User;
import players.PartyHandler;
import players.PartyMember;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;

public class SheetsHandler {
    private static final String APPLICATION_NAME = "Skill Lookup";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
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
     *
     * @param user The user to lookup
     * @return The skills of a user as a HashMap of skill names to facet count
     */
    public static Optional<Map<String, Integer>> getSkills(User user) {
        Map<String, Integer> skills = new HashMap<>();
        final Optional<String> spreadsheetID = getSpreadsheetForPartyMember(user);
        if (spreadsheetID.isPresent()) {
            try {
                final ValueRange response = instance.service.spreadsheets().values().get(spreadsheetID.get(), "Variables!AK3:AL1001").execute();
                response.getValues().stream()
                        .filter(objects -> objects.size() == 2 && ((String) objects.get(1)).matches("d[1-9]+[0-9]*"))
                        .forEach(objects -> skills.put((String) objects.get(0), Integer.parseInt(((String) (objects.get(1))).substring(1))));
                return Optional.of(skills);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
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
     *
     * @param user The user to look up
     * @return The number of plot points a user has in an optional, or empty if the user does not have a linked character sheet
     */
    public static Optional<Integer> getPlotPoints(User user) {
        return getPlotPointRange(user).map(valueRange -> Integer.parseInt((String) valueRange.getValues().get(0).get(0)));
    }

    /**
     * Sets the number of plot points for a user
     *
     * @param user  The user to modify
     * @param count The new number of plot points the user has
     * @return If the cell for plot points linked to that user exists
     */
    public static boolean setPlotPoints(User user, int count) {
        final Optional<ValueRange> rangeOptional = getPlotPointRange(user);
        if (rangeOptional.isPresent()) {
            final ValueRange plotPointRange = rangeOptional.get();
            plotPointRange.setValues(Collections.singletonList(Collections.singletonList(String.valueOf(count))));
            final Optional<String> spreadsheetID = getSpreadsheetForPartyMember(user);
            if (spreadsheetID.isPresent()) {
                try {
                    UpdateValuesResponse result = instance.service.spreadsheets().values().update(spreadsheetID.get(), plotPointRange.getRange(), plotPointRange)
                            .setValueInputOption("RAW")
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Gets the spreadsheet ID for a user
     *
     * @param user The user to look up
     * @return The spreadsheet ID for the character sheet of the user, or empty if the user does not have a linked character sheet
     */
    private static Optional<String> getSpreadsheetForPartyMember(User user) {
        return PartyHandler.getParties().stream()
                .flatMap(party -> party.getMembers().stream())
                .filter(partyMember -> user.getId() == partyMember.getDiscordId())
                .map(PartyMember::getSheetId)
                .findFirst();
    }
}
