package dev.uapi.api.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class ProtocolAndEnvelopeTest {
    @Test
    void negotiatesTheLowerMinorWhenMajorsMatch() {
        assertEquals(new ProtocolVersion(2, 0), UApiNetworkProtocol.CURRENT);
        ProtocolVersion local = new ProtocolVersion(2, 4);
        ProtocolVersion olderRemote = new ProtocolVersion(2, 1);
        ProtocolVersion newerRemote = new ProtocolVersion(2, 8);

        assertTrue(local.isCompatibleWith(olderRemote));
        assertTrue(local.isCompatibleWith(newerRemote));
        assertEquals(new ProtocolVersion(2, 1), local.negotiate(olderRemote).orElseThrow());
        assertEquals(new ProtocolVersion(2, 4), local.negotiate(newerRemote).orElseThrow());
        assertTrue(local.supportsMinor(4));
        assertFalse(local.supportsMinor(5));
    }

    @Test
    void rejectsMajorMismatchAndParsesStableTextForm() {
        ProtocolVersion local = ProtocolVersion.parse("2.3");

        assertEquals("2.3", local.toString());
        assertFalse(local.isCompatibleWith(new ProtocolVersion(3, 0)));
        assertTrue(local.negotiate(new ProtocolVersion(3, 0)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ProtocolVersion.parse("2"));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolVersion(-1, 0));
    }

    @Test
    void ackAndErrorContainOnlyCorrelationAndStableCodes() {
        RequestId requestId = new RequestId(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        OperationAck ack = new OperationAck(requestId, id("accepted"));
        OperationError error = new OperationError(requestId, id("not_allowed"));

        assertEquals(requestId, ack.requestId());
        assertEquals(id("accepted"), ack.code());
        assertEquals(requestId, error.requestId());
        assertEquals(id("not_allowed"), error.code());
        assertEquals(requestId, RequestId.parse(requestId.toString()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
