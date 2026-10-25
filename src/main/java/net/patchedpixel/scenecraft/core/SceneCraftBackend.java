package net.patchedpixel.scenecraft.core;

import net.patchedpixel.scenecraft.core.gui.GuiReplaySettings;
import net.patchedpixel.scenecraft.gui.versions.GuiEventBridge;
import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(SceneCraft.MOD_ID)
public class SceneCraftBackend {
    private final String version;
    private SceneCraft mod;

    public SceneCraftBackend() {
        this.version = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();

        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remoteVersion, isFromServer) -> true)
        );

        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(new GuiEventBridge());

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> {
                    if (this.mod == null) {
                        return parent;
                    }
                    return new GuiReplaySettings(parent, this.mod.getSettingsRegistry()).toMinecraft();
                })
        );
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        this.mod = new SceneCraft(this);
        this.mod.registerModuleKeyBindings();
        this.mod.getKeyBindingRegistry().onRegisterKeyMappings(event);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (this.mod != null) {
                this.mod.initModules();
            }
        });
    }

    public String getVersion() {
        return version;
    }

    public String getMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }
}
