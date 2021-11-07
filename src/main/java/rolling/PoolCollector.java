package rolling;

import io.vavr.control.Either;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class PoolCollector implements Collector<Try<Either<List<Dice>, Integer>>, List<Try<Either<List<Dice>, Integer>>>, Try<Pair<List<Dice>, List<Integer>>>> {
    public static PoolCollector toTripleList() {
        return new PoolCollector();
    }

    @Override
    public Supplier<List<Try<Either<List<Dice>, Integer>>>> supplier() {
        return ArrayList::new;
    }

    /**
     * An accumulator to accumulate the incoming triples into a single triple with 3 lists. Incoming elements are not merged if they are null.
     *
     * @return A BiConsumer with instructions on how to merge incoming triples into the container
     */
    @Override
    public BiConsumer<List<Try<Either<List<Dice>, Integer>>>, Try<Either<List<Dice>, Integer>>> accumulator() {
        return List::add;
    }

    @Override
    public BinaryOperator<List<Try<Either<List<Dice>, Integer>>>> combiner() {
        return (tryPair1, tryPair2) -> {
            tryPair1.addAll(tryPair2);
            return tryPair1;
        };
    }

    @Override
    public Function<List<Try<Either<List<Dice>, Integer>>>, Try<Pair<List<Dice>, List<Integer>>>> finisher() {
        return tryPair -> Try.sequence(tryPair)
                .map(seq -> {
                    List<Dice> dice = new ArrayList<>();
                    List<Integer> bonus = new ArrayList<>();

                    for (Either<List<Dice>, Integer> either : seq) {
                        either.peekLeft(dice::addAll);
                        either.peek(bonus::add);
                    }

                    return Pair.of(dice, bonus);
                });
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
