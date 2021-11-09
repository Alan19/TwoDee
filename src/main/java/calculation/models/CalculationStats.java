package calculation.models;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class CalculationStats {
    private final AtomicInteger errors;
    private final AtomicInteger skipped;
    private final AtomicInteger success;

    private File file;

    public CalculationStats() {
        this.errors = new AtomicInteger();
        this.skipped = new AtomicInteger();
        this.success = new AtomicInteger();
    }

    public CalculationStats(int errors, int skipped, int success) {
        this.errors = new AtomicInteger(errors);
        this.skipped = new AtomicInteger(skipped);
        this.success = new AtomicInteger(success);
    }

    public CalculationStats merge(CalculationStats other) {
        return new CalculationStats(
                this.errors.get() + other.errors.get(),
                this.skipped.get() + other.skipped.get(),
                this.success.get() + other.success.get()
        );
    }

    public void incrementError() {
        this.errors.incrementAndGet();
    }

    public void incrementSkipped() {
        this.skipped.incrementAndGet();
    }

    public void incrementSuccess() {
        this.success.incrementAndGet();
    }

    public int getSuccess() {
        return this.success.get();
    }

    public int getError() {
        return this.errors.get();
    }

    public int getSkipped() {
        return this.skipped.get();
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return this.file;
    }
}
