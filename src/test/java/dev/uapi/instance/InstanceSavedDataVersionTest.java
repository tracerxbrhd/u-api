package dev.uapi.instance;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class InstanceSavedDataVersionTest {
    @Test
    void acceptsOnlyTheExactCurrentVersion() {
        CompoundTag current = new CompoundTag();
        current.putInt(InstanceSavedData.DATA_VERSION_KEY, InstanceSavedData.DATA_VERSION);
        assertDoesNotThrow(() -> InstanceSavedData.requireCurrentDataVersion(current));

        assertThrows(IllegalStateException.class,
            () -> InstanceSavedData.requireCurrentDataVersion(new CompoundTag()));

        CompoundTag older = new CompoundTag();
        older.putInt(InstanceSavedData.DATA_VERSION_KEY, InstanceSavedData.DATA_VERSION - 1);
        assertThrows(IllegalStateException.class,
            () -> InstanceSavedData.requireCurrentDataVersion(older));

        CompoundTag newer = new CompoundTag();
        newer.putInt(InstanceSavedData.DATA_VERSION_KEY, InstanceSavedData.DATA_VERSION + 1);
        assertThrows(IllegalStateException.class,
            () -> InstanceSavedData.requireCurrentDataVersion(newer));
    }
}
