package rolling;

import java.util.Objects;

public class Dice {
    private final String name;
    private final int value;

    public Dice(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public DiceType getType() {
        switch (this.getName()) {
            case "cd":
                return DiceType.CHAOS_DIE;
            case "d":
                return DiceType.REGULAR;
            case "ed":
                return DiceType.ENHANCED_DIE;
            case "kd":
                return DiceType.KEPT_DIE;
            case "pd":
                return DiceType.PLOT_DIE;
        }
        throw new IllegalArgumentException("Invalid Dice Type");
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dice dice = (Dice) o;
        return value == dice.value && Objects.equals(name, dice.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
