package doom;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import roles.Player;
import roles.PlayerHandler;
import roles.Storytellers;
import util.DamerauLevenshtein;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DoomHandler {

    public static final int DOOM_DANGER_CAP = 301;
    private static Logger LOGGER = LogManager.getLogger(DoomHandler.class);

    public static final String DOOM = "Doom!";
    private static final DoomHandler instance = new DoomHandler();
    private DoomPools doomPoolsInstance;

    private DoomHandler() {
        try {
            doomPoolsInstance = new Gson().fromJson(new BufferedReader(new FileReader("resources/doom.json")), DoomPools.class);
        } catch (FileNotFoundException e) {
            doomPoolsInstance = new DoomPools();
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
        final int oldDoom = getDoom(pool);
        final Map<String, DoomPool> doomPools = instance.doomPoolsInstance.getDoomPools();
        if (doomPools.containsKey(pool)) {
            final int newDoom = doomPools.get(pool).setAndGetAmount(doomVal + oldDoom);
            serializePools();
            return generateDoomEmbed(pool, oldDoom, newDoom);
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Error")
                    .setDescription("No Doom Pool with Name ''**" + pool + "**'' exists.");
        }
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
        return Optional.ofNullable(instance.doomPoolsInstance.getDoomPools().get(pool)).map(DoomPool::getAmount).orElse(0);
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
        if (instance.doomPoolsInstance.getDoomPools().containsKey(pool)) {
            instance.doomPoolsInstance.getDoomPools().get(pool).setAndGetAmount(newDoom);
            serializePools();
            return generateDoomEmbed(pool, oldDoom, newDoom);
        }
        else {
            return new EmbedBuilder()
                    .setTitle("Error")
                    .setDescription("No Doom Pool with Name ''**" + pool + "**'' exists.");
        }
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
                .setColor(new Color((int) (doom % DOOM_DANGER_CAP * (2.55))));
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
                .setColor(new Color((int) (newDoom % DOOM_DANGER_CAP * (2.55))));
    }

    /**
     * Serializes the values of the various doom pools and writes it to doom.json
     */
    private static void serializePools() {
        try {
            final BufferedWriter writer = new BufferedWriter(new FileWriter("resources/doom.json"));
            new Gson().toJson(instance.doomPoolsInstance, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a doom pool and serialize the change.
     *
     * @param pool The name of the doom pool to delete
     * @return An embed containing information about the deleted doom pool and the number of doom points it had
     */
    public static EmbedBuilder deletePool(String pool) {
        final DoomPool removedDoom = instance.doomPoolsInstance.getDoomPools().remove(pool);
        final String description = Optional.ofNullable(removedDoom)
                .map(doom -> MessageFormat.format("I''ve removed the ''**{0}**'' doom pool, which contained {1} doom points.", pool, doom.getAmount()))
                .orElseGet(() -> MessageFormat.format("I was unable to find the ''**{0}**'' doom pool", pool));
        serializePools();
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
        final EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(DOOM)
                .setDescription(MessageFormat.format("Here are the values of all doom pools.\nThe current active doom pool is ''**{0}**'' with {1} doom points", getActivePool(), getDoom(getActivePool())))
                .setColor(new Color((int) (getDoom() % DOOM_DANGER_CAP * (2.55))));
        instance.doomPoolsInstance.getDoomPools().forEach((key, value) -> embedBuilder.addField(key, String.valueOf(value)));
        return embedBuilder;
    }

    /**
     * Getter for the value of the active pool
     *
     * @return The name of the active pool
     */
    public static String getActivePool() {
        return instance.doomPoolsInstance.getActivePool();
    }

    /**
     * Sets the active pool, serialize it, and return an embed with information about the pool.
     *
     * @param pool The doom pool to be set as the active pool
     * @return An embed with that includes the name and value of the new active doom pool
     */
    public static EmbedBuilder setActivePool(String pool) {
        instance.doomPoolsInstance.setActivePool(pool);
        serializePools();
        return new EmbedBuilder().setTitle(DOOM).setDescription(MessageFormat.format("I''ve set the active doom pool to ''**{0}**'', which contains {1} doom points.", pool, getDoom(pool)));
    }

    public static Optional<String> getUserDoomPool(User user) {
        return PlayerHandler.getPlayerFromUser(user).map(Player::getDoomPool);
    }

    public static String getDoomPoolOrDefault(User user) {
        return getUserDoomPool(user).orElse(getActivePool());
    }

    public static EmbedBuilder createPool(String poolName, int count, Role role) {
        instance.doomPoolsInstance.getDoomPools().put(poolName, new DoomPool(poolName, count, role.getId()));
        serializePools();
        return new EmbedBuilder()
                .setTitle(DOOM)
                .setDescription(MessageFormat.format("I''ve created the doom pool ''**{0}**'', which contains {1} doom points.", poolName, count));
    }

    @Nullable
    public static String findPool(String poolName) {
        if (instance.doomPoolsInstance.getDoomPools().containsKey(poolName)) {
            return poolName;
        }
        else {
            List<String> potentialPoolNames = Lists.newArrayList();
            int currentDistance = Integer.MAX_VALUE;
            for (String existingPool : instance.doomPoolsInstance.getDoomPools().keySet()) {
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
                return currentDistance <= 2 ? potentialPoolNames.get(0) : null;
            }
            else {
                LOGGER.warn("Found multiple doom pool names with same level of similarity");
                return null;
            }
        }
    }

}