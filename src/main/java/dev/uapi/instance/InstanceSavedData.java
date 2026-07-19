package dev.uapi.instance;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class InstanceSavedData extends SavedData {
    static final int DATA_VERSION = 1;
    static final String DATA_VERSION_KEY = "dataVersion";
    static final Factory<InstanceSavedData> FACTORY = new Factory<>(InstanceSavedData::new, InstanceSavedData::load);
    private final Map<UUID, ManagedInstance> instances = new LinkedHashMap<>();

    Map<UUID, ManagedInstance> instances() {
        return instances;
    }

    void changed() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(DATA_VERSION_KEY, DATA_VERSION);
        ListTag list = new ListTag();
        instances.values().forEach(instance -> list.add(instance.save()));
        tag.put("instances", list);
        return tag;
    }

    static InstanceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        requireCurrentDataVersion(tag);
        InstanceSavedData data = new InstanceSavedData();
        ListTag list = tag.getList("instances", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            ManagedInstance instance = ManagedInstance.load((CompoundTag) entry);
            data.instances.put(instance.id(), instance);
        }
        return data;
    }

    static void requireCurrentDataVersion(CompoundTag tag) {
        if (!tag.contains(DATA_VERSION_KEY, Tag.TAG_INT)) {
            throw new IllegalStateException("U-API instance storage is missing required integer "
                + DATA_VERSION_KEY + "; cross-version loading is not supported");
        }
        int actualVersion = tag.getInt(DATA_VERSION_KEY);
        if (actualVersion != DATA_VERSION) {
            throw new IllegalStateException("U-API instance storage requires " + DATA_VERSION_KEY + "="
                + DATA_VERSION + " but found " + actualVersion
                + "; cross-version loading is not supported");
        }
    }
}
