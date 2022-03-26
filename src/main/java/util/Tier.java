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
}
