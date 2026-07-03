package dev.uapi.reward;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.uapi.UApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public final class RewardReloadListener implements ResourceManagerReloadListener {
    private static final String ROOT = "u_api/reward_pools";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<ResourceLocation, RewardPool> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> resource : manager.listResources(ROOT,
            id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = resource.getKey();
            String path = file.getPath().substring((ROOT + "/").length(), file.getPath().length() - 5);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path);
            try (Reader reader = resource.getValue().openAsReader()) {
                loaded.put(id, RewardPool.parse(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception exception) {
                UApi.LOGGER.error("Failed to load reward pool {}", file, exception);
            }
        }
        RewardRegistry.replacePools(loaded);
    }
}
