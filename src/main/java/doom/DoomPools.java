package doom;

import java.util.HashMap;
import java.util.Map;

public class DoomPools {
    private final Map<String, DoomPool> pools;
    private String activePool;

    public DoomPools() {
        this.activePool = "Doom!";
        this.pools = new HashMap<>();
    }

    public Map<String, DoomPool> getDoomPools() {
        return pools;
    }

    public String getActivePool() {
        return activePool;
    }

    public void setActivePool(String activePool) {
        this.activePool = activePool;
    }

}
