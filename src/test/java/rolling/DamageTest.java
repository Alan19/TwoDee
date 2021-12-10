package rolling;

import logic.DamageLogic;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DamageTest {

    private DamageLogic logic;

    @BeforeEach
    void setup() {
        logic = new DamageLogic();
    }

    @Test
    void testSplitEven() {
        final Pair<Long, Long> splitDamage = logic.splitDamage(DamageLogic.DamageType.BASIC, 10);
        Assertions.assertEquals(5, splitDamage.getLeft());
        Assertions.assertEquals(5, splitDamage.getRight());
    }

    @Test
    void testSplitOdd() {
        final Pair<Long, Long> splitDamage = logic.splitDamage(DamageLogic.DamageType.BASIC, 9);
        Assertions.assertEquals(5, splitDamage.getLeft());
        Assertions.assertEquals(4, splitDamage.getRight());
    }

    @Test
    void testSplitSimple() {
        final Pair<Long, Long> splitStun = logic.splitDamage(DamageLogic.DamageType.STUN, 5);
        Assertions.assertEquals(5, splitStun.getLeft());
        Assertions.assertEquals(0, splitStun.getRight());

        final Pair<Long, Long> splitWounds = logic.splitDamage(DamageLogic.DamageType.WOUND, 5);
        Assertions.assertEquals(0, splitWounds.getLeft());
        Assertions.assertEquals(5, splitWounds.getRight());
    }

    @Test
    void testBasic() {
        final Triple<Long, Long, Long> damage = logic.calculateDamage(DamageLogic.DamageType.BASIC, 12, 0, 5, 5);
        Assertions.assertEquals(1, damage.getMiddle());
        Assertions.assertEquals(1, damage.getRight());
    }
}
