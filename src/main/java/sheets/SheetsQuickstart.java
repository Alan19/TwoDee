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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class SheetsQuickstart {
    private static final String RANGE = "B12:C12";
    private static final String APPLICATION_NAME = "Summary Stat Fetcher";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "resources/credentials.json";
    private ValueRange result;

    //Builds a new sheet object that contain information about the user's character
    public SheetsQuickstart (String id) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = "18McJSYbBDRr40ZHK7oG4gXqzORoz3B5nrJ0o9zF0F-8";
        final String range = generateRangeCommand(id);
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        result = service.spreadsheets().values().get(spreadsheetId, range).execute();
        int numRows = result.getValues() != null ? result.getValues().size() : 0;
        System.out.printf("%d rows retrieved.", numRows);
    }

    /**
     * Creates an authorized Credential object.
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
     * Gets the amount of points in a skill from a skill database:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     */
    public static void main(String... args) throws IOException, GeneralSecurityException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("resources/bot.properties"));
        String token = prop.getProperty("token");
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = "18McJSYbBDRr40ZHK7oG4gXqzORoz3B5nrJ0o9zF0F-8";
        final String range = "Data!A1:B270";
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        ValueRange result = service.spreadsheets().values().get(spreadsheetId, range).execute();
        int numRows = result.getValues() != null ? result.getValues().size() : 0;
        System.out.printf("%d rows retrieved.", numRows);
    }

    public static ValueRange getPlotPointCell(String docID) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = docID;
        final String range = RANGE;
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return response;
    }

    public ValueRange getResult() {
        return result;
    }

    private String generateRangeCommand(String id) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("resources/players.properties"));
        String[] range = prop.getProperty(id).split(",");
        return "Data!" + range[0] + "1:" + range[1] + "270";
    }

    //Writes a value to the plot point field of a spreadsheet
    public static void writePlotPoints(int plotPoints, String docID) {
        List<List<Object>> values = Arrays.asList(
                Arrays.asList(plotPoints)
        );
        try {
            NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            ValueRange body = new ValueRange().setValues(values);
            UpdateValuesResponse result = service.spreadsheets().values().update(docID, RANGE, body)
                    .setValueInputOption("RAW")
                    .execute();
            System.out.printf("%d cells updated.", result.getUpdatedCells());

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }



}