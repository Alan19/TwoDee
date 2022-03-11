package doom;

import javax.annotation.Nullable;

public final class DoomPool {
    private final Long roleId;
    private int amount;

    DoomPool(int amount, @Nullable Long roleId) {
        this.amount = amount;
        this.roleId = roleId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public int getAmount() {
        return amount;
    }

    public int setAndGetAmount(int amount) {
        this.amount = amount;
        return amount;
    }
}
