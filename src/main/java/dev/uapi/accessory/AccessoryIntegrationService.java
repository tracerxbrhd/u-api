package dev.uapi.accessory;

import dev.uapi.UApi;
import dev.uapi.integration.IntegrationService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Loader-safe facade for optional accessory systems.
 *
 * <p>Providers can be registered by other mods. U-API includes a reflective Curios
 * provider so neither U-API nor its consumers need a hard Curios dependency.</p>
 */
public final class AccessoryIntegrationService {
    private static final CopyOnWriteArrayList<AccessoryProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static volatile boolean initialized;

    private AccessoryIntegrationService() {}

    /** Initializes built-in optional providers once mod discovery has completed. */
    public static void bootstrap() {
        initialize();
    }

    public static void registerProvider(AccessoryProvider provider) {
        if (provider == null) throw new IllegalArgumentException("Accessory provider cannot be null");
        if (PROVIDERS.stream().noneMatch(current -> current.id().equals(provider.id()))) PROVIDERS.add(provider);
    }

    public static boolean isLoaded() {
        initialize();
        return !PROVIDERS.isEmpty();
    }

    public static boolean supportsCharmSlot(Player player) {
        return supportsSlot(player, "charm");
    }

    public static boolean supportsSlot(Player player, String slotId) {
        initialize();
        return PROVIDERS.stream().anyMatch(provider -> safe(() -> provider.supportsSlot(player, slotId), false));
    }

    public static boolean canEquip(Player player, ItemStack stack) {
        return canEquip(player, stack, "charm");
    }

    public static boolean canEquip(Player player, ItemStack stack, String slotId) {
        initialize();
        return PROVIDERS.stream().anyMatch(provider -> safe(() -> provider.canEquip(player, stack, slotId), false));
    }

    public static boolean isItemEquipped(Player player, Predicate<ItemStack> predicate) {
        initialize();
        return PROVIDERS.stream().anyMatch(provider -> safe(() -> provider.isItemEquipped(player, predicate), false));
    }

    public static List<ItemStack> getEquippedAccessories(Player player) {
        initialize();
        List<ItemStack> result = new ArrayList<>();
        for (AccessoryProvider provider : PROVIDERS) {
            List<ItemStack> stacks = safe(() -> provider.getEquippedAccessories(player), List.of());
            stacks.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).forEach(result::add);
        }
        return List.copyOf(result);
    }

    public static List<String> activeProviderIds() {
        initialize();
        return PROVIDERS.stream().map(AccessoryProvider::id).toList();
    }

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        if (!IntegrationService.isLoaded("curios")) return;
        try {
            registerProvider(new ReflectiveCuriosProvider());
            UApi.LOGGER.info("Enabled optional Curios accessory provider");
        } catch (ReflectiveOperationException | LinkageError exception) {
            UApi.LOGGER.warn("Curios is installed but its API is not compatible with this U-API build; accessory support is disabled", exception);
        }
    }

    private static <T> T safe(CheckedSupplier<T> action, T fallback) {
        try {
            return action.get();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            UApi.LOGGER.debug("Optional accessory provider call failed", exception);
            return fallback;
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws ReflectiveOperationException;
    }

    private static final class ReflectiveCuriosProvider implements AccessoryProvider {
        private final Method inventoryGetter;

        private ReflectiveCuriosProvider() throws ReflectiveOperationException {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi", false,
                AccessoryIntegrationService.class.getClassLoader());
            inventoryGetter = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
        }

        @Override public String id() { return "curios"; }

        @Override
        public boolean supportsSlot(Player player, String slotId) {
            Object handler = handler(player).orElse(null);
            if (handler == null) return false;
            try {
                Object curios = handler.getClass().getMethod("getCurios").invoke(handler);
                return curios instanceof java.util.Map<?, ?> map && map.containsKey(slotId);
            } catch (NoSuchMethodException ignored) {
                return "charm".equals(slotId);
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }

        @Override
        public boolean canEquip(Player player, ItemStack stack, String slotId) {
            return supportsSlot(player, slotId);
        }

        @Override
        public boolean isItemEquipped(Player player, Predicate<ItemStack> predicate) {
            Object handler = handler(player).orElse(null);
            if (handler == null) return false;
            try {
                return (boolean) handler.getClass().getMethod("isEquipped", Predicate.class).invoke(handler, predicate);
            } catch (ReflectiveOperationException exception) {
                return getEquippedAccessories(player).stream().anyMatch(predicate);
            }
        }

        @Override
        public List<ItemStack> getEquippedAccessories(Player player) {
            Object handler = handler(player).orElse(null);
            if (handler == null) return List.of();
            try {
                Object itemHandler = handler.getClass().getMethod("getEquippedCurios").invoke(handler);
                Method slots = itemHandler.getClass().getMethod("getSlots");
                Method stackAt = itemHandler.getClass().getMethod("getStackInSlot", int.class);
                int count = (int) slots.invoke(itemHandler);
                List<ItemStack> result = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    Object value = stackAt.invoke(itemHandler, index);
                    if (value instanceof ItemStack stack && !stack.isEmpty()) result.add(stack.copy());
                }
                return result;
            } catch (ReflectiveOperationException exception) {
                return List.of();
            }
        }

        private Optional<?> handler(Player player) {
            try {
                Object result = inventoryGetter.invoke(null, player);
                return result instanceof Optional<?> optional ? optional : Optional.empty();
            } catch (ReflectiveOperationException exception) {
                return Optional.empty();
            }
        }
    }
}
