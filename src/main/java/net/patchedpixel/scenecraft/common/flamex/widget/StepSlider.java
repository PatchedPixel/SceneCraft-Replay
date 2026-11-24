package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
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
}
