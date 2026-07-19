package dev.uapi.client.ui.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Small observable state holder for client view models.
 *
 * <p>Updates can be batched, and every subscription is explicitly disposable so a screen can
 * release its listeners when it closes.</p>
 */
public final class UIState<T> {
    private final Map<Long, Consumer<? super T>> listeners = new LinkedHashMap<>();
    private T value;
    private long nextListenerId;
    private long revision;
    private int batchDepth;
    private boolean pendingNotification;

    public UIState(T initialValue) {
        value = Objects.requireNonNull(initialValue, "initialValue");
    }

    public synchronized T get() {
        return value;
    }

    public synchronized long revision() {
        return revision;
    }

    public void set(T nextValue) {
        List<Consumer<? super T>> toNotify;
        synchronized (this) {
            nextValue = Objects.requireNonNull(nextValue, "nextValue");
            if (Objects.equals(value, nextValue)) return;
            value = nextValue;
            revision++;
            if (batchDepth > 0) {
                pendingNotification = true;
                return;
            }
            toNotify = List.copyOf(listeners.values());
        }
        notifyListeners(toNotify, nextValue);
    }

    public void update(UnaryOperator<T> updater) {
        Objects.requireNonNull(updater, "updater");
        List<Consumer<? super T>> toNotify;
        T nextValue;
        synchronized (this) {
            nextValue = Objects.requireNonNull(updater.apply(value), "updater result");
            if (Objects.equals(value, nextValue)) return;
            value = nextValue;
            revision++;
            if (batchDepth > 0) {
                pendingNotification = true;
                return;
            }
            toNotify = List.copyOf(listeners.values());
        }
        notifyListeners(toNotify, nextValue);
    }

    public void batch(Runnable updates) {
        Objects.requireNonNull(updates, "updates");
        synchronized (this) {
            batchDepth++;
        }
        Throwable failure = null;
        try {
            updates.run();
        } catch (RuntimeException | Error exception) {
            failure = exception;
        }
        try {
            endBatch();
        } catch (RuntimeException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }

    public Subscription subscribe(Consumer<? super T> listener) {
        return subscribe(listener, true);
    }

    public Subscription subscribe(Consumer<? super T> listener, boolean emitCurrentValue) {
        Objects.requireNonNull(listener, "listener");
        long id;
        T current;
        synchronized (this) {
            id = nextListenerId++;
            listeners.put(id, listener);
            current = value;
        }
        if (emitCurrentValue) {
            try {
                listener.accept(current);
            } catch (RuntimeException exception) {
                unsubscribe(id);
                throw exception;
            }
        }
        return new Subscription(this, id);
    }

    public synchronized int listenerCount() {
        return listeners.size();
    }

    private void endBatch() {
        List<Consumer<? super T>> toNotify = List.of();
        T current = null;
        synchronized (this) {
            if (batchDepth <= 0) throw new IllegalStateException("UIState batch underflow");
            batchDepth--;
            if (batchDepth == 0 && pendingNotification) {
                pendingNotification = false;
                toNotify = new ArrayList<>(listeners.values());
                current = value;
            }
        }
        if (!toNotify.isEmpty()) notifyListeners(toNotify, current);
    }

    private static <T> void notifyListeners(List<Consumer<? super T>> listeners, T value) {
        RuntimeException failure = null;
        for (Consumer<? super T> listener : listeners) {
            try {
                listener.accept(value);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    private synchronized void unsubscribe(long id) {
        listeners.remove(id);
    }

    public static final class Subscription implements AutoCloseable {
        private UIState<?> owner;
        private final long id;

        private Subscription(UIState<?> owner, long id) {
            this.owner = owner;
            this.id = id;
        }

        @Override
        public synchronized void close() {
            if (owner == null) return;
            owner.unsubscribe(id);
            owner = null;
        }
    }
}
