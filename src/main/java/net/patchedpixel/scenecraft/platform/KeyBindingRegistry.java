package net.patchedpixel.scenecraft.platform;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.patchedpixel.scenecraft.core.events.PreRenderCallback;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.core.events.KeyBindingEventCallback;
import net.patchedpixel.scenecraft.core.events.KeyEventCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import static net.patchedpixel.scenecraft.SceneCraft.MOD_ID;
import static net.patchedpixel.scenecraft.core.versions.MCVer.identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class KeyBindingRegistry extends EventRegistrations {
    public static final String LEGACY_KEY_PREFIX = "scenecraft.input.";
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(identifier(MOD_ID, "general"));

    private final List<KeyMapping> pendingMappings = new ArrayList<>();

    private final Map<String, Binding> bindings = new HashMap<>();
    private Set<KeyMapping> onlyInReplay = new HashSet<>();
    private Multimap<Integer, Function<KeyInput, Boolean>> rawHandlers = ArrayListMultimap.create();

    public Binding registerKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInRepay) {
        Binding binding = registerKeyBinding(name, keyCode, onlyInRepay);
        binding.handlers.add(whenPressed);
        return binding;
    }

    public Binding registerRepeatedKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInRepay) {
        Binding binding = registerKeyBinding(name, keyCode, onlyInRepay);
        binding.repeatedHandlers.add(whenPressed);
        return binding;
    }

    private Binding registerKeyBinding(String name, int keyCode, boolean onlyInRepay) {
        Binding binding = bindings.get(name);
        if (binding == null) {
            if (keyCode <= 0) {
                keyCode = InputConstants.UNKNOWN.getValue();
            }
            Identifier id = identifier(MOD_ID, name.substring(LEGACY_KEY_PREFIX.length()));
            String key = String.format("key.%s.%s", id.getNamespace(), id.getPath());
            KeyMapping keyBinding = new KeyMapping(key, InputConstants.Type.KEYBOARD, keyCode, CATEGORY);
            pendingMappings.add(keyBinding);
            binding = new Binding(name, keyBinding);
            bindings.put(name, binding);
            if (onlyInRepay) {
                this.onlyInReplay.add(keyBinding);
            }
        } else if (!onlyInRepay) {
            this.onlyInReplay.remove(binding.keyBinding);
        }
        return binding;
    }

    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        pendingMappings.forEach(event::register);
    }

    public void registerRaw(int keyCode, Function<KeyInput, Boolean> whenPressed) {
        rawHandlers.put(keyCode, whenPressed);
    }

    public Map<String, Binding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public Set<KeyMapping> getOnlyInReplay() {
        return Collections.unmodifiableSet(onlyInReplay);
    }

    { on(PreRenderCallback.EVENT, this::handleRepeatedKeyBindings); }

    public void handleRepeatedKeyBindings() {
        for (Binding binding : bindings.values()) {
            if (binding.keyBinding.isDown()) {
                invokeKeyBindingHandlers(binding, binding.repeatedHandlers);
            }
        }
    }

    { on(KeyBindingEventCallback.EVENT, this::handleKeyBindings); }
    private void handleKeyBindings() {
        for (Binding binding : bindings.values()) {
            while (binding.keyBinding.consumeClick()) {
                invokeKeyBindingHandlers(binding, binding.handlers);
                invokeKeyBindingHandlers(binding, binding.repeatedHandlers);
            }
        }
    }

    private void invokeKeyBindingHandlers(Binding binding, Collection<Runnable> handlers) {
        for (final Runnable runnable : handlers) {
            try {
                runnable.run();
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.forThrowable(cause, "Handling Key Binding");
                CrashReportCategory category = crashReport.addCategory("Key Binding");
                category.setDetail("Key Binding", () -> binding.name);
                category.setDetail("Handler", runnable::toString);
                throw new ReportedException(crashReport);
            }
        }
    }

    { on(KeyEventCallback.EVENT, this::handleRaw); }
    private boolean handleRaw(KeyInput keyInput, int action) {
        if (action != KeyEventCallback.ACTION_PRESS) return false;
        for (final Function<KeyInput, Boolean> handler : rawHandlers.get(keyInput.key)) {
            try {
                if (handler.apply(keyInput)) {
                    return true;
                }
            } catch (Throwable cause) {
                CrashReport crashReport = CrashReport.forThrowable(cause, "Handling Raw Key Binding");
                CrashReportCategory category = crashReport.addCategory("Key Binding");
                category.setDetail("Key Code", () -> "" + keyInput.key);
                category.setDetail("Handler", handler::toString);
                throw new ReportedException(crashReport);
            }
        }
        return false;
    }

    public class Binding {
        public final String name;
        public final KeyMapping keyBinding;
        private final List<Runnable> handlers = new ArrayList<>();
        private final List<Runnable> repeatedHandlers = new ArrayList<>();
        private boolean autoActivation;
        private Consumer<Boolean> autoActivationUpdate;

        public Binding(String name, KeyMapping keyBinding) {
            this.name = name;
            this.keyBinding = keyBinding;
        }

        public String getBoundKey() {
            try {
                return keyBinding.getTranslatedKeyMessage().getString();
            } catch (ArrayIndexOutOfBoundsException e) {
                // Apparently windows likes to press strange keys, see https://www.replaymod.com/forum/thread/55
                return "Unknown";
            }
        }

        public boolean isBound() {
            return !keyBinding.isUnbound();
        }

        public void trigger() {
            keyBinding.clickCount = keyBinding.clickCount + 1;
            handleKeyBindings();
        }

        public void registerAutoActivationSupport(boolean active, Consumer<Boolean> update) {
            this.autoActivation = active;
            this.autoActivationUpdate = update;
        }

        public boolean supportsAutoActivation() {
            return autoActivationUpdate != null;
        }

        public boolean isAutoActivating() {
            return supportsAutoActivation() && autoActivation;
        }

        public void setAutoActivating(boolean active) {
            if (this.autoActivation == active) {
                return;
            }
            this.autoActivation = active;
            this.autoActivationUpdate.accept(active);
        }
    }
}
