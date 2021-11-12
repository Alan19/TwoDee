package sheets;

import doom.DoomHandler;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.user.User;
import roles.Storytellers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Class to manage modifications to the plot point and bleed cells
 */
public class PlotPointUtils {

    public static CompletableFuture<Optional<Integer>> addPlotPointsToUser(User user, Integer amount) {
        final Optional<Integer> oldPlotPointCount = SheetsHandler.getPlotPoints(user);
        if (oldPlotPointCount.isPresent()) {
            final int newPlotPointCount = oldPlotPointCount.get() + amount;
            return SheetsHandler.setPlotPoints(user, newPlotPointCount);
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Handles changes in plot points on a roll. If a Storyteller rolls, use doom points from the active doom pool instead.
     *
     * @param user   The user that rolled
     * @param amount The amount of plot points to add
     * @return The new amount of plot / doom points. If the user does not have a character sheet, returns the amount of plot points that get added.
     */
    public static CompletableFuture<Integer> addPlotPointsOnRoll(User user, int amount) {
        if (Storytellers.isUserStoryteller(user)) {
            return CompletableFuture.completedFuture(DoomHandler.addDoom(amount));
        }
        else {
            final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
            if (plotPoints.isPresent()) {
                return Try.success(plotPoints.get()).toCompletableFuture().thenCompose(integer -> SheetsHandler.setPlotPoints(user, integer + amount).thenApply(Optional::get));
            }
            else {
                return CompletableFuture.completedFuture(amount);
            }
        }
    }

    public static CompletableFuture<PlotPointChangeResult> addPlotPointsToUsers(Collection<User> users, Integer amount) {
        List<Triple<User, Integer, Integer>> changes = new ArrayList<>();
        List<User> errors = new ArrayList<>();
        final Stream<CompletableFuture<Void>> addPointsFuture = users.stream().map(user -> addPlotPointsToUser(user, amount)
                .thenAccept(integer -> {
                    if (integer.isPresent()) {
                        changes.add(Triple.of(user, integer.get() - amount, integer.get()));
                    }
                    else {
                        errors.add(user);
                    }
                }));

        return CompletableFuture.allOf(addPointsFuture.toArray(CompletableFuture[]::new)).thenApply(unused -> new PlotPointChangeResult(changes, errors));
    }
}
