package rolling;

import io.vavr.control.Either;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class PoolCollector implements Collector<Try<Either<List<Dice>, Integer>>, Try<Pair<List<Dice>, List<Integer>>>, Try<Pair<List<Dice>, List<Integer>>>> {
    public static PoolCollector toTripleList() {
        return new PoolCollector();
    }

    @Override
    public Supplier<Try<Pair<List<Dice>, List<Integer>>>> supplier() {
        return () -> Try.success(Pair.of(new ArrayList<>(), new ArrayList<>()));
    }

    /**
     * An accumulator to accumulate the incoming triples into a single triple with 3 lists. Incoming elements are not merged if they are null.
     *
     * @return A BiConsumer with instructions on how to merge incoming triples into the container
     */
    @Override
    public BiConsumer<Try<Pair<List<Dice>, List<Integer>>>, Try<Either<List<Dice>, Integer>>> accumulator() {
        return (list, incomingEither) -> list.andThen(pair -> incomingEither.andThen(integers -> {
            if (integers.isLeft()) {
                pair.getLeft().addAll(integers.getLeft());
            }
            else {
                pair.getRight().add(integers.get());
            }
        }));
    }

    @Override
    public BinaryOperator<Try<Pair<List<Dice>, List<Integer>>>> combiner() {
        return (tryPair1, tryPair2) -> tryPair1.andThen(pair1 -> tryPair2.andThen(pair2 -> {
            pair1.getLeft().addAll(pair2.getLeft());
            pair1.getRight().addAll(pair2.getRight());
        }));
    }

    @Override
    public Function<Try<Pair<List<Dice>, List<Integer>>>, Try<Pair<List<Dice>, List<Integer>>>> finisher() {
        return tryPair -> tryPair;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return new HashSet<>(Collections.singletonList(Characteristics.IDENTITY_FINISH));
    }
}
