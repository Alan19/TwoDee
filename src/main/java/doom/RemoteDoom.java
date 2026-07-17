package doom;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import configs.Settings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record RemoteDoom(
        long id,
        String name,
        int doom
) implements Doom {
    public static final TypeToken<List<RemoteDoom>> LIST_TYPE_TOKEN = new TypeToken<>() {
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
                    RemoteDoom doomPool = GSON.fromJson(httpResponse.body(), RemoteDoom.class);
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
        //Todo Implement
        return CompletableFuture.completedFuture(new DoomChange(0, amount));
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        //Todo Implement
        return CompletableFuture.completedFuture(false);
    }
}
