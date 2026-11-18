package net.patchedpixel.scenecraft.common.compat;

import net.patchedpixel.scenecraft.common.extras.FullBrightness;

import java.util.function.BooleanSupplier;

public class BadOptimizationsLightmapHook implements BooleanSupplier {
    public BadOptimizationsLightmapHook() {}

    @Override
    public boolean getAsBoolean() {
        return FullBrightness.consumeLightmapDirty();
    }
}
