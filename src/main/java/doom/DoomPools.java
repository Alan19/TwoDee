package doom;

import java.util.ArrayList;
import java.util.List;

public class DoomPools {
    private final String activePool;
    private List<DoomPool> pools;

    public DoomPools() {
        this.activePool = "";
        this.pools = new ArrayList<>();
    }

    public String getActivePool() {
        return activePool;
    }

    public List<DoomPool> getPools() {
        return pools;
    }
}
