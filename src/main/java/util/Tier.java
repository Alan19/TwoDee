package util;

public enum Tier {
    NONE("None", -10000),
    EASY("Easy", 3),
    AVERAGE("Average", 7),
    HARD("Hard", 11),
    FORMIDABLE("Formidable", 15),
    HEROIC("Heroic", 19),
    INCREDIBLE("Incredible", 23),
    RIDICULOUS("Ridiculous", 27),
    IMPOSSIBLE("Impossible", 31);

    private final String name;
    private final int min;

    Tier(String name, int min) {
        this.name = name;
        this.min = min;
    }

    public static Tier getByIndex(int index) {
        int actual = Math.max(0, Math.min(Tier.values().length, index));

        return Tier.values()[actual];
    }
}
