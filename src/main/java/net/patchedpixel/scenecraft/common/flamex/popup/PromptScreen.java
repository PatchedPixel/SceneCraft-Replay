package net.patchedpixel.scenecraft.common.flamex.popup;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.FlameX;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PromptScreen extends PopupScreen {
    private final String titleKey;
    private final String initial;
    private final String yesKey, noKey;
    private final Predicate<String> valid;
    private final Consumer<String> onAccept;
    private EditBox field;
    private Button yes;

    public PromptScreen(@Nullable Screen parent, String titleKey, String initial, String yesKey, String noKey, Predicate<String> valid, Consumer<String> onAccept) {
        super(parent, 7);
        this.dim = true;
        this.titleKey = titleKey;
        this.initial = initial;
        this.yesKey = yesKey;
        this.noKey = noKey;
        this.valid = valid;
        this.onAccept = onAccept;
    }

    @Override
    protected void build(LinearLayout root) {
        root.addChild(label(Component.translatable(titleKey)));
        field = FlameX.textField(200, initial, null, s -> update());
        field.setWidth(200);
        root.addChild(field);
        LinearLayout buttons = FlameX.row(5);
        yes = FlameX.button(yesKey, 98, this::accept);
        buttons.addChild(yes);
        buttons.addChild(FlameX.button(noKey, 98, this::close));
        root.addChild(buttons);
        update();
    }

    @Override
    protected void afterLayout() {
        setInitialFocus(field);
    }

    private void update() {
        if(yes != null) yes.active = valid.test(field.getValue());
    }

    private void accept() {
        String value = field.getValue();
        close();
        onAccept.accept(value);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && yes.active) {
            accept();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
