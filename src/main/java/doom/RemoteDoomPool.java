package doom;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import configs.Settings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record RemoteDoomPool(
        long id,
        String name,
        int doom
) implements DoomPool {
    public static final TypeToken<List<RemoteDoomPool>> LIST_TYPE_TOKEN = new TypeToken<>() {
    };

    private static final Gson GSON = new Gson();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public CompletableFuture<Integer> getDoom() {
        return httpClient.sendAsync(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/doompool/" + this.id()))
                        .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                        .build(),
                BodyHandlers.ofString()
        ).thenComposeAsync(httpResponse -> {
            if (httpResponse.statusCode() == 200) {
                try {
                    RemoteDoomPool doomPool = GSON.fromJson(httpResponse.body(), RemoteDoomPool.class);
                    return CompletableFuture.completedFuture(doomPool.doom());
                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            } else {
                return CompletableFuture.failedFuture(
                        new Exception("Received %s for Http Response for getDoom".formatted(httpResponse.statusCode()))
                );
            }
        });
    }

    @Override
    public CompletableFuture<DoomChange> changeDoom(int amount) {
        return httpClient.sendAsync(
                HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(Map.of("amount", amount))))
                        .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/doompool/" + this.id() + "/doom"))
                        .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                        .build(),
                BodyHandlers.ofString()
        ).thenComposeAsync(httpResponse -> {
            if (httpResponse.statusCode() == 200) {
                try {
                    DoomChange doomChange = GSON.fromJson(httpResponse.body(), DoomChange.class);
                    return CompletableFuture.completedFuture(doomChange);
                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            } else {
                return CompletableFuture.failedFuture(
                        new Exception("Received %s for Http Response for getDoom".formatted(httpResponse.statusCode()))
                );
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        //Todo Implement
        return CompletableFuture.completedFuture(false);
    }
}
