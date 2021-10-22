package rolling;

public class RollParameters {
    private final String pool;
    private final int discount;
    private final boolean enhanceable;
    private final boolean opportunity;
    private final int diceKept;

    public RollParameters(String pool, int discount, boolean enhanceable, boolean opportunity, int diceKept) {
        this.pool = pool;
        this.discount = discount;
        this.enhanceable = enhanceable;
        this.opportunity = opportunity;
        this.diceKept = diceKept;
    }

    public String getPool() {
        return pool;
    }

    public int getDiscount() {
        return discount;
    }

    public boolean isEnhanceable() {
        return enhanceable;
    }

    public boolean isOpportunity() {
        return opportunity;
    }

    public int getDiceKept() {
        return diceKept;
    }
}
