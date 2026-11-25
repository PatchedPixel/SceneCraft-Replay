package net.patchedpixel.scenecraft.common.flamex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.popup.MessageScreen;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class FlameX {
    private FlameX() {}

    public static Component t(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static Button button(String i18nKey, int width, Runnable onPress) {
        return Button.builder(t(i18nKey), button -> onPress.run()).width(width).build();
    }

    public static Button button(Component label, int width, Runnable onPress) {
        return Button.builder(label, button -> onPress.run()).width(width).build();
    }

    public static Button button(Component label, int width, String tooltipKey, Runnable onPress) {
        Button button = button(label, width, onPress);
        button.setTooltip(Tooltip.create(t(tooltipKey)));

        return button;
    }

    public static Button button(String i18nKey, int width, String tooltipKey, Runnable onPress) {
        Button button = button(i18nKey, width, onPress);
        button.setTooltip(Tooltip.create(t(tooltipKey)));

        return button;
    }

    public static StringWidget label(Component text) {
        Minecraft mc = MCVer.getMinecraft();
        return new StringWidget(text, MCVer.getFontRenderer());
    }

    public static StringWidget label(String i18nKey) {
        return label(t(i18nKey));
    }

    public static Checkbox checkbox(String i18nKey, boolean selected, Consumer<Boolean> onChange) {
        Minecraft mc = MCVer.getMinecraft();
        Component text = t(i18nKey);
        return new Checkbox(0, 0, 20 + MCVer.getFontRenderer().width(text) + 4, 20, text, selected) {
            @Override
            public void onPress() {
                super.onPress();
                onChange.accept(selected());
            }
        };
    }

    public static EditBox textField(int width, String value, Predicate<String> filter, Consumer<String> onChange) {
        Minecraft mc = MCVer.getMinecraft();
        EditBox box = new EditBox(MCVer.getFontRenderer(), 0, 0, width, 20, Component.empty());
        box.setMaxLength(Integer.MAX_VALUE);
        if(filter != null) {
            box.setFilter(filter);
        }

        box.setValue(value);
        if(onChange != null) {
            box.setResponder(onChange);
        }

        return box;
    }

    public static EditBox integerField(int width, int value, Consumer<Integer> onChange) {
        return textField(width, String.valueOf(value), s -> s.isEmpty() || s.matches("-?\\d*"), s -> {
            try {
                onChange.accept(Integer.parseInt(s));
            } catch(NumberFormatException ignored) {}
        });
    }

    public static <T> CycleButton<T> cycle(Component label, List<T> values, T initial,
                                           Function<T, Component> valueText, int width, Consumer<T> onChange) {
        return CycleButton.<T>builder(valueText).withValues(values).withInitialValue(initial)
                .create(0, 0, width, 20, label, (b, v) -> onChange.accept(v));
    }

    public static CycleButton<Boolean> toggle(Component label, boolean initial, int width, Consumer<Boolean> onChange) {
        return CycleButton.onOffBuilder(initial).create(0, 0, width, 20, label, (b, v) -> onChange.accept(v));
    }

    public static Screen info(Runnable onClose, String... lines) {
        Component[] text = new Component[lines.length];
        for(int i = 0; i < lines.length; i++) {
            text[i] = t(lines[i]);
        }

        return new MessageScreen(MCVer.getMinecraft().screen, onClose, null, null, text);
    }

    public static Screen confirm(Screen parent, String messageKey, String yesKey, String noKey, Runnable onYes) {
        return new MessageScreen(parent, onYes, yesKey, noKey, t(messageKey));
    }

    public static LinearLayout row(int spacing) {
        return new FlameableLayout(LinearLayout.Orientation.HORIZONTAL, Math.max(0, spacing / 2));
    }

    public static LinearLayout column(int spacing) {
        return new FlameableLayout(LinearLayout.Orientation.VERTICAL, Math.max(0, spacing / 2));
    }
}
