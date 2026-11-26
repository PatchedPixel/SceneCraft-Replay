package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CheckboxWidget extends Checkbox {
    private static final int BOX_BACKGROUND = 0xFF2E2E2E;

    private final Consumer<Boolean> onChange;

    public CheckboxWidget(Component label, boolean selected, Consumer<Boolean> onChange) {
        super(0, 0, MCVer.getFontRenderer().lineHeight + 2 + 2 + MCVer.getFontRenderer().width(label),
                MCVer.getFontRenderer().lineHeight + 2, label, selected);
        this.onChange = onChange;
    }

    @Override
    public void onPress() {
        super.onPress();
        onChange.accept(selected());
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = MCVer.getFontRenderer();
        int color = active ? 0xFFE0E0E0 : 0xFFA0A0A0;
        int box = getHeight();
        int x = getX();
        int y = getY();

        graphics.fill(x, y, x + box, y + box, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + box - 1, y + box - 1, BOX_BACKGROUND);
        if(selected()) {
            graphics.drawCenteredString(font, "x", x + box / 2 + 1, y + 1, color);
        }
        graphics.drawString(font, getMessage(), x + box + 2, y + 2, color, false);
    }
}
