package dev.uapi.instance;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class InstanceSavedData extends SavedData {
    static final int DATA_VERSION = 1;
    static final String DATA_VERSION_KEY = "dataVersion";
    static final Codec<InstanceSavedData> CODEC = CompoundTag.CODEC.xmap(InstanceSavedData::load,
        InstanceSavedData::save);
    static final SavedDataType<InstanceSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("u_api", "instances"), InstanceSavedData::new, CODEC);
    private final Map<UUID, ManagedInstance> instances = new LinkedHashMap<>();

    Map<UUID, ManagedInstance> instances() {
        return instances;
    }

    void changed() {
        setDirty();
    }

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(DATA_VERSION_KEY, DATA_VERSION);
        ListTag list = new ListTag();
        instances.values().forEach(instance -> list.add(instance.save()));
        tag.put("instances", list);
        return tag;
    }

    static InstanceSavedData load(CompoundTag tag) {
        requireCurrentDataVersion(tag);
        InstanceSavedData data = new InstanceSavedData();
        ListTag list = tag.getListOrEmpty("instances");
        for (Tag entry : list) {
            ManagedInstance instance = ManagedInstance.load((CompoundTag) entry);
            data.instances.put(instance.id(), instance);
        }
        return data;
    }

    static void requireCurrentDataVersion(CompoundTag tag) {
        if (!tag.contains(DATA_VERSION_KEY)) {
            throw new IllegalStateException("U-API instance storage is missing required integer "
                + DATA_VERSION_KEY + "; cross-version loading is not supported");
        }
        int actualVersion = tag.getIntOr(DATA_VERSION_KEY, Integer.MIN_VALUE);
        if (actualVersion != DATA_VERSION) {
            throw new IllegalStateException("U-API instance storage requires " + DATA_VERSION_KEY + "="
                + DATA_VERSION + " but found " + actualVersion
                + "; cross-version loading is not supported");
        }
    }
}
