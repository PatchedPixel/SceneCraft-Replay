package net.patchedpixel.scenecraft.common.flamex.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import net.patchedpixel.scenecraft.common.flamex.widget.NineSlice;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import javax.annotation.Nullable;

public abstract class FlameableScreen extends Screen {
    public enum Background {
        DEFAULT,
        DIRT,
        NONE
    }

    @Nullable
    protected final Screen parent;
    protected Background background = Background.DEFAULT;
    protected boolean drawTitle = true;
    protected boolean panel;
    protected boolean dim;
    protected LinearLayout root;
    private final int spacing;

    protected FlameableScreen(Component title, @Nullable Screen parent, int spacing) {
        super(title);
        this.parent = parent;
        this.spacing = spacing;
    }

    protected FlameableScreen(Component title, @Nullable Screen parent) {
        this(title, parent, 8);
    }

    protected FlameableScreen(String titleKey, @Nullable Screen parent, int spacing) {
        this(Component.translatable(titleKey), parent, spacing);
    }

    protected FlameableScreen(String titleKey, @Nullable Screen parent) {
        this(Component.translatable(titleKey), parent, 8);
    }

    protected abstract void build(LinearLayout root);

    protected void afterLayout() {}

    protected int outerPadding() {
        return root instanceof FlameableLayout layout ? layout.verticalOuterPadding() : 0;
    }

    protected int rootX() {
        return (this.width - root.getWidth()) / 2;
    }

    protected int rootY() {
        return (this.height - root.getHeight()) / 2;
    }

    @Override
    protected void init() {
        root = FlameX.column(spacing);
        if(panel && !title.getString().isEmpty()) root.addChild(FlameX.label(title));
        build(root);
        if(panel) {
            root.visitWidgets(widget -> {
                if(widget instanceof StringWidget label) label.setColor(FlameXTheme.TEXT_ON_PANEL);
            });
        }

        root.visitWidgets(this::addRenderableWidget);
        root.arrangeElements();
        FrameLayout.centerInRectangle(root, 0, 0, this.width, this.height);
        root.setX(rootX());
        root.setY(rootY());
        afterLayout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if(panel) {
            if(parent != null) {
                parent.render(graphics, -1, -1, partialTicks);
            } else {
                this.renderBackground(graphics);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);
            if(dim) graphics.fill(0, 0, this.width, this.height, FlameXTheme.DIM);
            int outer = outerPadding();
            NineSlice.panel(graphics, root.getX() - FlameXTheme.PANEL_PADDING, root.getY() + outer - FlameXTheme.PANEL_PADDING,
                    root.getWidth() + FlameXTheme.PANEL_PADDING * 2, root.getHeight() - outer * 2 + FlameXTheme.PANEL_PADDING * 2);
        } else switch(background) {
            case DIRT -> this.renderDirtBackground(graphics);
            case DEFAULT -> this.renderBackground(graphics);
            case NONE -> {}
        }

        if(drawTitle && !panel) {
            graphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, FlameXTheme.TITLE_Y, FlameXTheme.TITLE_COLOR | 0xFF000000, false);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
        if(panel) graphics.pose().popPose();
    }

    public void display() {
        MCVer.getMinecraft().setScreen(this);
    }

    public void close() {
        MCVer.getMinecraft().setScreen(parent);
    }

    @Override
    public void onClose() {
        close();
    }
}
