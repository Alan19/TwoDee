package roles;

import com.google.gson.Gson;
import configs.Settings;
import doom.DoomHandler;
import doom.RemoteDoomPool;
import org.apache.commons.lang3.tuple.Pair;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sheets.SheetsHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class PlayerHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerHandler.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final PlayerHandler instance = new PlayerHandler();
    private final List<Player> players;

    private PlayerHandler() {
        players = Settings.getPlayerSettings();
    }

    public static PlayerHandler getInstance() {
        return instance;
    }

    public static Optional<Player> getPlayerFromUser(User user) {
        return getInstance().getPlayers()
                .stream()
                .filter(player -> player.getDiscordId() == user.getId())
                .findFirst();
    }

    public static Collection<Pair<User, Player>> getPlayersFromMentionable(Mentionable mentionable) {
        if (mentionable instanceof Role) {
            return ((Role) mentionable).getUsers()
                    .stream()
                    .map(user -> getPlayerFromUser(user)
                            .map(player -> Pair.of(user, player))
                    )
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
        } else if (mentionable instanceof User) {
            return getPlayerFromUser((User) mentionable)
                    .map(player -> Pair.of((User) mentionable, player))
                    .map(Collections::singleton)
                    .orElse(Collections.emptySet());
        } else {
            return Collections.emptyList();
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public CompletableFuture<DiscordApi> uploadPlayersToRemote(DiscordApi discordApi) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/character"))
                .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<List<RemoteCharacter>>thenApply(response -> GSON.fromJson(
                        response.body(),
                        RemoteCharacter.LIST_TYPE_TOKEN.getType()
                ))
                .thenApply(this::getPlayersWithoutRemoteCharacters)
                .thenCompose(players -> CompletableFuture.allOf(players.stream()
                        .map(player -> createCharacterFor(discordApi, player))
                        .toArray(CompletableFuture[]::new)
                ))
                .thenApply(ignore -> discordApi);
    }

    private List<Player> getPlayersWithoutRemoteCharacters(List<RemoteCharacter> remoteCharacters) {
        List<String> existingSheets = remoteCharacters.stream()
                .map(RemoteCharacter::sheet)
                .toList();

        return players.stream()
                .filter(player -> !existingSheets.contains(player.getSheetId()))
                .toList();
    }

    private CompletableFuture<Void> createCharacterFor(DiscordApi discordApi, Player player) {
        return this.findUserForPlayer(Long.toString(player.getDiscordId()))
                .thenCompose(existingUser -> existingUser.map(CompletableFuture::completedFuture)
                        .orElseGet(() -> this.createUserForPlayer(discordApi, player))
                )
                .thenCompose(remoteUser -> createCharacterFor(remoteUser, player))
                .thenAccept(remoteCharacter -> LOGGER.info("Created new Remote Character {}", remoteCharacter.name()));
    }

    private CompletableFuture<RemoteCharacter> createCharacterFor(RemoteUser remoteUser, Player player) {
        LOGGER.info("Creating new character for user {}", player.getDiscordId());
        return SheetsHandler.getName(player.getSheetId())
                .thenCompose(name -> {
                    if (!name.isEmpty()) {
                        Map<String, Object> map = new HashMap<>();

                        map.put("name", name);
                        map.put("sheet", player.getSheetId());
                        map.put("ownerId", remoteUser.id());

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/character"))
                                .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(map)))
                                .build();

                        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                .thenApply(response -> GSON.fromJson(response.body(), RemoteCharacter.class));
                    } else {
                        throw new CompletionException(new IllegalStateException("Failed to get name for " + player.getSheetId()));
                    }
                });
    }

    private CompletableFuture<Optional<RemoteUser>> findUserForPlayer(String discordId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/user?external_id=" + discordId))
                .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .<List<RemoteUser>>thenApply(response -> GSON.fromJson(response.body(), RemoteUser.LIST_TYPE_TOKEN.getType()))
                .thenApply(existingUsers -> existingUsers.stream()
                        .filter(user -> Optional.ofNullable(user.externalId())
                                .filter(discordId::equals)
                                .isPresent()
                        )
                        .findFirst()
                );
    }

    private CompletableFuture<RemoteUser> createUserForPlayer(DiscordApi discordApi, Player player) {
        Optional<Long> doomPoolId = DoomHandler.getDoomPool(player.getDoomPool())
                .flatMap(doomPool -> {
                    if (doomPool instanceof RemoteDoomPool remoteDoomPool) {
                        return Optional.of(remoteDoomPool.id());
                    } else {
                        return Optional.empty();
                    }
                });
        LOGGER.info("Creating new user for user {}", player.getDiscordId());
        return discordApi.getUserById(player.getDiscordId())
                .thenCompose(user -> {
                    Map<String, Object> requestValues = new HashMap<>();
                    requestValues.put("name", user.getName());
                    requestValues.put("externalId", Long.toString(player.getDiscordId()));
                    requestValues.put("notes", "Created by TwoDee");
                    doomPoolId.ifPresent(id -> requestValues.put("doomPermission", id.toString()));

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/user"))
                            .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestValues)))
                            .build();

                    return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                })
                .thenApply(response -> GSON.fromJson(response.body(), RemoteUser.class));
    }
}
