package rolling;

public class Roll {
    private String type;
    private int value;

    public Roll(String type, int value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}
