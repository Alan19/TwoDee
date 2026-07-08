package doom;

import java.util.concurrent.CompletableFuture;

public interface Doom {
    String getName();

    CompletableFuture<Integer> getDoom();

    CompletableFuture<DoomChange> changeDoom(int amount);

    CompletableFuture<Boolean> delete();
}
