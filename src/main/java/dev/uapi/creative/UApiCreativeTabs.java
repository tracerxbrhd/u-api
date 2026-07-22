package dev.uapi.creative;

import dev.uapi.UApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Factory for isolated, ordered creative tabs owned by each ecosystem mod. */
public final class UApiCreativeTabs {
    private static final Registrar API = create(UApi.MOD_ID, "main", "itemGroup.u_api",
        () -> new ItemStack(Items.ENDER_EYE));

    private UApiCreativeTabs() {}

    public static Registrar create(String modId, String tabName, String titleKey, Supplier<ItemStack> icon) {
        return new Registrar(modId, tabName, titleKey, icon);
    }

    public static void registerBus(IEventBus modBus) {
        API.registerBus(modBus);
    }

    public static final class Registrar {
        private record Entry(Identifier itemId, int order, BooleanSupplier visible) {}

        private final String modId;
        private final DeferredRegister<CreativeModeTab> tabs;
        private final Map<Identifier, Entry> entries = new ConcurrentHashMap<>();
        private final DeferredHolder<CreativeModeTab, CreativeModeTab> tab;
        private boolean registered;

        private Registrar(String modId, String tabName, String titleKey, Supplier<ItemStack> icon) {
            this.modId = modId;
            tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modId);
            tab = tabs.register(tabName, () -> CreativeModeTab.builder()
                .title(Component.translatable(titleKey))
                .icon(icon)
                .displayItems((parameters, output) -> entries.values().stream()
                    .filter(entry -> entry.visible().getAsBoolean())
                    .sorted(Comparator.comparingInt(Entry::order)
                        .thenComparing(entry -> entry.itemId().toString()))
                    .forEach(entry -> {
                        Item item = BuiltInRegistries.ITEM.getValue(entry.itemId());
                        // ItemLike acceptance constructs a fresh, component-less ItemStack. Use the
                        // item's canonical default stack so custom potion contents and other default
                        // data components survive creative-tab population.
                        if (item != Items.AIR) output.accept(item.getDefaultInstance());
                    }))
                .build());
        }

        public void registerBus(IEventBus modBus) {
            if (registered) throw new IllegalStateException("Creative tab already registered for " + modId);
            registered = true;
            tabs.register(modBus);
        }

        public DeferredHolder<CreativeModeTab, CreativeModeTab> tab() { return tab; }

        public void add(Identifier itemId, int order) {
            add(itemId, order, () -> true);
        }

        public void add(Identifier itemId, int order, BooleanSupplier visible) {
            if (!itemId.getNamespace().equals(modId))
                throw new IllegalArgumentException("Creative tab " + modId + " cannot contain " + itemId);
            Entry previous = entries.putIfAbsent(itemId, new Entry(itemId, order, visible));
            if (previous != null) throw new IllegalStateException("Creative tab item already registered: " + itemId);
        }
    }
}
