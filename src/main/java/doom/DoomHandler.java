package doom;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import configs.DoomSettings;
import configs.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import roles.Player;
import roles.PlayerHandler;
import roles.Storytellers;
import util.CachedValue;
import util.DamerauLevenshtein;

import javax.annotation.Nullable;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class DoomHandler {

    private static final Logger LOGGER = LogManager.getLogger(DoomHandler.class);

    public static final String DOOM = "Doom!";
    private static final Gson GSON = new Gson();
    private static final DoomHandler instance = new DoomHandler();
    public final DoomSettings doomSettings;

    private final CachedValue<List<RemoteDoom>> remoteDooms;

    private DoomHandler() {
        doomSettings = Settings.getDoom();
        remoteDooms = new CachedValue<>(Duration.ofHours(4).toMillis(), this::getRemoteDoom);
    }

    public static void setupRemoteDoom() {
        if (!getDoomPools().isEmpty()) {
            instance.doomSettings
                    .getDoomPools()
                    .entrySet()
                    .removeIf(doomPool -> createDoomPool(doomPool.getKey(), doomPool.getValue()));
            Settings.serializePersonalSettings();
        }

        instance.remoteDooms.initialize();
    }

    private static boolean createDoomPool(String name, int count) {
        HttpClient httpClient = HttpClient.newHttpClient();

        String body = GSON.toJson(new RemoteDoomRequest(name, count));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/doompool"))
                .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Wrote Remote Doom: {}", response.body());
            if (response.statusCode() == 200) {
                return true;
            } else {
                LOGGER.error("Received {}, {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create doom pool", e);
            return false;
        }

    }

    private List<RemoteDoom> getRemoteDoom() {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Settings.getRemoteDataSettings().getUrl() + "/doompool"))
                    .header("Authorization", "Bearer " + Settings.getRemoteDataSettings().getToken())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return GSON.fromJson(response.body(), new TypeToken<List<RemoteDoom>>() {
            }.getType());
        } catch (Exception e) {
            LOGGER.error("Failed to read Remote Doom", e);
            return Collections.emptyList();
        }
    }

    /**
     * Adds the specified amount of doom to a specified doom pool and then serialize it. If the doom pool does not
     * exist, create a new doom pool and set the number of doom points to be added as the doom pool's value.
     *
     * @param pool    The name of the doom pool to add the doom to
     * @param doomVal The number of doom points to be added
     * @return An embed that shows the change in doom points for the specified doom pool
     */
    public static EmbedBuilder addDoom(String pool, int doomVal) {
        return getDoomPool(pool)
                .map(doomPool -> {
                    try {
                        DoomChange doomChange = doomPool.changeDoom(doomVal).get();
                        return generateDoomEmbed(pool, doomChange.old(), doomChange.current());
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Failed to update doom", e);
                        return new EmbedBuilder()
                                .setTitle("Error")
                                .setDescription("Failed to update doom: " + e.getMessage());
                    }
                })
                .orElseGet(() -> new EmbedBuilder()
                        .setTitle("Error")
                        .setDescription("No Doom Pool with Name ''**" + pool + "**'' exists.")
                );


    }

    /**
     * Overloaded version of addDoom that adds doom to the currently active pool
     *
     * @param doomVal The number of doom points to add to the pool
     * @return The new amount of doom in the default doom pool
     */
    public static int addDoom(int doomVal) {
        addDoom(getActivePool(), doomVal);
        return getDoom();
    }

    /**
     * Adds doom to the player's set doom pool, or the default doom pool if there isn't any.
     *
     * @param user  The user object
     * @param count The amount of doom to add
     * @return The new amount of plot points in the doom pool
     */
    public static int addDoomOnOpportunity(User user, int count) {
        if (Storytellers.isUserStoryteller(user)) {
            return getDoom();
        } else {
            final String doomPool = PlayerHandler.getPlayerFromUser(user).map(Player::getDoomPool).orElse(getActivePool());
            addDoom(doomPool, count);
            return getDoom(doomPool);
        }
    }

    /**
     * Gets the number of doom points in a doom pool
     *
     * @param pool The pool to check
     * @return The number of doom points in that pool
     */
    public static int getDoom(String pool) {
        return getDoomPool(pool)
                .flatMap(doomPool -> {
                    try {
                        return Optional.of(doomPool.getDoom().get());
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Failed to call getDoom(String pool)", e);
                        return Optional.empty();
                    }
                })
                .orElse(0);
    }

    public static Optional<Doom> getDoomPool(String pool) {
        return getDoomPools()
                .stream()
                .filter(doomPool -> doomPool.getName().equalsIgnoreCase(pool))
                .findFirst();
    }

    public static List<RemoteDoom> getRemoteDoomPools() {
        return instance.remoteDooms.get();
    }

    public static List<ConfigDoom> getOldDoomPools() {
        return instance.doomSettings.getDoomPools()
                .entrySet()
                .stream()
                .map(entry -> new ConfigDoom(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static List<Doom> getDoomPools() {
        ArrayList<Doom> dooms = new ArrayList<>();
        dooms.addAll(getRemoteDoomPools());
        dooms.addAll(getOldDoomPools());
        return dooms;
    }

    /**
     * Gets the number of doom points in the active doom pool
     *
     * @return The number of doom points in the active doom pool
     */
    public static int getDoom() {
        return getDoom(getActivePool());
    }

    /**
     * Sets the doom for a specific doom pool and then serialize it. Creates a new doom pool if it does already exist.
     *
     * @param newDoom The new value of the doom pool
     * @return The embed with a new doom value
     */
    public static EmbedBuilder setDoom(String poolName, int newDoom) {
        try {
            return setDoomAsync(poolName, newDoom).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to set doom", e);
            return new EmbedBuilder()
                    .setTitle("Error")
                    .setDescription("Failed to set doom for ''**" + poolName + "**'':" + e.getMessage());
        }
    }

    public static CompletableFuture<EmbedBuilder> setDoomAsync(String poolName, int newDoom) {
        return getDoomPool(poolName)
                .map(pool -> pool.getDoom()
                        .thenCompose(doom -> pool.changeDoom(newDoom - doom))
                        .thenApply(doomChange -> generateDoomEmbed(pool.getName(), doomChange.old(), doomChange.current()))
                )
                .orElseGet(() -> CompletableFuture.completedFuture(new EmbedBuilder()
                        .setTitle("Error")
                        .setDescription("No Doom Pool with Name ''**" + poolName + "**'' exists.")
                ));
    }

    /**
     * Generates an embed that shows the amount of doom points in the specified doom pool.
     *
     * @param pool The name of the doom pool to check
     * @return The embed containing the value of the doom pool
     */
    public static EmbedBuilder generateDoomEmbed(String pool) {
        final int doom = getDoom(pool);
        return new EmbedBuilder()
                .setTitle(pool)
                .setDescription(String.valueOf(doom))
                .setColor(new Color((int) (doom % 101 * (2.55))));
    }

    /**
     * Generates an embed to show a change in doom points.
     *
     * @param pool    The name of the doom pool to display
     * @param oldDoom The original amount of doom in the pool before it was modified
     * @param newDoom The new amount of doom in the pool
     * @return An embed that shows the name of the pool, and change in doom. The color of the embed gets darker as the
     * number of points in the pool approaches the next multiple of 100.
     */
    public static EmbedBuilder generateDoomEmbed(String pool, int oldDoom, int newDoom) {
        return new EmbedBuilder()
                .setTitle(pool)
                .setDescription(oldDoom + " → " + newDoom)
                .setColor(new Color((int) (newDoom % 101 * (2.55))));
    }

    /**
     * Deletes a doom pool and serialize the change.
     *
     * @param pool The name of the doom pool to delete
     * @return An embed containing information about the deleted doom pool and the number of doom points it had
     */
    public static EmbedBuilder deletePool(String pool) {
        String description = getDoomPool(pool)
                .map(doom -> MessageFormat.format("I''ve removed the ''**{0}**'' doom pool, which contained {1} doom points.", pool, doom))
                .orElseGet(() -> MessageFormat.format("I was unable to find the ''**{0}**'' doom pool", pool));

        return new EmbedBuilder()
                .setTitle(DOOM)
                .setDescription(description);
    }

    /**
     * Generates an embed that provides a summary of the doom pools.
     *
     * @return Am embed with information on the names and values of all available doom pools and the name and value of
     * the active doom pool
     */
    public static EmbedBuilder generateDoomEmbed() {
        final EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(DOOM)
                .setDescription(MessageFormat.format(
                        "Here are the values of all doom pools.\nThe current active doom pool is ''**{0}**'' with {1} doom points",
                        getActivePool(),
                        getDoom(getActivePool())
                ))
                .setColor(new Color((int) (getDoom() % 101 * (2.55))));
        getDoomPools().forEach(doomPool -> embedBuilder.addField(
                doomPool.getName(),
                String.valueOf(doomPool.getDoom())
        ));
        return embedBuilder;
    }

    /**
     * Getter for the value of the active pool
     *
     * @return The name of the active pool
     */
    public static String getActivePool() {
        return instance.doomSettings.getActivePool();
    }

    /**
     * Sets the active pool, serialize it, and return an embed with information about the pool.
     *
     * @param pool The doom pool to be set as the active pool
     * @return An embed with that includes the name and value of the new active doom pool
     */
    public static EmbedBuilder setActivePool(String pool) {
        instance.doomSettings.setActivePool(pool);
        Settings.serializePersonalSettings();
        return new EmbedBuilder().setTitle(DOOM).setDescription(MessageFormat.format("I''ve set the active doom pool to ''**{0}**'', which contains {1} doom points.", pool, getDoom(pool)));
    }

    public static Optional<String> getUserDoomPool(User user) {
        return PlayerHandler.getPlayerFromUser(user).map(Player::getDoomPool);
    }

    public static String getDoomPoolOrDefault(User user) {
        return getUserDoomPool(user).orElse(getActivePool());
    }

    public static EmbedBuilder createPool(String poolName, int count) {
        if (createDoomPool(poolName, count)) {
            return new EmbedBuilder()
                    .setTitle(DOOM)
                    .setDescription(MessageFormat.format("I''ve created the doom pool ''**{0}**'', which contains {1} doom points.", poolName, count));
        } else {
            return new EmbedBuilder()
                    .setTitle(DOOM)
                    .setDescription(MessageFormat.format("Failed to create doom pool ''**{0}**''.", poolName));
        }
    }

    @Nullable
    public static String findPool(String poolName) {
        if (getDoomPools().stream().anyMatch(pool -> pool.getName().equalsIgnoreCase(poolName))) {
            return poolName;
        } else {
            List<String> potentialPoolNames = Lists.newArrayList();
            int currentDistance = Integer.MAX_VALUE;
            for (Doom existingPool : getDoomPools()) {
                int distance = DamerauLevenshtein.calculateDistance(poolName, existingPool.getName());
                if (distance < currentDistance) {
                    potentialPoolNames.clear();
                    potentialPoolNames.add(existingPool.getName());
                    currentDistance = distance;
                } else if (distance == currentDistance) {
                    potentialPoolNames.add(existingPool.getName());
                }
            }

            if (potentialPoolNames.isEmpty()) {
                return null;
            } else if (potentialPoolNames.size() == 1) {
                if (currentDistance <= 2) {
                    return potentialPoolNames.get(0);
                } else {
                    return null;
                }
            } else {
                LOGGER.warn("Found multiple doom pool names with same level of similarity");
                return null;
            }
        }
    }

    public static DoomHandler getInstance() {
        return instance;
    }

}