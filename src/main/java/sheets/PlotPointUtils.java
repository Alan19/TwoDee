package sheets;

import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Triple;
import org.javacord.api.entity.user.User;

import java.text.MessageFormat;
import java.util.*;
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

    public static CompletableFuture<Integer> addPlotPointsToPlayer(User user, int amount) {
        final Optional<Integer> plotPoints = SheetsHandler.getPlotPoints(user);
        if (plotPoints.isPresent()) {
            return Try.success(plotPoints.get()).toCompletableFuture().thenCompose(integer -> SheetsHandler.setPlotPoints(user, integer + amount).thenApply(Optional::get));
        }
        else {
            return Try.failure(new NoSuchElementException(MessageFormat.format("Unable to retrieve {0}''s plot points!", user))).toCompletableFuture().thenApply(o -> (int) o);
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
