package dev.uapi.client.ui.state;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UIStateTest {
    @Test
    void batchesMultipleChangesIntoOneNotification() {
        UIState<Integer> state = new UIState<>(0);
        List<Integer> received = new ArrayList<>();
        state.subscribe(received::add, false);

        state.batch(() -> {
            state.set(1);
            state.set(2);
            state.set(3);
        });

        assertEquals(List.of(3), received);
        assertEquals(3, state.revision());
    }

    @Test
    void closedSubscriptionStopsNotifications() {
        UIState<String> state = new UIState<>("initial");
        List<String> received = new ArrayList<>();
        UIState.Subscription subscription = state.subscribe(received::add);

        subscription.close();
        subscription.close();
        state.set("updated");

        assertEquals(List.of("initial"), received);
        assertEquals(0, state.listenerCount());
    }

    @Test
    void failedInitialEmissionDoesNotLeakTheListener() {
        UIState<Integer> state = new UIState<>(1);

        assertThrows(IllegalStateException.class,
            () -> state.subscribe(value -> { throw new IllegalStateException("broken listener"); }));

        assertEquals(0, state.listenerCount());
    }
}
