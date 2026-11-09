package util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CachedValue<T> {
    private final long ttlMillis;
    private final Supplier<T> refresh;

    private final AtomicLong expiryTime;
    private final AtomicReference<T> value;

    public CachedValue(long ttlMillis, Supplier<T> refresh) {
        this.ttlMillis = ttlMillis;
        this.refresh = refresh;
        this.value = new AtomicReference<>();
        this.expiryTime = new AtomicLong();
    }

    public T get() {
        if (expiryTime.get() < System.currentTimeMillis()) {
            this.value.set(null);
        }

        T currentValue = this.value.get();
        if (currentValue == null) {
            currentValue = refresh.get();
            this.value.set(currentValue);
            this.expiryTime.set(System.currentTimeMillis() + ttlMillis);
        }
        return currentValue;
    }

    public void initialize() {
        this.get();
    }

    public void forceRefresh() {
        this.value.set(null);
    }
}
