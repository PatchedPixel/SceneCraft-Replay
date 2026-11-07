package net.patchedpixel.scenecraft.common.core;

import net.patchedpixel.scenecraft.platform.KeyBindingRegistry;

public interface Module {
    default void initCommon() {}
    default void initClient() {}
    default void registerKeyBindings(KeyBindingRegistry registry) {}
}
