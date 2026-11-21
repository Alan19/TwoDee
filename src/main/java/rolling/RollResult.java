package rolling;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Stream;

public class RollResult {
    private final EnumMap<ResultDiceCategories, List<Integer>> rolledDice = new EnumMap<>(ResultDiceCategories.class);
    private final int kept;
    private final int plotDiceCost;
    private final Random random = new SecureRandom();

    // Manual entry for testing
    public RollResult(List<RolledDie> rolls, List<Integer> flatBonuses, int diceKept) {
        kept = diceKept;
        Arrays.stream(ResultDiceCategories.values()).forEach(category -> rolledDice.put(category, new ArrayList<>()));

        plotDiceCost = rolls.stream().filter(roll -> roll.getType().equals("pd")).mapToInt(RolledDie::getEnhancedValue).sum();
        rolls.forEach(this::acceptDice);
        rolledDice.put(ResultDiceCategories.FLAT_BONUS, flatBonuses);
    }

    // Alternative constructor that rolls the dice in the constructor
    public RollResult(Map<DiceType, List<Integer>> dicePool, int flatBonusSum, int diceKept) {
        kept = diceKept;
        for (ResultDiceCategories category : ResultDiceCategories.values()) {
            rolledDice.put(category, new ArrayList<>());
        }

        boolean plotDieMinimum = dicePool.getOrDefault(DiceType.PLOT_DIE, new ArrayList<>()).size() + dicePool.getOrDefault(DiceType.ENHANCED_DIE, new ArrayList<>()).size() > 1;
        plotDiceCost = dicePool.getOrDefault(DiceType.PLOT_DIE, new ArrayList<>()).stream().mapToInt(integer -> integer / 2).sum();
        dicePool.forEach((diceType, diceFacets) -> {
            for (Integer facets : diceFacets) {
                acceptDice(diceType, facets, plotDieMinimum);
            }
        });
        rolledDice.put(ResultDiceCategories.FLAT_BONUS, List.of(flatBonusSum));
    }


    private void acceptDice(DiceType dice, int facets, boolean plotDieMinimum) {
        switch (dice) {
            case REGULAR -> rolledDice.get(ResultDiceCategories.REGULAR).add(random.nextInt(1, facets + 1));
            case ASSURED_DIE -> rolledDice.get(ResultDiceCategories.REGULAR).add(facets);
            case PLOT_DIE, ENHANCED_DIE -> rolledDice.get(ResultDiceCategories.PLOT_DIE).add(plotDieMinimum ? Math.max(facets / 2, random.nextInt(1, facets + 1)) : random.nextInt(1, facets + 1));
            case KEPT_DIE -> rolledDice.get(ResultDiceCategories.KEPT_DIE).add(random.nextInt(1, facets + 1));
            case CHAOS_DIE -> rolledDice.get(ResultDiceCategories.CHAOS_DIE).add(random.nextInt(facets * -1 + 1, 0));
        }
    }

    private void acceptDice(RolledDie roll) {
        switch (roll.getType()) {
            case "pd", "ed" -> rolledDice.get(ResultDiceCategories.PLOT_DIE).add(roll.getValue());
            case "cd" -> rolledDice.get(ResultDiceCategories.CHAOS_DIE).add(roll.getValue() * -1);
            case "kd" -> rolledDice.get(ResultDiceCategories.KEPT_DIE).add(roll.getValue());
            default -> rolledDice.get(ResultDiceCategories.REGULAR).add(roll.getValue());
        }
    }

    public int getTotal() {
        return getPickedDice().stream().mapToInt(Integer::intValue).sum() + getFlatBonuses().stream().mapToInt(Integer::intValue).sum();
    }

    public List<Integer> getDroppedDice() {
        final Stream<Integer> droppedRegularDice = Stream.concat(rolledDice.get(ResultDiceCategories.REGULAR).stream(), getPlotDice().stream().skip(1)).sorted(Comparator.reverseOrder()).skip(kept);
        return Stream.concat(getChaosDiceStream().sorted(Comparator.naturalOrder()).skip(2), droppedRegularDice).toList();
    }

    public List<Integer> getFlatBonuses() {
        return rolledDice.get(ResultDiceCategories.FLAT_BONUS);
    }

    public List<Integer> getPickedDice() {
        Stream<Integer> pickedRegularDice = Stream.concat(rolledDice.get(ResultDiceCategories.REGULAR).stream(), getPlotDice().stream().sorted(Comparator.reverseOrder()).skip(1)).sorted(Comparator.reverseOrder()).limit(kept);
        pickedRegularDice = Stream.concat(pickedRegularDice, getPlotDice().stream().sorted(Comparator.reverseOrder()).limit(1));
        final Stream<Integer> pickedChaosDiceAndKeptDice = Stream.concat(getChaosDiceStream().sorted(Comparator.naturalOrder()).limit(2), rolledDice.get(ResultDiceCategories.KEPT_DIE).stream());
        return Stream.concat(pickedRegularDice, pickedChaosDiceAndKeptDice).sorted(Comparator.reverseOrder()).toList();
    }

    public List<Integer> getKeptDice() {
        return rolledDice.get(ResultDiceCategories.KEPT_DIE);
    }

    public List<Integer> getPlotDice() {
        return rolledDice.get(ResultDiceCategories.PLOT_DIE);
    }

    private Stream<Integer> getChaosDiceStream() {
        return rolledDice.get(ResultDiceCategories.CHAOS_DIE).stream();
    }

    public List<Integer> getRegularAndChaosDice() {
        return Stream.concat(rolledDice.get(ResultDiceCategories.REGULAR).stream(), rolledDice.get(ResultDiceCategories.CHAOS_DIE).stream()).toList();
    }

    public int getOpportunities() {
        return Math.toIntExact(Stream.of(rolledDice.get(ResultDiceCategories.REGULAR), rolledDice.get(ResultDiceCategories.PLOT_DIE), rolledDice.get(ResultDiceCategories.KEPT_DIE))
                .flatMap(Collection::stream)
                .filter(integer -> integer == 1)
                .count());
    }

    public int getPlotDiceCost() {
        return plotDiceCost;
    }
}
