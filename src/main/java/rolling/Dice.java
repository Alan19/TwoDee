package rolling;

import java.util.Objects;

public record Dice(String name, int value) {

    public DiceType getType() {
        for (DiceType diceType : DiceType.values()) {
            if (name.equals(diceType.getAbbreviation())) {
                return diceType;
            }
        }
        throw new IllegalArgumentException("Invalid Dice Type");
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
