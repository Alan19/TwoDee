package rolling;

import java.util.Objects;

public record Dice(String name, int value) {

    public DiceType getType() {
        return switch (this.name()) {
            case "cd" -> DiceType.CHAOS_DIE;
            case "d" -> DiceType.REGULAR;
            case "ed" -> DiceType.ENHANCED_DIE;
            case "kd" -> DiceType.KEPT_DIE;
            case "pd" -> DiceType.PLOT_DIE;
            default -> throw new IllegalArgumentException("Invalid Dice Type");
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Dice dice = (Dice) o;
        return value == dice.value && Objects.equals(name, dice.name);
    }

    @Override
    public String toString() {
        return name + value;
    }
}
