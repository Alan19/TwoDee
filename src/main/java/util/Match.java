package util;

public enum Match {
    EXACT("Exact Match"),
    CLOSE("Close Match"),
    NONE("No Match");

    private final String text;

    Match(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }
}
