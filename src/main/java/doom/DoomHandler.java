package doom;

import com.google.common.collect.Lists;
import configs.DoomSettings;
import configs.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import roles.Player;
import roles.PlayerHandler;
import roles.Storytellers;
import util.DamerauLevenshtein;

import javax.annotation.Nullable;
import java.awt.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DoomHandler {

    private static final Logger LOGGER = LogManager.getLogger(DoomHandler.class);

    public static final String DOOM = "Doom!";
    private static final DoomHandler instance = new DoomHandler();
    private final DoomSettings doomSettings;

    private DoomHandler() {
        doomSettings = Settings.getDoom();
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
        final int oldDoom = getDoom(pool);
        if (oldDoom == 0) {
            return new EmbedBuilder()
                    .setTitle("Error")
                    .setDescription("No Doom Pool with Name ''**" + pool + "**'' exists.");
        }
        final int newDoom = getDoomPools().compute(pool, (s, integer) -> integer != null ? integer + doomVal : doomVal);
        Settings.serializePersonalSettings();
        return generateDoomEmbed(pool, oldDoom, newDoom);
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
        }
        else {
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
        return getDoomPools().getOrDefault(pool, 0);
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
    public static EmbedBuilder setDoom(String pool, int newDoom) {
        int oldDoom = getDoom(pool);
        if (oldDoom == 0) {
            return new EmbedBuilder()
                    .setTitle("Error")
                    .setDescription("No Doom Pool with Name ''**" + pool + "**'' exists.");
        }
        getDoomPools().put(pool, newDoom);
        Settings.serializePersonalSettings();
        return generateDoomEmbed(pool, oldDoom, newDoom);
    }

    /**
     * Generates an embed that shows the amount of doom points in the specified doom pool.
     *
     * @param pool The name of the doom pool to check
     * @return The an embed containing the value of the doom pool
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
        final Integer removedDoom = getDoomPools().remove(pool);
        final String description = Optional.ofNullable(removedDoom).map(doom -> MessageFormat.format("I''ve removed the ''**{0}**'' doom pool, which contained {1} doom points.", pool, doom)).orElseGet(() -> MessageFormat.format("I was unable to find the ''**{0}**'' doom pool", pool));
        Settings.serializePersonalSettings();
        return new EmbedBuilder()
                .setTitle(DOOM)
                .setDescription(description);
    }

    /**
     * Generates an embed that provides a summary of all of the doom pools.
     *
     * @return Am embed with information on the names and values of all available doom pools and the name and value of
     * the active doom pool
     */
    public static EmbedBuilder generateDoomEmbed() {
        final EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(DOOM).setDescription(MessageFormat.format("Here are the values of all doom pools.\nThe current active doom pool is ''**{0}**'' with {1} doom points", getActivePool(), getDoom(getActivePool()))).setColor(new Color((int) (getDoom() % 101 * (2.55))));
        getDoomPools().forEach((key, value) -> embedBuilder.addField(key, String.valueOf(value)));
        return embedBuilder;
    }

    public static Map<String, Integer> getDoomPools() {
        return instance.doomSettings.getDoomPools();
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
        getDoomPools().put(poolName, count);
        Settings.serializePersonalSettings();
        return new EmbedBuilder()
                .setTitle(DOOM)
                .setDescription(MessageFormat.format("I''ve created the doom pool ''**{0}**'', which contains {1} doom points.", poolName, count));
    }

    @Nullable
    public static String findPool(String poolName) {
        if (getDoomPools().containsKey(poolName)) {
            return poolName;
        }
        else {
            List<String> potentialPoolNames = Lists.newArrayList();
            int currentDistance = Integer.MAX_VALUE;
            for (String existingPool : getDoomPools().keySet()) {
                int distance = DamerauLevenshtein.calculateDistance(poolName, existingPool);
                if (distance < currentDistance) {
                    potentialPoolNames.clear();
                    potentialPoolNames.add(existingPool);
                    currentDistance = distance;
                }
                else if (distance == currentDistance) {
                    potentialPoolNames.add(existingPool);
                }
            }

            if (potentialPoolNames.isEmpty()) {
                return null;
            }
            else if (potentialPoolNames.size() == 1) {
                if (currentDistance <= 2) {
                    return potentialPoolNames.get(0);
                }
                else {
                    return null;
                }
            }
            else {
                LOGGER.warn("Found multiple doom pool names with same level of similarity");
                return null;
            }
        }
    }

}