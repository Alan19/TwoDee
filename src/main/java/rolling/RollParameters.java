package rolling;

public record RollParameters(String pool, int discount, Boolean enhanceable, boolean opportunity, int diceKept,
                             boolean devastating) {
}
