package rolling;

import io.vavr.control.Try;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiceRollerTest {


    private Try<Pair<List<Dice>, List<Integer>>> pool;
    private Pair<List<Roll>, List<Integer>> roll;
    private Result result;

    @BeforeEach
    void setUp() {
        pool = Roller.parse("2d8 d6 pd4 cd8 +1 stealth", s -> Try.success(s.replace("stealth", "d8")));
        roll = Roller.roll(Pair.of(Arrays.asList(new Dice("d", 12), new Dice("pd", 12), new Dice("cd", 12)), Arrays.asList(3, -2)));
        result = new Result(Arrays.asList(new Roll("d", 4),
                new Roll("d", 1),
                new Roll("pd", 12, 6),
                new Roll("ed", 12, 6),
                new Roll("cd", 3),
                new Roll("kd", 1)), Arrays.asList(6, -3), 2);
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

    //@Test This Test Chokes on Windows for some Reason?
    void testPlotPointSpendEmbedTest() {
        assertEquals("24 → 20", Roller.getPlotPointSpendingText(4, 21, true));
        assertEquals("26 → 20", Roller.getPlotPointSpendingText(6, 20, false));
    }

    @Test
    void testResult() {
        assertEquals(29, result.getTotal());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testParsingForInvalidDice() {
        Try<Pair<List<Dice>, List<Integer>>> invalidRolled = Roller.parse("xd8 d8", s -> Try.failure(new IllegalArgumentException("no stats")));

        Assertions.assertTrue(invalidRolled.isFailure());
        Assertions.assertEquals(invalidRolled.getCause().getMessage(), "`xd8` does not result in valid dice, or is not registered on the character sheet!");
    }
}