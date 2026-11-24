package net.patchedpixel.scenecraft.common.flamex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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

    @Override
    protected void init() {
        root = FlameX.column(spacing);
        if(panel && !title.getString().isEmpty()) root.addChild(FlameX.label(title));
        build(root);
        if(panel) {
            root.visitWidgets(w -> {
                if(w instanceof StringWidget label) label.setColor(FlameXTheme.TEXT_ON_PANEL);
            });
        }

        root.visitWidgets(this::addRenderableWidget);
        root.arrangeElements();
        FrameLayout.centerInRectangle(root, 0, 0, this.width, this.height);
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
            NineSlice.panel(graphics, root.getX() - FlameXTheme.PANEL_PADDING, root.getY() - FlameXTheme.PANEL_PADDING,
                    root.getWidth() + FlameXTheme.PANEL_PADDING * 2, root.getHeight() + FlameXTheme.PANEL_PADDING * 2);
        } else switch(background) {
            case DIRT -> this.renderDirtBackground(graphics);
            case DEFAULT -> this.renderBackground(graphics);
            case NONE -> {}
        }

        if(drawTitle && !panel) {
            graphics.drawCenteredString(this.font, this.title, this.width / 2, FlameXTheme.TITLE_Y, FlameXTheme.TITLE_COLOR);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
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
