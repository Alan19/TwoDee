package doom;

import java.util.concurrent.CompletableFuture;

public record RemoteDoom(
        long id,
        String name,
        int doom
) implements Doom {
    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public CompletableFuture<Integer> getDoom() {
        //Todo Implement
        return CompletableFuture.completedFuture(0);
    }

    @Override
    public CompletableFuture<DoomChange> changeDoom(int amount) {
        //Todo Implement
        return CompletableFuture.completedFuture(new DoomChange(0, amount));
    }

    @Override
    public CompletableFuture<Boolean> delete() {
        //Todo Implement
        return CompletableFuture.completedFuture(false);
    }
}
