package rolling;

import util.UtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

//TODO Have statistics use something different
public class DicePoolBuilder {

    public static final Pattern DICE_PATTERN = Pattern.compile("(?<count>\\d*)(?<type>[kpec]?d)(?<facets>\\d+)");
    public static final Pattern FLAT_BONUS_PATTERN = Pattern.compile("\\+(?<value>[1-9]\\d*)");
    public static final Pattern FLAT_PENALTY_PATTERN = Pattern.compile("-(?<value>[1-9]\\d*)");

    private final List<Integer> regularDice;
    private final List<Integer> plotDice;
    private final List<Integer> chaosDice;
    private final List<Integer> keptDice;
    private final List<Integer> enhancedDie;
    private final List<Integer> flatBonus;
    private final boolean devastating;
    private int diceKept = 2;

    public DicePoolBuilder(String pool, UnaryOperator<String> parseFunction, boolean devastating) {
        pool = parseFunction.apply(pool);
        regularDice = new ArrayList<>();
        plotDice = new ArrayList<>();
        chaosDice = new ArrayList<>();
        keptDice = new ArrayList<>();
        enhancedDie = new ArrayList<>();
        flatBonus = new ArrayList<>();
        this.devastating = devastating;

        if (pool.isEmpty()) {
            return;
        }

        String[] paramArray = pool.split(" ");
        for (String param : paramArray) {
            final Matcher diceMatcher = DICE_PATTERN.matcher(param);
            final Matcher flatBonusMatcher = FLAT_BONUS_PATTERN.matcher(param);
            final Matcher flatPenaltyMatcher = FLAT_PENALTY_PATTERN.matcher(param);
            //Any type of dice
            if (diceMatcher.matches()) {
                processDice(diceMatcher);
            }
            //Flat bonus
            else if (flatBonusMatcher.matches()) {
                flatBonus.add(Integer.parseInt(flatBonusMatcher.group("value")));
            }
            //Flat penalty
            else if (flatPenaltyMatcher.matches()) {
                flatBonus.add(-1 * Integer.parseInt(flatPenaltyMatcher.group("value")));
            }
        }
    }

    public boolean isDevastating() {
        return devastating;
    }

    private void processDice(Matcher diceMatcher) {
        final int count = UtilFunctions.tryParseInt(diceMatcher.group("count")).orElse(1);
        final String type = diceMatcher.group("type");
        final int facets = Integer.parseInt(diceMatcher.group("facets"));
        switch (type) {
            case "pd" -> IntStream.range(0, count).forEach(i -> plotDice.add(facets));
            case "cd" -> IntStream.range(0, count).forEach(i -> chaosDice.add(facets));
            case "kd" -> IntStream.range(0, count).forEach(i -> keptDice.add(facets));
            case "ed" -> IntStream.range(0, count).forEach(i -> enhancedDie.add(facets));
            default -> IntStream.range(0, count).forEach(i -> regularDice.add(facets));
        }
    }

    public DicePoolBuilder withDiceKept(Integer diceKept) {
        this.diceKept = diceKept;
        return this;
    }

    public List<Integer> getRegularDice() {
        return regularDice;
    }

    public List<Integer> getPlotDice() {
        return plotDice;
    }

    public List<Integer> getChaosDice() {
        return chaosDice;
    }

    public List<Integer> getKeptDice() {
        return keptDice;
    }

    public List<Integer> getEnhancedDice() {
        return enhancedDie;
    }

    public List<Integer> getFlatBonuses() {
        return flatBonus;
    }

    public int getDiceKept() {
        return diceKept;
    }

}
