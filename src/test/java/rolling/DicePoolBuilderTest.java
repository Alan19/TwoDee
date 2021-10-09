package rolling;

import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DicePoolBuilderTest {


    private Try<Pair<List<Dice>, List<Integer>>> pool;

    @BeforeEach
    void setUp() {
        pool = Roller.parse("2d8 d6 pd4 cd8 +1 stealth", strings -> Try.success(Collections.singletonList(new Dice("d", 8))));
    }

    @Test
    void testDiceCount() {
        assertEquals(6, pool.map(Pair::getLeft).get().size());
    }

    @Test
    void testModifiers() {
        assertEquals(1, pool.map(Pair::getRight).get().size());
    }

    @AfterEach
    void tearDown() {
    }
}