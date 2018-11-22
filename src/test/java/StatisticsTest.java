import logic.StatisticsGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatisticsTest {

    private StatisticsGenerator generator;

    @BeforeEach
    public void setUp(){
        generator = new StatisticsGenerator("d10 2d12 pd10 pd12");
    }

    @Test
    public void correctLists(){
        assertEquals(generator.getDiceList().size(), 3);
        assertEquals(generator.getPlotDice().size(), 2);
    }
}
