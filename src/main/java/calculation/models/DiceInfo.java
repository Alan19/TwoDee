package calculation.models;

import rolling.DiceType;

public class DiceInfo {
    private final DiceType diceType;
    private final int rolled;
    private final int result;

    public DiceInfo(DiceType diceType, int rolled, int result) {
        this.diceType = diceType;
        this.rolled = rolled;
        this.result = result;
    }

    public DiceType getDiceType() {
        return diceType;
    }

    public int getRolled() {
        return rolled;
    }

    public int getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "DiceInfo{" +
                "diceType=" + diceType +
                ", rolled=" + rolled +
                ", result=" + result +
                '}';
    }
}
