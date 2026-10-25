package net.patchedpixel.scenecraft.core;

public interface Module {
    default void initCommon() {}
    default void initClient() {}
    default void registerKeyBindings(KeyBindingRegistry registry) {}
}
