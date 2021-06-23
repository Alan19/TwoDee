package util;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Optional;

public class OptionalHelper {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T, U> Optional<Pair<T, U>> tupled(Optional<T> t, Optional<U> u) {
        if (t.isPresent() && u.isPresent()) {
            return Optional.of(Pair.of(t.get(), u.get()));
        }
        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T, U, V> Optional<Triple<T, U, V>> tuple3(Optional<T> t, Optional<U> u, Optional<V> v) {
        if (t.isPresent() && u.isPresent() && v.isPresent()) {
            return Optional.of(Triple.of(t.get(), u.get(), v.get()));
        }
        return Optional.empty();
    }
}
