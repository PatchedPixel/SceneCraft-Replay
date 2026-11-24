package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberBox extends EditBox {
    private final int precision;

    public NumberBox(int width, int precision) {
        super(MCVer.getFontRenderer(), 0, 0, width, 20, Component.empty());
        this.precision = precision;

        setMaxLength(64);
        setFilter(s -> precision == 0 ? s.matches("-?\\d*") : s.matches("-?\\d*\\.?\\d*"));
        setValue(precision == 0 ? "0" : "0.0");
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
