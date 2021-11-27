package calculation.outputs;

import calculation.models.Info;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;

/**
 * Output class for output a json new line delimited form for Google's Big Query. Dice end up being an array object in the Roll Object
 */
public class BigQueryOutput implements IOutput {
    private final static Logger LOGGER = LogManager.getLogger(BigQueryOutput.class);
    private final static Gson GSON = new GsonBuilder()
            .create();

    private final File file;
    private final BufferedWriter writer;

    public BigQueryOutput(File file, BufferedWriter writer) {
        this.file = file;
        this.writer = writer;
    }

    @Nullable
    @Override
    public Path getFilePath() {
        return file.toPath();
    }

    @Override
    public void close() throws Exception {
        writer.flush();
        writer.close();
    }

    @Override
    public void accept(Info info) {
        try {
            this.writer.write(GSON.toJson(info.toJson()));
            this.writer.newLine();
        } catch (IOException e) {
            LOGGER.warn("Failed to write to file", e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static Try<BigQueryOutput> create(String channel) {
        File file = new File("tmp/" + channel + "-" + Calendar.getInstance().getTime().getTime() + ".txt");
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
            return Try.success(new BigQueryOutput(file, new BufferedWriter(new FileWriter(file))));
        } catch (IOException e) {
            return Try.failure(e);
        }

    }
}
