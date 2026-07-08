package doom;

import configs.Settings;

import java.util.concurrent.CompletableFuture;

public record ConfigDoom(
        String name,
        int doom
) implements Doom {

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public CompletableFuture<Integer> getDoom() {
        return CompletableFuture.completedFuture(this.doom());
    }

    @Override
    public CompletableFuture<DoomChange> changeDoom(int amount) {
        int old = this.doom();
        int current = this.doom() + amount;
        DoomHandler.getInstance().doomSettings.getDoomPools()
                .put(this.name(), current);
        return CompletableFuture.completedFuture(new DoomChange(old, current));
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        Integer removed = DoomHandler.getInstance()
                .doomSettings
                .getDoomPools()
                .remove(this.getName());
        if (removed == null) {
            return CompletableFuture.completedFuture(false);
        } else {
            Settings.serializePersonalSettings();
            return CompletableFuture.completedFuture(true);
        }
    }
}
