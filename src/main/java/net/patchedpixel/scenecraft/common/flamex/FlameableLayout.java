package net.patchedpixel.scenecraft.common.flamex;

import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class FlameableLayout extends LinearLayout {
    private final boolean horizontal;
    private final int childPadding;
    private final List<LayoutElement> elements = new ArrayList<>();

    public FlameableLayout(Orientation orientation, int childPadding) {
        super(0, 0, orientation);
        this.horizontal = orientation == Orientation.HORIZONTAL;
        this.childPadding = childPadding;
        if(horizontal) {
            defaultChildLayoutSetting().padding(childPadding, 0).alignVerticallyMiddle();
        } else {
            defaultChildLayoutSetting().padding(0, childPadding).alignHorizontallyCenter();
        }
    }

    @Override
    public <T extends LayoutElement> @NotNull T addChild(@NotNull T child, @NotNull LayoutSettings settings) {
        elements.add(child);
        return super.addChild(child, settings);
    }

    @Override
    public void arrangeElements() {
        int length = 0;
        for(LayoutElement element : elements) {
            if(element instanceof Layout layout) layout.arrangeElements();
            length += (horizontal ? element.getWidth() : element.getHeight()) + childPadding * 2;
        }
        if(horizontal) {
            this.width = length;
        } else {
            this.height = length;
        }
        super.arrangeElements();
    }
}
