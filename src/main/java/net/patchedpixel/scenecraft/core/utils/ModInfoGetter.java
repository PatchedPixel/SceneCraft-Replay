package net.patchedpixel.scenecraft.core.utils;

import net.patchedpixel.scenecraft.scenestudio.data.ModInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        Map<String, ModInfo> modInfoMap = ModList.get().getMods().stream()
                .map(mod -> new ModInfo(
                        mod.getModId(),
                        mod.getDisplayName(),
                        mod.getVersion().toString()
                ))
                .collect(Collectors.toMap(ModInfo::getId, Function.identity()));

        return net.minecraft.core.registries.BuiltInRegistries.REGISTRY.stream()
                .map(Registry::keySet)
                .flatMap(Set::stream)
                .map(ResourceLocation::getNamespace)
                .filter(s -> !s.equals("minecraft"))
                .distinct()
                .map(modInfoMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}