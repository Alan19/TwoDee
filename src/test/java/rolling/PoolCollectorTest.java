package rolling;

import io.vavr.control.Either;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class PoolCollectorTest {

    @Test
    void validDiceShouldSuccess() {
        Try<Pair<List<Dice>, List<Integer>>> dice = Stream.of(
                        Try.success(Either.<List<Dice>, Integer>left(Collections.singletonList(new Dice("d", 4))))
                )
                .collect(PoolCollector.toTripleList());

        Assertions.assertTrue(dice.isSuccess());
        Assertions.assertEquals(dice.get(), Pair.<List<Dice>, List<Integer>>of(
                Collections.singletonList(new Dice("d", 4)),
                Collections.emptyList()
        );
    }

    @Test
    void failedDiceShouldFailed() {
        Try<Pair<List<Dice>, List<Integer>>> dice = Stream.of(
                        Try.<Either<List<Dice>, Integer>>failure(new IllegalArgumentException("Invalid Dice Type"))
                )
                .collect(PoolCollector.toTripleList());

        Assertions.assertTrue(dice.isFailure());
        Assertions.assertEquals(dice.getCause(), new IllegalArgumentException("Invalid Dice Type"));
    }
}
