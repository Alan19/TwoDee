package calculation.outputs;

import calculation.models.Info;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Interface that is the Combination of consuming the Info classes, inserting cleanup, and getting any files created
 */
public interface IOutput extends Consumer<Info>, AutoCloseable {
    @Nullable
    Path getFilePath();
}
