package doom;

public final class DoomPool {
    private final long roleId;
    private int amount;

    DoomPool(String name, int amount, long roleId) {
        this.amount = amount;
        this.roleId = roleId;
    }

    public long getRoleId() {
        return roleId;
    }

    public int getAmount() {
        return amount;
    }

    public DoomPool setAmount(int amount) {
        this.amount = amount;
        return this;
    }
}
