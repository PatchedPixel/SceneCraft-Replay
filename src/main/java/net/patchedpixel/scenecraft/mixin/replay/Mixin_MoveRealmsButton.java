package net.patchedpixel.scenecraft.mixin.replay;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.replay.Setting;
import net.patchedpixel.scenecraft.replay.handler.GuiHandler.MainMenuButtonPosition;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TitleScreen.class)
public abstract class Mixin_MoveRealmsButton {
    private static final String REALMS_INIT = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;init(Lnet/minecraft/client/Minecraft;II)V";

    @ModifyArg(
            method = "init",
            at = @At(value = "INVOKE", target = REALMS_INIT),
            index = 2
    )
    private int adjustRealmsButton(int height) {
        String setting = SceneCraft.instance.getSettingsRegistry().get(Setting.MAIN_MENU_BUTTON);
        if (MainMenuButtonPosition.valueOf(setting) == MainMenuButtonPosition.BIG) {
            height -= 24 * 4;
        }
        return height;
    }
}
