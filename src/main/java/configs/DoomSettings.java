package configs;

import java.util.Map;

public class DoomSettings {
    private final Map<String, Integer> doomPools;
    private String activePool;

    public DoomSettings() {
        doomPools = Map.of("Doom!", 0);
        activePool = "Doom!";
    }

    public Map<String, Integer> getDoomPools() {
        return doomPools;
    }

    public String getActivePool() {
        return activePool;
    }

    public void setActivePool(String activePool) {
        this.activePool = activePool;
    }
}
