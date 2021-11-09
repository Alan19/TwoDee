package calculation;

import calculation.models.CalculationStats;
import calculation.models.Info;
import calculation.outputs.IOutput;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class CalculationCollector implements Collector<Try<? extends Info>, CalculationStats, Try<CalculationStats>> {
    private final IOutput consumer;

    public CalculationCollector(IOutput consumer) {
        this.consumer = consumer;
    }

    @Override
    public Supplier<CalculationStats> supplier() {
        return CalculationStats::new;
    }

    @Override
    public BiConsumer<CalculationStats, Try<? extends Info>> accumulator() {
        return (stats, info) -> {
            info.onFailure(throwable -> stats.incrementError());
            info.onSuccess(value -> {
                if (value == null) {
                    stats.incrementSkipped();
                }
                else {
                    consumer.accept(value);
                    stats.incrementSuccess();
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
