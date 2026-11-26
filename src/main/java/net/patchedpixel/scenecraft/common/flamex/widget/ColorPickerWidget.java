package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.util.color.Color;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ColorPickerWidget extends AbstractWidget {
    private static final int PICKER_SIZE = 100;

    private final Consumer<Color> onSelection;
    private Color color;
    private boolean opened;
    private boolean dragging;

    public ColorPickerWidget(int width, int height, Color initial, Consumer<Color> onSelection) {
        super(0, 0, width, height, Component.empty());
        this.color = initial;
        this.onSelection = onSelection;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    private static Color colorAt(int x, int y) {
        if(x < 5) {
            int intensity = 255 - y * 255 / PICKER_SIZE;
            return new Color(intensity, intensity, intensity);
        }
        float hue = (x - 5f) / (PICKER_SIZE - 5f);
        float saturation = Math.min(y / (PICKER_SIZE / 2f), 1);
        float brightness = Math.min(2 - y / (PICKER_SIZE / 2f), 1);
        return fromHsb(hue, saturation, brightness);
    }

    private static Color fromHsb(float hue, float saturation, float brightness) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1f - saturation);
        float q = brightness * (1f - saturation * f);
        float t = brightness * (1f - saturation * (1f - f));
        float r, g, b;
        switch((int) h) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }
        return new Color((int) (r * 255f + 0.5f), (int) (g * 255f + 0.5f), (int) (b * 255f + 0.5f));
    }

    private boolean overPicker(double x, double y) {
        return opened && x >= getX() && x < getX() + PICKER_SIZE
                && y >= getY() + getHeight() && y < getY() + getHeight() + PICKER_SIZE;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || overPicker(mouseX, mouseY);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, color.toARGB());

        if(opened) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            int top = y + getHeight();
            for(int px = 0; px < PICKER_SIZE; px++) {
                for(int py = 0; py < PICKER_SIZE; py++) {
                    graphics.fill(x + px, top + py, x + px + 1, top + py + 1, colorAt(px, py).toARGB());
                }
            }
            graphics.pose().popPose();
        }
    }

    private void pick(double mouseX, double mouseY) {
        int px = (int) mouseX - getX();
        int py = (int) mouseY - (getY() + getHeight());
        if(px < 0 || py < 0 || px >= PICKER_SIZE || py >= PICKER_SIZE) return;
        color = colorAt(px, py);
        onSelection.accept(color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!active || !visible || button != 0) return false;
        if(overPicker(mouseX, mouseY)) {
            dragging = true;
            pick(mouseX, mouseY);
            return true;
        }
        if(super.isMouseOver(mouseX, mouseY)) {
            opened = !opened;
            return true;
        }
        opened = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if(!dragging) return false;
        pick(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if(!focused) opened = false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
