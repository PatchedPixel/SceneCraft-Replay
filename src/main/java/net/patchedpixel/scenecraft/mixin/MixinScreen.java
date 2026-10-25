package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.gui.versions.ScreenExt;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Screen.class, priority = 1100)
public abstract class MixinScreen implements ScreenExt {
    @Unique
    private boolean passEvents;

    @Override
    public boolean doesPassEvents() {
        return this.passEvents;
    }

    @Override
    public void setPassEvents(boolean passEvents) {
        this.passEvents = passEvents;
    }
}
