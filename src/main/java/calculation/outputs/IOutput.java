package calculation.outputs;

import calculation.models.Info;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface IOutput extends Consumer<Info>, AutoCloseable {
    @Nullable
    Path getFilePath();
}
