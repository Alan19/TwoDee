package rolling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import statistics.DicePool;
import statistics.DicePoolStatistics;
import statistics.StatisticsRollResult;

public class StatisticsTest {
    @Test
    public void testStatistics() {
        DicePoolStatistics statistics = new DicePoolStatistics(new DicePool("3d12 +5 pd12"));
        Assertions.assertEquals(statistics.getResults().keySet().stream().mapToInt(StatisticsRollResult::getTotal).min().getAsInt(), 13);
        Assertions.assertEquals(statistics.getResults().keySet().stream().mapToInt(StatisticsRollResult::getTotal).max().getAsInt(), 41);

    }
}
