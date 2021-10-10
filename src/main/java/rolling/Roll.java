package rolling;

public class Roll {
    private String type;
    private int value;
    private int enhancedValue;

    public Roll(String type, int value) {
        this.type = type;
        this.value = value;
    }

    public Roll(String type, int value, int enhancedValue) {
        this.type = type;
        this.value = value;
        this.enhancedValue = enhancedValue;
    }

    public String getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public int getEnhancedValue() {
        return enhancedValue;
    }
}
