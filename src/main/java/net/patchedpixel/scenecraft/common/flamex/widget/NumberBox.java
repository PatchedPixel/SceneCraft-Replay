package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberBox extends EditBox {
    private final int precision;
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private boolean clickedInside;

    public NumberBox(int width, int precision) {
        super(MCVer.getFontRenderer(), 0, 0, width, 20, Component.empty());
        this.precision = precision;

        setMaxLength(64);
        setFilter(s -> precision == 0 ? s.matches("-?\\d*") : s.matches("-?\\d*\\.?\\d*"));
        setValue(precision == 0 ? "0" : "0.0");
    }

    public NumberBox setRange(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        clickedInside = isMouseOver(mouseX, mouseY);
        return result;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if(focused) {
            clickedInside = false;
        } else if(clickedInside) {
            clickedInside = false;
        } else {
            double value = getDouble();
            double clamped = Math.max(min, Math.min(max, value));
            if(clamped != value || getValue().isEmpty() || getValue().equals("-")) setNumber(clamped);
        }
    }

    public void setNumber(double value) {
        if(precision == 0) {
            setValue(Long.toString(Math.round(value)));
        } else {
            setValue(BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        }
    }

    public double getDouble() {
        try {
            return Double.parseDouble(getValue());
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    public float getFloat() {
        return (float) getDouble();
    }

    public int getInteger() {
        return (int) Math.round(getDouble());
    }

    public long getLong() {
        return Math.round(getDouble());
    }
}
