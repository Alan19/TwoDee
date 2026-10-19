//package rolling;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import statistics.StatisticsLogic;
//import statistics.opposed.strategy.SuccessStrategy;
//
//import java.util.HashMap;
//
//public class OpposedStatisticsTest {
//    @Test
//    void testOpposedCheck() {
//        HashMap<BuildablePoolResult, Long> attackResults = new StatisticsLogic(new DicePoolBuilder("d2", s -> s, false)).results().get();
//        HashMap<BuildablePoolResult, Long> defenseResults = new StatisticsLogic(new DicePoolBuilder("d2", s -> s, false)).results().get();
//        System.out.println(new SuccessStrategy().getProbability(true, new StatisticsLogic(new DicePoolBuilder("2d12 d6 d10")).results().get(), new StatisticsLogic(new DicePoolBuilder("4d8 2d12")).results().get()));
//        System.out.println(new SuccessStrategy().getProbability(false, new StatisticsLogic(new DicePoolBuilder("4d8 2d12")).results().get(), new StatisticsLogic(new DicePoolBuilder("2d12 d6 d10")).results().get()));
//        Assertions.assertEquals(.75, new SuccessStrategy().getProbability(true, attackResults, defenseResults));
//    }
//}
