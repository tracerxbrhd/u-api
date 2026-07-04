package dev.uapi.accessory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/** Optional-mod adapter used by {@link AccessoryIntegrationService}. */
public interface AccessoryProvider {
    String id();

    boolean supportsSlot(Player player, String slotId);

    boolean canEquip(Player player, ItemStack stack, String slotId);

    boolean isItemEquipped(Player player, Predicate<ItemStack> predicate);

    List<ItemStack> getEquippedAccessories(Player player);
}
