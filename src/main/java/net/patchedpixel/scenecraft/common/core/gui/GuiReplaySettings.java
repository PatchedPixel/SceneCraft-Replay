package net.patchedpixel.scenecraft.common.core.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;

import java.util.List;

public class GuiReplaySettings extends FlameableScreen {
    private final SettingsRegistry settingsRegistry;

    public GuiReplaySettings(final Screen parent, final SettingsRegistry settingsRegistry) {
        super("scenecraft.gui.settings.title", parent);
        this.settingsRegistry = settingsRegistry;
    }

    @Override
    protected void build(LinearLayout root) {}

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void init() {
        int i = 0;
        int left = width / 2 - 155;
        int top = height / 6;
        int[] columnY = {top, top};
        for(final SettingsRegistry.SettingKey<?> key : settingsRegistry.getSettings()) {
            if(key.getDisplayString() == null) continue;
            AbstractWidget element;
            Component label = Component.translatable(key.getDisplayString());
            if(key.getDefault() instanceof Boolean) {
                final SettingsRegistry.SettingKey<Boolean> booleanKey = (SettingsRegistry.SettingKey<Boolean>) key;
                element = FlameX.toggle(label, settingsRegistry.get(booleanKey), 150, value -> {
                    settingsRegistry.set(booleanKey, value);
                    settingsRegistry.save();
                });
            } else if(key instanceof SettingsRegistry.MultipleChoiceSettingKey) {
                final SettingsRegistry.MultipleChoiceSettingKey<?> multipleChoiceKey =
                        (SettingsRegistry.MultipleChoiceSettingKey<?>) key;
                List values = multipleChoiceKey.getChoices();
                Object currentValue = settingsRegistry.get(multipleChoiceKey);
                String prefix = label.getString() + ": ";
                element = FlameX.dropdown(values, currentValue, v -> prefix + I18n.get(v.toString()), 150, value -> {
                    settingsRegistry.set((SettingsRegistry.SettingKey) multipleChoiceKey, value);
                    settingsRegistry.save();
                });
            } else {
                throw new IllegalArgumentException("Type " + key.getDefault().getClass() + " not supported.");
            }
            int column = i++ % 2;
            element.setPosition(left + column * 160, columnY[column]);
            columnY[column] += 24;
            addRenderableWidget(element);
        }
        addRenderableWidget(FlameX.button("gui.done", 200, this::close)).setPosition(width / 2 - 100, height - 27);
    }
}
