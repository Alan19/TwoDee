package rolling;

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
}
