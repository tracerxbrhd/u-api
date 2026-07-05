package dev.uapi;

import com.mojang.logging.LogUtils;
import dev.uapi.command.UApiCommandRegistry;
import dev.uapi.accessory.AccessoryIntegrationService;
import dev.uapi.command.UApiCommands;
import dev.uapi.config.UApiClientConfig;
import dev.uapi.config.UApiCommonConfig;
import dev.uapi.config.UApiCommonConfigManager;
import dev.uapi.config.UApiServerConfig;
import dev.uapi.reward.RewardRegistry;
import dev.uapi.server.UApiServerEvents;
import dev.uapi.creative.UApiCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(UApi.MOD_ID)
public final class UApi {
    public static final String MOD_ID = "u_api";
    public static final String MOD_NAME = "U-API";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UApi(IEventBus modBus, ModContainer container) {
        UApiCommandRegistry.registerSection("api", UApiCommands::create);
        UApiCreativeTabs.registerBus(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onConfigLoading);
        modBus.addListener(this::onConfigReloading);
        container.registerConfig(ModConfig.Type.COMMON, UApiServerConfig.SPEC, "uapi/u-api/server.toml");
        container.registerConfig(ModConfig.Type.COMMON, UApiCommonConfig.SPEC, "uapi/u-api/common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, UApiClientConfig.SPEC, "uapi/u-api/client.toml");
        RewardRegistry.bootstrap();
        NeoForge.EVENT_BUS.register(UApiServerEvents.class);
        NeoForge.EVENT_BUS.register(UApiCommandRegistry.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AccessoryIntegrationService::bootstrap);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == UApiCommonConfig.SPEC)
            UApiCommonConfigManager.reloadFromSpec();
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == UApiCommonConfig.SPEC)
            UApiCommonConfigManager.reloadFromSpec();
    }
}
