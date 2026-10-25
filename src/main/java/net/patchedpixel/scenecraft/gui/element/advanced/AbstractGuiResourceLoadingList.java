/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.patchedpixel.scenecraft.gui.element.advanced;

import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.gui.GuiRenderer;
import net.patchedpixel.scenecraft.gui.RenderInfo;
import net.patchedpixel.scenecraft.gui.container.AbstractGuiVerticalList;
import net.patchedpixel.scenecraft.gui.container.GuiContainer;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.element.GuiElement;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.function.Click;
import net.patchedpixel.scenecraft.gui.function.Clickable;
import net.patchedpixel.scenecraft.gui.function.Closeable;
import net.patchedpixel.scenecraft.gui.function.KeyHandler;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.function.Loadable;
import net.patchedpixel.scenecraft.gui.function.Tickable;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.layout.VerticalLayout;
import net.patchedpixel.scenecraft.gui.utils.Colors;
import net.patchedpixel.scenecraft.gui.utils.Consumer;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.Point;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static net.patchedpixel.scenecraft.gui.utils.Utils.DOUBLE_CLICK_INTERVAL;

public abstract class AbstractGuiResourceLoadingList
        <T extends AbstractGuiResourceLoadingList<T, U>, U extends GuiElement<U> & Comparable<U>>
        extends AbstractGuiVerticalList<T> implements Tickable, Loadable, Closeable, KeyHandler {
    private static final String[] LOADING_TEXT = {"Ooo", "oOo", "ooO", "oOo"};
    private final GuiLabel loadingElement = new GuiLabel();
    private final GuiPanel resourcesPanel = new GuiPanel(getListPanel()).setLayout(new VerticalLayout());

    private final Queue<Runnable> resourcesQueue = new ConcurrentLinkedQueue<>();

    private Consumer<Consumer<Supplier<U>>> onLoad;
    private Runnable onSelectionChanged;
    private Runnable onSelectionDoubleClicked;
    private Thread loaderThread;
    private int tick;

    private final List<Element> selected = new ArrayList<>();
    private long selectedLastClickTime;

    public AbstractGuiResourceLoadingList() {
    }

    public AbstractGuiResourceLoadingList(GuiContainer container) {
        super(container);
    }

    @Override
    public void tick() {
        loadingElement.setText(LOADING_TEXT[tick++ / 5 % LOADING_TEXT.length]);
        Runnable resource;
        while ((resource = resourcesQueue.poll()) != null) {
            resource.run();
        }
    }

    @Override
    public void load() {
        // Stop current loading
        if (loaderThread != null) {
            loaderThread.interrupt();
            try {
                loaderThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Clear list
        resourcesQueue.clear();
        for (GuiElement element : new ArrayList<>(resourcesPanel.getChildren())) {
            resourcesPanel.removeElement(element);
        }
        selected.clear();
        onSelectionChanged();

        // Load new data
        loaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    onLoad.consume(new Consumer<Supplier<U>>() {
                        @Override
                        public void consume(final Supplier<U> obj) {
                            resourcesQueue.offer(new Runnable() {
                                @Override
                                public void run() {
                                    resourcesPanel.addElements(null, new Element(obj.get()));
                                    resourcesPanel.sortElements();
                                }
                            });
                        }
                    });
                } finally {
                    resourcesQueue.offer(new Runnable() {
                        @Override
                        public void run() {
                            getListPanel().removeElement(loadingElement);
                        }
                    });
                }
            }
        });
        getListPanel().addElements(new VerticalLayout.Data(0.5), loadingElement);
        loaderThread.start();
    }

    @Override
    public void close() {
        loaderThread.interrupt();
    }

    public T onLoad(Consumer<Consumer<Supplier<U>>> function) {
        this.onLoad = function;
        return getThis();
    }

    public void onSelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    public void onSelectionDoubleClicked() {
        if (onSelectionDoubleClicked != null) {
            onSelectionDoubleClicked.run();
        }
    }

    public T onSelectionChanged(Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
        return getThis();
    }

    public T onSelectionDoubleClicked(Runnable onSelectionDoubleClicked) {
        this.onSelectionDoubleClicked = onSelectionDoubleClicked;
        return getThis();
    }

    public List<U> getSelected() {
        List<U> selectedResources = new ArrayList<>(selected.size());
        for (Element element : selected) {
            selectedResources.add(element.resource);
        }
        return selectedResources;
    }

    @Override
    public boolean handleKey(KeyInput keyInput) {
        if (keyInput.hasCtrl() && keyInput.keycode == InputConstants.KEY_A) {
            List<Element> all = new ArrayList<>();
            for (GuiElement<?> child : getListPanel().getChildren()) {
                if (child instanceof AbstractGuiResourceLoadingList.Element) {
                    //noinspection unchecked
                    all.add((Element) child);
                }
            }
            if (selected.size() < all.size()) {
                selected.clear();
                selected.addAll(all);
            } else {
                selected.clear();
            }
            onSelectionChanged();
            return true;
        }

        return false;
    }

    private class Element extends GuiPanel implements Clickable, Comparable<Element> {
        private final U resource;

        public Element(final U resource) {
            this.resource = resource;
            addElements(null, resource);
            setLayout(new CustomLayout<GuiPanel>() {
                @Override
                protected void layout(GuiPanel container, int width, int height) {
                    pos(resource, 2, 2);
                }

                @Override
                public ReadableDimension calcMinSize(GuiContainer<?> container) {
                    ReadableDimension size = resource.getMinSize();
                    return new Dimension(size.getWidth() + 4, size.getHeight() + 4);
                }
            });
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            if (renderInfo.layer == 0 && selected.contains(this)) {
                // Draw selection
                int w = size.getWidth();
                int h = size.getHeight();
                // Black background
                renderer.drawRect(0, 0, w, h, Colors.BLACK);
                // Light gray border
                renderer.drawRect(0, 0, w, 1, Colors.LIGHT_GRAY); // Top
                renderer.drawRect(0, h - 1, w, 1, Colors.LIGHT_GRAY); // Bottom
                renderer.drawRect(0, 0, 1, h, Colors.LIGHT_GRAY); // Left
                renderer.drawRect(w - 1, 0, 1, h, Colors.LIGHT_GRAY); // Right
            }
            super.draw(renderer, size, renderInfo);
        }

        @Override
        public boolean mouseClick(Click click) {
            Point point = new Point(click);
            getContainer().convertFor(this, point);
            if (point.getX() > 0 && point.getX() < getLastSize().getWidth()
                    && point.getY() > 0 && point.getY() < getLastSize().getHeight()) {
                if (click.hasCtrl()) {
                    if (selected.contains(this)) {
                        selected.remove(this);
                    } else {
                        selected.add(this);
                    }
                    onSelectionChanged();
                } else if (selected.contains(this) && System.currentTimeMillis() - selectedLastClickTime < DOUBLE_CLICK_INTERVAL) {
                    onSelectionDoubleClicked();
                } else {
                    selected.clear();
                    selected.add(this);
                    onSelectionChanged();
                }
                selectedLastClickTime = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        @Override
        public int compareTo(Element o) {
            return resource.compareTo(o.resource);
        }
    }
}
