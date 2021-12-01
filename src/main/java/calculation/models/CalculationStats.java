package calculation.models;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Class that collects the results of all messages processed, simplifying the Output message. All but File are threadsafe.
 */
public class CalculationStats {
    private final AtomicInteger errors;
    private final AtomicInteger skipped;
    private final AtomicInteger success;

    private final Map<String, AtomicInteger> errorAmounts;

    private File file;

    public CalculationStats() {
        this.errors = new AtomicInteger();
        this.skipped = new AtomicInteger();
        this.success = new AtomicInteger();
        this.errorAmounts = new HashMap<>();
    }

    public CalculationStats(int errors, int skipped, int success, Map<String, AtomicInteger> errorAmounts) {
        this.errors = new AtomicInteger(errors);
        this.skipped = new AtomicInteger(skipped);
        this.success = new AtomicInteger(success);
        this.errorAmounts = errorAmounts;
    }

    public CalculationStats merge(CalculationStats other) {
        return new CalculationStats(
                this.errors.get() + other.errors.get(),
                this.skipped.get() + other.skipped.get(),
                this.success.get() + other.success.get(),
                Stream.concat(this.errorAmounts.entrySet().stream(), other.errorAmounts.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (value1, value2) -> new AtomicInteger(value1.intValue() + value2.intValue())
                        ))
        );
    }

    public int incrementError(String message) {
        this.errorAmounts.computeIfAbsent(message, value -> new AtomicInteger()).incrementAndGet();
        return this.errors.incrementAndGet();
    }

    public void incrementSkipped() {
        this.skipped.incrementAndGet();
    }

    public int incrementSuccess() {
        return this.success.incrementAndGet();
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

    public String getTopError() {
        return this.errorAmounts.entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue().intValue()))
                .reduce(Pair.of("", 0), (pair1, pair2) -> {
                    if (pair1.getRight() > pair2.getRight()) {
                        return pair1;
                    }
                    else {
                        return pair2;
                    }
                })
                .getKey();
    }
}
