package calculation.outputs;

import io.vavr.control.Try;

import java.util.Optional;

public enum OutputType {
    SQLITE {
        @Override
        public Try<SQLOutput> setup(String channelName) {
            return SQLOutput.create(channelName);
        }
    },
    BIG_QUERY {
        @Override
        public Try<? extends IOutput> setup(String channelName) {
            return null;
        }
    };

    public abstract Try<? extends IOutput> setup(String channelName);

    public static Optional<OutputType> getByName(String name) {
        for (OutputType outputType : OutputType.values()) {
            if (outputType.name().equalsIgnoreCase(name)) {
                return Optional.of(outputType);
            }
        }
        return Optional.empty();
    }
}
