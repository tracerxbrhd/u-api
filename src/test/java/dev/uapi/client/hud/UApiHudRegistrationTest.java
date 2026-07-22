package dev.uapi.client.hud;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UApiHudRegistrationTest {
    @Test
    void registrationDoesNotSampleRuntimeDimensions() {
        AtomicInteger samples = new AtomicInteger();
        HudElement element = new HudElement() {
            @Override public Identifier id() {
                return Identifier.fromNamespaceAndPath("u_api", "deferred_dimension_test");
            }

            @Override public int width() {
                samples.incrementAndGet();
                throw new IllegalStateException("runtime dimensions are unavailable during registration");
            }

            @Override public int height() {
                samples.incrementAndGet();
                throw new IllegalStateException("runtime dimensions are unavailable during registration");
            }

            @Override public void render(HudRenderContext context) {
            }
        };

        try (HudElementRegistration registration = UApiHud.register(element)) {
            assertTrue(registration.isActive());
            assertEquals(0, samples.get());
        }
    }
}
