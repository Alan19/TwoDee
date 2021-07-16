package dicerolling;

import org.javacord.api.entity.user.User;
import sheets.SheetsHandler;
import util.UtilFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

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
    private int diceKept = 2;
    private boolean opportunity = true;
    private int discount;
    private boolean enhanceable;
    private boolean errored;


    public DicePoolBuilder(User user, String pool) {
        regularDice = new ArrayList<>();
        plotDice = new ArrayList<>();
        chaosDice = new ArrayList<>();
        keptDice = new ArrayList<>();
        enhancedDie = new ArrayList<>();
        flatBonus = new ArrayList<>();

        final Optional<Map<String, Integer>> skills = SheetsHandler.getSkills(user);

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
                flatBonus.add(-1 * Integer.parseInt(flatBonusMatcher.group("value")));
            }
            //Skill
            else {
                final String lowercaseSkill = param.toLowerCase();
                final Optional<Integer> skillFacets = skills.flatMap(stringIntegerMap -> stringIntegerMap.containsKey(lowercaseSkill) ? Optional.of(stringIntegerMap.get(lowercaseSkill)) : Optional.empty());
                if (skillFacets.isPresent()) {
                    regularDice.addAll(splitSkillFacets(skillFacets.get()));
                }
                else {
                    errored = true;
                }
            }
        }
    }

    private void processDice(Matcher diceMatcher) {
        final int count = UtilFunctions.tryParseInt(diceMatcher.group("count")).orElse(1);
        final String type = diceMatcher.group("type");
        final int facets = Integer.parseInt(diceMatcher.group("facets"));
        switch (type) {
            case "pd":
                IntStream.range(0, count).forEach(i -> plotDice.add(facets));
                break;
            case "cd":
                IntStream.range(0, count).forEach(i -> chaosDice.add(facets));
                break;
            case "kd":
                IntStream.range(0, count).forEach(i -> keptDice.add(facets));
                break;
            case "ed":
                IntStream.range(0, count).forEach(i -> enhancedDie.add(facets));
                break;
            default:
                IntStream.range(0, count).forEach(i -> regularDice.add(facets));
                break;
        }
    }

    private List<Integer> splitSkillFacets(int skillFacets) {
        List<Integer> dice = new ArrayList<>();
        final int d12Dice = skillFacets / 12;
        final int remainder = skillFacets % 12;
        for (int i = 0; i < d12Dice; i++) {
            dice.add(12);
        }
        if (remainder >= 4) {
            dice.add(remainder);
        }
        return dice;
    }

    public DicePoolBuilder withDiceKept(Integer diceKept) {
        this.diceKept = diceKept;
        return this;
    }

    public DicePoolBuilder withDiscount(Integer discount) {
        this.discount = discount;
        return this;
    }

    public DicePoolBuilder withEnhanceable(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Boolean> enhanceable) {
        boolean defaultEnhanceable = plotDice.isEmpty() && enhancedDie.isEmpty();
        this.enhanceable = enhanceable.orElse(defaultEnhanceable);
        return this;
    }

    public DicePoolBuilder withOpportunity(Boolean opportunity) {
        this.opportunity = opportunity;
        return this;
    }

    public Optional<RollResult> getResults() {
        if (!errored) {
            return Optional.of(new RollResult(regularDice, plotDice, keptDice, chaosDice, enhancedDie, flatBonus, diceKept, discount, opportunity, enhanceable));
        }
        else {
            return Optional.empty();
        }
    }
}
