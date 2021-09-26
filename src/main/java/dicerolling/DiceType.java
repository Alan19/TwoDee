package dicerolling;

public enum DiceType {
    REGULAR("d"),
    PLOT_DIE("pd"),
    KEPT_DIE("kd"),
    CHAOS_DIE("cd"),
    ENHANCED_DIE("ed");

    private final String abbreviation;

    private DiceType(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
