package net.patchedpixel.scenecraft.platform;

//import net.patchedpixel.scenecraft.ScbinMappingReloadListener;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.gui.GuiReplaySettings;
import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
//import net.minecraft.resources.Identifier;
import net.minecraft.SharedConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
//import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(value = SceneCraft.MOD_ID, dist = Dist.CLIENT)
public class SceneCraftPlatform {
    private final String version;
    private SceneCraft mod;

    public SceneCraftPlatform(IEventBus modBus, ModContainer container) {
        this.version = container.getModInfo().getVersion().toString();

        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            this.mod = new SceneCraft(this);
            this.mod.registerModuleKeyBindings();
            this.mod.getKeyBindingRegistry().onRegisterKeyMappings(event);
        });
        modBus.addListener(RegisterPayloadHandlersEvent.class, SceneCraftRecorder::registerPayloads);
//        modBus.addListener(AddClientReloadListenersEvent.class, event ->
//                event.addListener(
//                        Identifier.fromNamespaceAndPath(SceneCraft.MOD_ID, "scbin_mapping_generator"),
//                        new ScbinMappingReloadListener()));
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> {
            if (this.mod == null) {
                return parent;
            }
            return new GuiReplaySettings(parent, this.mod.getSettingsRegistry()).toMinecraft();
        });
        modBus.addListener(FMLClientSetupEvent.class, _ -> {
            if (this.mod != null) this.mod.initModules();
        });
    }

    public String getVersion() {
        return version;
    }

    public String getMinecraftVersion() {
        return SharedConstants.getCurrentVersion().name();
    }
}
