package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.util.function.IntFunction;

public class StepSlider extends AbstractSliderButton {
    private final int steps;
    private int value;
    private IntFunction<Component> text = v -> MCVer.literalText(Integer.toString(v));
    private Runnable onValueChanged = () -> {};

    public StepSlider(int width, int height, int steps) {
        super(0, 0, width, height, Component.empty(), 0);
        this.steps = steps;

        updateMessage();
    }

    public StepSlider setText(IntFunction<Component> text) {
        this.text = text;
        updateMessage();

        return this;
    }

    public StepSlider onValueChanged(Runnable r) {
        this.onValueChanged = r;
        return this;
    }

    public int getValue() {
        return value;
    }

    public StepSlider setValue(int value) {
        this.value = Math.max(0, Math.min(steps - 1, value));
        super.value = steps <= 1 ? 0 : (double) this.value / (steps - 1);
        updateMessage();
        onValueChanged.run();

        return this;
    }

    @Override
    protected void updateMessage() {
        if(text != null) {
            setMessage(text.apply(value));
        }
    }

    @Override
    protected void applyValue() {
        int v = steps <= 1 ? 0 : (int) Math.round(super.value * (steps - 1));
        if(v != value) {
            value = v;
            updateMessage();
            onValueChanged.run();
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int width = getWidth();
        int height = getHeight();
        int half = width / 2;
        graphics.blit(AbstractWidget.WIDGETS_LOCATION, getX(), getY(), 0, 46, half, height);
        graphics.blit(AbstractWidget.WIDGETS_LOCATION, getX() + half, getY(), 200 - half, 46, half, height);

        int sliderX = (int) ((width - 8) * super.value);
        graphics.blit(AbstractWidget.WIDGETS_LOCATION, getX() + sliderX, getY(), 0, 66, 4, 20);
        graphics.blit(AbstractWidget.WIDGETS_LOCATION, getX() + sliderX + 4, getY(), 196, 66, 4, 20);

        int color = !active ? 0xA0A0A0 : isHoveredOrFocused() ? 0xFFFFA0 : 0xE0E0E0;
        var font = MCVer.getFontRenderer();
        graphics.drawString(font, getMessage(), getX() + half - font.width(getMessage()) / 2, getY() + height / 2 - 4, color | 0xFF000000, false);
    }
}
