package calculation;

import calculation.models.CalculationStats;
import calculation.models.Info;
import calculation.outputs.IOutput;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * Collector for adding the RollInfo into an Output/Maintaining Status/Counts
 */
public class CalculationCollector implements Collector<Try<? extends Info>, CalculationStats, Try<CalculationStats>> {
    private final IOutput consumer;
    private final IntConsumer updateHandler;

    public CalculationCollector(IOutput consumer, IntConsumer updateHandler) {
        this.consumer = consumer;
        this.updateHandler = updateHandler;
    }

    @Override
    public Supplier<CalculationStats> supplier() {
        return CalculationStats::new;
    }

    @Override
    public BiConsumer<CalculationStats, Try<? extends Info>> accumulator() {
        return (stats, info) -> {
            info.onFailure(throwable -> {
                int errors = stats.incrementError(throwable.getMessage());
                if ((errors + stats.getSuccess()) % 100 == 0) {
                    updateHandler.accept(stats.getSuccess() + errors);
                }
            });
            info.onSuccess(value -> {
                if (value == null) {
                    stats.incrementSkipped();
                }
                else {
                    consumer.accept(value);
                    int successes = stats.incrementSuccess();
                    if ((successes + stats.getError()) % 100 == 0) {
                        updateHandler.accept(successes + stats.getError());
                    }
                }
            });
        };
    }

    @Override
    public BinaryOperator<CalculationStats> combiner() {
        return CalculationStats::merge;
    }

    @Override
    public Function<CalculationStats, Try<CalculationStats>> finisher() {
        return stats -> Try.of(() -> {
            consumer.close();
            if (consumer.getFilePath() != null) {
                stats.setFile(consumer.getFilePath().toFile());
            }
            return stats;
        });
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.UNORDERED);
    }
}
