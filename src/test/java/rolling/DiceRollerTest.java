package rolling;

import io.vavr.control.Try;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiceRollerTest {


    private Try<Pair<List<Dice>, List<Integer>>> pool;
    private Pair<List<Roll>, List<Integer>> roll;

    @BeforeEach
    void setUp() {
        pool = Roller.parse("2d8 d6 pd4 cd8 +1 stealth", strings -> Try.success(Collections.singletonList(new Dice("d", 8))));
        roll = Roller.roll(Pair.of(Arrays.asList(new Dice("d", 12), new Dice("pd", 12), new Dice("cd", 12)), Arrays.asList(3, -2)));
    }

    @Test
    void testDiceCount() {
        assertEquals(6, pool.map(Pair::getLeft).get().size());
    }

    @Test
    void testModifiers() {
        assertEquals(1, pool.map(Pair::getRight).get().size());
    }

    @Test
    void testRoll() {
        assertEquals(3, roll.getLeft().size());
        assertTrue(roll.getLeft().stream().mapToInt(Roll::getValue).allMatch(value -> Range.between(1, 12).contains(value)));
        assertEquals(Arrays.asList(3, -2), roll.getRight());
    }

    @Test
    void testPlotPointSpendEmbedTest() {
        assertEquals("24 → 20", Roller.getPlotPointSpendingText(4, 21, true));
        assertEquals("26 → 20", Roller.getPlotPointSpendingText(6, 20, false));
    }

    @AfterEach
    void tearDown() {
    }
}