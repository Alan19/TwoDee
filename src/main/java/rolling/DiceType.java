package rolling;

import java.util.List;
import java.util.stream.IntStream;

public enum DiceType {
    REGULAR("d"),
    PLOT_DIE("pd"),
    KEPT_DIE("kd"),
    CHAOS_DIE("cd"),
    ENHANCED_DIE("ed"),
    ASSURED_DIE("ad");

    private final String abbreviation;

    DiceType(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public List<Integer> getSampleSpace(int facets) {
        return switch (this) {
            case REGULAR, PLOT_DIE, KEPT_DIE, ENHANCED_DIE -> IntStream.rangeClosed(1, facets).boxed().toList();
            case CHAOS_DIE -> IntStream.rangeClosed(facets * -1, -1).boxed().toList();
            case ASSURED_DIE -> List.of(facets);
        };
    }
}
