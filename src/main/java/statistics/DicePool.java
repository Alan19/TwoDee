package statistics;

import exceptions.InvalidUserInputException;
import rolling.DicePoolBuilder;
import rolling.DiceType;
import rolling.RollResult;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;

public class DicePool {
    private final List<Integer> flatBonus = new ArrayList<>();
    private final EnumMap<DiceType, List<Integer>> dicePoolMap = new EnumMap<>(DiceType.class);
    private final String input;

    public DicePool(String input) throws InvalidUserInputException {
        this.input = input;
        String[] dicePoolArray = input.split(" ");
        for (String die : dicePoolArray) {
            final Matcher diceMatcher = DicePoolBuilder.DICE_PATTERN.matcher(die);
            final Matcher flatBonusMatcher = DicePoolBuilder.FLAT_BONUS_PATTERN.matcher(die);
            final Matcher flatPenaltyMatcher = DicePoolBuilder.FLAT_PENALTY_PATTERN.matcher(die);
            //Any type of dice
            if (diceMatcher.matches()) {
                addDiceToPool(diceMatcher);
            }
            //Flat bonus
            else if (flatBonusMatcher.matches()) {
                flatBonus.add(Integer.parseInt(flatBonusMatcher.group("value")));
            }
            //Flat penalty
            else if (flatPenaltyMatcher.matches()) {
                flatBonus.add(-1 * Integer.parseInt(flatPenaltyMatcher.group("value")));
            }
            else {
                throw new InvalidUserInputException(MessageFormat.format("Invalid dice type: {0}", die));
            }
        }
        System.out.println("Your dice pool is " + input);
    }

    @Override
    public String toString() {
        return input;
    }

    public Map<DiceType, List<Integer>> getDicePoolMap() {
        return dicePoolMap;
    }

    public int getFlatBonusSum() {
        return flatBonus.stream().mapToInt(value -> value).sum();
    }

    private void addDiceToPool(Matcher diceMatcher) {
        //noinspection OptionalGetWithoutIsPresent We are guaranteed to get a value with the regex
        DiceType matchedDieType = Arrays.stream(DiceType.values())
                .filter(diceType -> diceType.getAbbreviation().equals(diceMatcher.group("type")))
                .findFirst()
                .get();
        String count = diceMatcher.group("count");
        int diceCount = count.isEmpty() ? 1 : Integer.parseInt(count);
        int facets = Integer.parseInt(diceMatcher.group("facets"));
        for (int i = 0; i < diceCount; i++) {
            dicePoolMap.computeIfAbsent(matchedDieType, _ -> new ArrayList<>()).add(facets);
        }
    }

    public RollResult roll(int diceKept) {
        return new RollResult(dicePoolMap, getFlatBonusSum(), diceKept);
    }

    public boolean isPlotDiceUsed() {
        return !dicePoolMap.getOrDefault(DiceType.PLOT_DIE, new ArrayList<>()).isEmpty() && !dicePoolMap.getOrDefault(DiceType.PLOT_DIE, new ArrayList<>()).isEmpty();
    }
}
