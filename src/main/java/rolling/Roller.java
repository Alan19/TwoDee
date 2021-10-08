package rolling;

import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import util.UtilFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static rolling.DicePoolBuilder.*;

public class Roller {
    public static Try<Pair<List<Dice>, List<Integer>>> parse(String pool, Function<List<String>, Try<List<Dice>>> operator) {
        List<Dice> dice = new ArrayList<>();
        List<Integer> modifiers = new ArrayList<>();
        List<String> skills = new ArrayList<>();

        String[] paramArray = pool.split(" ");
        Arrays.stream(paramArray).<Triple<List<Dice>, Integer, String>>map(s -> {
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
        }).forEach(triple -> {
            dice.addAll(triple.getLeft());
            modifiers.add(triple.getMiddle());
            skills.add(triple.getRight());
        });
        modifiers.removeIf(Objects::isNull);
        skills.removeIf(Objects::isNull);
        dice.removeIf(Objects::isNull);
        return operator.apply(skills).map(skill -> {
            dice.addAll(skill);
            return Pair.of(dice, modifiers);
        });
    }
}
