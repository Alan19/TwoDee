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
