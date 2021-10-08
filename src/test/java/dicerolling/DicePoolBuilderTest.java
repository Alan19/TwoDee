package dicerolling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class DicePoolBuilderTest {

    @BeforeEach
    void setUp() {
        DicePoolBuilder pool = new DicePoolBuilder("2d8 d6 pd4 cd8 +1");
        pool.getResults()
    }

    @AfterEach
    void tearDown() {
    }
}