package doom;

public record DoomPool(String name, int amount, long roleId) {

    public long getRoleId() {
        return roleId;
    }

    public int getAmount() {
        return amount;
    }

    public String getName() {
        return name;
    }
}
