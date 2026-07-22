package dev.uapi.api.permissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class PermissionContractsTest {
    @Test
    void validatesPermissionKeys() {
        PermissionKey key = PermissionKey.parse("example:territory.block_break");

        assertEquals("example:territory.block_break", key.toString());
        assertThrows(IllegalArgumentException.class, () -> PermissionKey.parse("Not a resource location"));
        assertThrows(NullPointerException.class, () -> new PermissionKey(null));
    }

    @Test
    void representsAllowAndDenyWithStableReasonKeys() {
        Identifier allowedReason = id("role_allows");
        Identifier deniedReason = id("missing_permission");

        PermissionDecision allowed = PermissionDecision.allowed(allowedReason);
        PermissionDecision denied = PermissionDecision.denied(deniedReason);

        assertTrue(allowed.allowed());
        assertEquals(allowedReason, allowed.reasonKey());
        assertFalse(denied.allowed());
        assertEquals(deniedReason, denied.reasonKey());
    }

    @Test
    void contextIdentityIncludesActorTargetActionAndResource() {
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        PermissionResource resource = new PermissionResource(id("claim"), "overworld/4/9");
        PermissionContext first = new PermissionContext(
            actor, Optional.of(target), id("block_break"), Optional.of(resource)
        );
        PermissionContext equal = PermissionContext.action(actor, id("block_break"))
            .withTarget(target)
            .withResource(resource);

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, equal.withTarget(UUID.randomUUID()));
        assertNotEquals(first, PermissionContext.action(actor, id("block_place"))
            .withTarget(target).withResource(resource));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("permission_test", path);
    }
}
