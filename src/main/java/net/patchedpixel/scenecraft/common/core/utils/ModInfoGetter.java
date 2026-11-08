package net.patchedpixel.scenecraft.common.core.utils;

import net.patchedpixel.scenecraft.common.scenestudio.data.ModInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.patchedpixel.scenecraft.platform.ModLoader;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModInfo> modInfoMap = ModLoader.getLoadedMods().stream()
                .map(mod -> new ModInfo(
                        mod.id(),
                        mod.displayName(),
                        mod.version()
                ))
                .collect(Collectors.toMap(ModInfo::getId, Function.identity()));

        return net.minecraft.core.registries.BuiltInRegistries.REGISTRY.stream()
                .map(Registry::keySet)
                .flatMap(Set::stream)
                .map(Identifier::getNamespace)
                .filter(s -> !s.equals("minecraft"))
                .distinct()
                .map(modInfoMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}