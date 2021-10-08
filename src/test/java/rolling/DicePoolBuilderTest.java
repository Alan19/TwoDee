package rolling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DicePoolBuilderTest {

    private DicePoolBuilder pool;

    @BeforeEach
    void setUp() {
        pool = new DicePoolBuilder("2d8 d6 pd4 cd8 +1 stealth", s -> s.replaceFirst("stealth", "d4"));
    }

    @Test
    void testDiceCount() {
        assertEquals(4, pool.getRegularDice().size());
        assertEquals(1, pool.getChaosDice().size());
        assertEquals(1, pool.getPlotDice().size());
        assertEquals(1, pool.getFlatBonuses().size());
    }

    @AfterEach
    void tearDown() {
    }
}