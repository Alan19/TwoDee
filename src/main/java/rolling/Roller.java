package rolling;

import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import util.UtilFunctions;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static rolling.DicePoolBuilder.*;

public class Roller {
    public static Try<Pair<List<Dice>, List<Integer>>> parse(String pool, Function<List<String>, Try<List<Dice>>> operator) {
        String[] paramArray = pool.split(" ");
        final Triple<List<Dice>, List<Integer>, List<String>> diceModifierSkillTuple = Arrays.stream(paramArray)
                .map(Roller::processPoolParam)
                .collect(PoolCollector.toTripleList());
        final List<String> skills = diceModifierSkillTuple.getRight();
        final List<Dice> dice = diceModifierSkillTuple.getLeft();
        final List<Integer> modifiers = diceModifierSkillTuple.getMiddle();
        return operator.apply(skills).map(skill -> Pair.of(ImmutableList.<Dice>builder().addAll(dice).addAll(skill).build(), modifiers));
    }

    private static Triple<List<Dice>, Integer, String> processPoolParam(String s) {
        final Matcher diceMatcher = DICE_PATTERN.matcher(s);
        final Matcher flatBonusMatcher = FLAT_BONUS_PATTERN.matcher(s);
        final Matcher flatPenaltyMatcher = FLAT_PENALTY_PATTERN.matcher(s);
        //Any type of dice
        if (diceMatcher.matches()) {
            final int count = UtilFunctions.tryParseInt(diceMatcher.group("count")).orElse(1);
            final String type = diceMatcher.group("type");
            final int facets = Integer.parseInt(diceMatcher.group("facets"));
            return Triple.of(IntStream.range(0, count).mapToObj(value -> new Dice(type, facets)).collect(Collectors.toList()), null, null);
        }
        //Flat bonus
        else if (flatBonusMatcher.matches()) {
            return Triple.of(null, Integer.parseInt(flatBonusMatcher.group("value")), null);
        }
        //Flat penalty
        else if (flatPenaltyMatcher.matches()) {
            return Triple.of(null, Integer.parseInt(flatBonusMatcher.group("value")) * -1, null);
        }
        else {
            return Triple.of(null, null, s);
        }
    }
}
