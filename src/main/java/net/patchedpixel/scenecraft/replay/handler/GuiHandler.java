package net.patchedpixel.scenecraft.replay.handler;

import net.patchedpixel.scenecraft.compat.SceneCraftCompat;
import net.patchedpixel.scenecraft.core.gui.GuiReplayButton;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.Setting;
import net.patchedpixel.scenecraft.gui.container.VanillaGuiScreen;
import net.patchedpixel.scenecraft.gui.element.GuiTooltip;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.gui.utils.geometry.Point;
import net.patchedpixel.scenecraft.gui.versions.callbacks.InitScreenCallback;
import net.patchedpixel.scenecraft.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.patchedpixel.scenecraft.core.versions.ScreenWidgets;
import net.minecraftforge.fml.ModList;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.patchedpixel.scenecraft.core.versions.MCVer.*;
import static net.patchedpixel.scenecraft.replay.SceneCraftReplay.LOGGER;

public class GuiHandler extends EventRegistrations {
    private static final int BUTTON_REPLAY_VIEWER = 17890234;
    private static final int BUTTON_EXIT_REPLAY = 17890235;

    private final SceneCraftReplay mod;

    public GuiHandler(SceneCraftReplay mod) {
        this.mod = mod;
    }

    { on(InitScreenCallback.EVENT, this::injectIntoIngameMenu); }
    private void injectIntoIngameMenu(Screen guiScreen, Collection<AbstractWidget> buttonList) {
        if (!(guiScreen instanceof PauseScreen)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Pause replay when menu is opened
            mod.getReplayHandler().getReplaySender().setReplaySpeed(0);
            final Component BUTTON_EXIT_SERVER = Component.translatable("menu.disconnect");
            final Component BUTTON_ADVANCEMENTS = Component.translatable("gui.advancements");
            final Component BUTTON_STATS = Component.translatable("gui.stats");
            final Component BUTTON_OPEN_TO_LAN = Component.translatable("menu.shareToLan");

            AbstractWidget achievements = null, stats = null;
            for(AbstractWidget b : new ArrayList<>(buttonList)) {
                boolean remove = false;
                Component id = b.getMessage();
                if (id == null) {
                    // likely a button of some third-part mod
                    // e.g. https://github.com/Pokechu22/WorldDownloader/blob/b1b279f948beec2d7dac7524eea8f584a866d8eb/share_14/src/main/java/wdl/WDLHooks.java#L491
                    continue;
                }
                if (id.equals(BUTTON_EXIT_SERVER)) {
                    // Replace "Exit Server" button with "Exit Replay" button
                    remove = true;
                    addButton(guiScreen, new InjectedButton(
                            guiScreen,
                            BUTTON_EXIT_REPLAY,
                            b.getX(),
                            b.getY(),
                            b.getWidth(),
                            b.getHeight(),
                            "scenecraft.gui.exit",
                            null,
                            this::onButton
                    ));
                } else if (id.equals(BUTTON_ADVANCEMENTS)) {
                    // Remove "Advancements", "Stats" and "Open to LAN" buttons
                    remove = true;
                    achievements = b;
                } else if (id.equals(BUTTON_STATS)) {
                    remove = true;
                    stats = b;
                } else if (id.equals(BUTTON_OPEN_TO_LAN)) {
                    remove = true;
                }
                if (remove) {
                    // Moving the button far off-screen is easier to do cross-version than actually removing it
                    b.setX(-1000);
                    b.setY(-1000);
                }
            }
            if (achievements != null && stats != null) {
                moveAllButtonsInRect(buttonList,
                        achievements.getX(), stats.getX() + stats.getWidth(),
                        achievements.getY(), Integer.MAX_VALUE,
                        -24);
            }
            // In 1.13+ Forge, the Options button shares one row with the Open to LAN button
        }
    }

    /**
     * Moves all buttons that in any way intersect a rectangle by a given amount on the y axis.
     * @param buttons List of buttons
     * @param yStart Top y limit of the rectangle
     * @param yEnd Bottom y limit of the rectangle
     * @param xStart Left x limit of the rectangle
     * @param xEnd Right x limit of the rectangle
     * @param moveBy Signed distance to move the buttons
     */
    private void moveAllButtonsInRect(
            Collection<AbstractWidget> buttons,
            int xStart,
            int xEnd,
            int yStart,
            int yEnd,
            int moveBy
    ) {
        buttons.stream()
                .filter(button -> button.getX() <= xEnd && button.getX() + button.getWidth() >= xStart)
                .filter(button -> button.getY() <= yEnd && button.getY() + button.getHeight() >= yStart)
                // FIXME remap bug: needs the {} to recognize the setter (it also doesn't understand +=)
                .forEach(button -> { button.setY(button.getY() + moveBy); });
    }

    { on(InitScreenCallback.EVENT, (screen, buttons) -> ensureReplayStopped(screen)); }
    private void ensureReplayStopped(Screen guiScreen) {
        if (!(guiScreen instanceof TitleScreen || guiScreen instanceof JoinMultiplayerScreen)) {
            return;
        }
        SceneCraftCompat.recoverStaleHud();

        if (mod.getReplayHandler() != null) {
            // Something went terribly wrong and we ended up in the main menu with the replay still active.
            // To prevent players from joining live servers and using the CameraEntity, try to stop the replay now.
            try {
                mod.getReplayHandler().endReplay();
            } catch (IOException e) {
                LOGGER.error("Trying to stop broken replay: ", e);
            } finally {
                if (mod.getReplayHandler() != null) {
                    mod.forcefullyStopReplay();
                }
            }
        }
    }

    { on(InitScreenCallback.EVENT, this::injectIntoMainMenu); }
    private void injectIntoMainMenu(Screen guiScreen, Collection<AbstractWidget> buttonList) {
        if (!(guiScreen instanceof TitleScreen)) {
            return;
        }

        if (mod.getCore().getSettingsRegistry().get(Setting.LEGACY_MAIN_MENU_BUTTON)) {
            legacyInjectIntoMainMenu(guiScreen, buttonList);
        } else {
            properInjectIntoMainMenu(guiScreen);
        }
    }

    private void properInjectIntoMainMenu(Screen screen) {
        List<AbstractWidget> buttonList = ScreenWidgets.getWidgets(screen);
        MainMenuButtonPosition buttonPosition = MainMenuButtonPosition.valueOf(mod.getCore().getSettingsRegistry().get(Setting.MAIN_MENU_BUTTON));

        // Workaround for FancyMenu v2 initializing the screen twice, likely fixed in v3
        if (isFancyMenu2Installed()) {
            for (AbstractWidget button : buttonList) {
                if (button instanceof InjectedButton) {
                    return;
                }
            }
        }

        Point pos;
        if (buttonPosition == MainMenuButtonPosition.BIG) {
            int x = screen.width / 2 - 100;
            // We want to position our button below the realms button
            Optional<AbstractWidget> targetButton = findButton(buttonList, "menu.online", 14)
                    .map(Optional::of)
                    // or, if someone removed the realms button, we'll alternatively take the multiplayer one
                    .orElseGet(() -> findButton(buttonList, "menu.multiplayer", 2));

            int y = targetButton
                    // if we found some button, put our button at its position (we'll move it out of the way shortly)
                    .map(it -> it.getY())
                    // and if we can't even find that one, then just guess
                    .orElse(screen.height / 4 + 10 + 4 * 24);

            // Move all buttons above or at our one upwards
            moveAllButtonsInRect(buttonList,
                    x, x + 200,
                    Integer.MIN_VALUE, y,
                    -24);

            pos = new Point(x, y);
        } else {
            pos = determineButtonPos(buttonPosition, screen, buttonList);
        }

        AbstractWidget replayViewerButton;
        if (buttonPosition == MainMenuButtonPosition.BIG) {
            replayViewerButton = new InjectedButton(
                    screen, BUTTON_REPLAY_VIEWER,
                    pos.getX(), pos.getY(),
                    200, 20,
                    "scenecraft.gui.replayviewer",
                    null,
                    this::onButton
            );
        } else {
            replayViewerButton = new InjectedButton(
                    screen, BUTTON_REPLAY_VIEWER,
                    pos.getX(), pos.getY(),
                    20, 20,
                    "",
                    "scenecraft.gui.replayviewer",
                    this::onButton
            ) {
                @Override
                public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
                    super.renderWidget(context, mouseX, mouseY, delta);

                    context.setColor(1.0F, 1.0F, 1.0F, this.alpha);
                    context.blit(
                            GuiReplayButton.ICON,
                            this.getX() + 3, this.getY() + 3,
                            this.width - 6, this.height - 6,
                            0, 0,
                            1, 1,
                            1, 1
                    );
                    context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            };
        }

        int index = determineButtonIndex(buttonList, replayViewerButton);
        if (index != -1) {
            buttonList.add(index, replayViewerButton);
        } else {
            buttonList.add(replayViewerButton);
        }
    }

    private boolean isFancyMenu2Installed() {
        return ModList.get().getModContainerById("fancymenu")
                .map(c -> c.getModInfo().getVersion().toString().startsWith("2."))
                .orElse(false);
    }

    private void legacyInjectIntoMainMenu(Screen guiScreen, Collection<AbstractWidget> buttonList) {
        boolean isCustomMainMenuMod = guiScreen.getClass().getName().endsWith("custommainmenu.gui.GuiFakeMain");

        MainMenuButtonPosition buttonPosition = MainMenuButtonPosition.valueOf(mod.getCore().getSettingsRegistry().get(Setting.MAIN_MENU_BUTTON));
        if (buttonPosition != MainMenuButtonPosition.BIG && !isCustomMainMenuMod) {
            VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(guiScreen);

            GuiReplayButton replayButton = new GuiReplayButton();
            replayButton
                    .onClick(() -> new GuiReplayViewer(mod).display())
                    .setTooltip(new GuiTooltip().setI18nText("scenecraft.gui.replayviewer"));

            vanillaGui.setLayout(new CustomLayout<GuiScreen>(vanillaGui.getLayout()) {
                private Point pos;

                @Override
                protected void layout(GuiScreen container, int width, int height) {
                    if (pos == null) {
                        // Delaying computation so we can take into account buttons
                        // added after our callback.
                        pos = determineButtonPos(buttonPosition, guiScreen, buttonList);
                    }
                    size(replayButton, 20, 20);
                    pos(replayButton, pos.getX(), pos.getY());
                }
            }).addElements(null, replayButton);
            return;
        }

        int x = guiScreen.width / 2 - 100;
        // We want to position our button below the realms button
        int y = findButton(buttonList, "menu.online", 14)
                .map(Optional::of)
                // or, if someone removed the realms button, we'll alternatively take the multiplayer one
                .orElse(findButton(buttonList, "menu.multiplayer", 2))
                // if we found some button, put our button at its position (we'll move it out of the way shortly)
                .map(it -> it.getY())
                // and if we can't even find that one, then just guess
                .orElse(guiScreen.height / 4 + 10 + 4 * 24);

        // Move all buttons above or at our one upwards
        moveAllButtonsInRect(buttonList,
                x, x + 200,
                Integer.MIN_VALUE, y,
                -24);

        // Add our button
        InjectedButton button = new InjectedButton(
                guiScreen,
                BUTTON_REPLAY_VIEWER,
                x,
                y,
                200,
                20,
                "scenecraft.gui.replayviewer",
                null,
                this::onButton
        );
        addButton(guiScreen, button);
    }

    private Point determineButtonPos(MainMenuButtonPosition buttonPosition, Screen guiScreen, Collection<AbstractWidget> buttonList) {
        Point topRight = new Point(guiScreen.width - 20 - 5, 5);

        if (buttonPosition == MainMenuButtonPosition.TOP_LEFT) {
            return new Point(5, 5);
        } else if (buttonPosition == MainMenuButtonPosition.TOP_RIGHT) {
            return topRight;
        } else if (buttonPosition == MainMenuButtonPosition.DEFAULT) {
            return Stream.of(
                            findButton(buttonList, "menu.singleplayer", 1),
                            findButton(buttonList, "menu.multiplayer", 2),
                            findButton(buttonList, "menu.online", 14)
                    )
                    // skip buttons which do not exist
                    .flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty))
                    // skip buttons which already have something next to them
                    .filter(it -> buttonList.stream().noneMatch(button ->
                            button.getX() <= it.getX() + it.getWidth() + 4 + 20
                                    && button.getY() <= it.getY() + it.getHeight()
                                    && button.getX() + button.getWidth() >= it.getX() + it.getWidth() + 4
                                    && button.getY() + button.getHeight() >= it.getY()
                    ))
                    // then take the bottom-most and if there's two, the right-most
                    .max(Comparator.<AbstractWidget>comparingInt(it -> it.getY()).thenComparingInt(it -> it.getX()))
                    // and place ourselves next to it
                    .map(it -> new Point(it.getX() + it.getWidth() + 4, it.getY()))
                    // if all fails, just go with TOP_RIGHT
                    .orElse(topRight);
        } else {
            return Optional.of(buttonList).flatMap(buttons -> {
                switch (buttonPosition) {
                    case LEFT_OF_SINGLEPLAYER:
                    case RIGHT_OF_SINGLEPLAYER:
                        return findButton(buttons, "menu.singleplayer", 1);
                    case LEFT_OF_MULTIPLAYER:
                    case RIGHT_OF_MULTIPLAYER:
                        return findButton(buttons, "menu.multiplayer", 2);
                    case LEFT_OF_REALMS:
                    case RIGHT_OF_REALMS:
                        return findButton(buttons, "menu.online", 14);
                }
                throw new RuntimeException();
            }).map(button -> {
                switch (buttonPosition) {
                    case LEFT_OF_SINGLEPLAYER:
                    case LEFT_OF_MULTIPLAYER:
                    case LEFT_OF_REALMS:
                    case RIGHT_OF_SINGLEPLAYER:
                    case RIGHT_OF_MULTIPLAYER:
                    case RIGHT_OF_REALMS:
                        return new Point(button.getX() + button.getWidth() + 4, button.getY());
                }
                throw new RuntimeException();
            }).orElse(topRight);
        }
    }

    private int determineButtonIndex(Collection<AbstractWidget> buttons, AbstractWidget button) {
        AbstractWidget best = null;
        int bestIndex = -1;

        int index = 0;
        for (AbstractWidget other : buttons) {
            if (other.getY() > button.getY() || other.getY() == button.getY() && other.getX() > button.getX()) {
                index++;
                continue;
            }

            if (best == null || other.getY() > best.getY() || other.getY() == best.getY() && other.getX() > best.getX()) {
                best = other;
                bestIndex = index + 1;
            }

            index++;
        }
        return bestIndex;
    }

    private void onButton(InjectedButton button) {
        Screen guiScreen = button.guiScreen;
        if(!button.active) return;

        if (guiScreen instanceof TitleScreen) {
            if (button.id == BUTTON_REPLAY_VIEWER) {
                new GuiReplayViewer(mod).display();
            }
        }

        if (guiScreen instanceof PauseScreen && mod.getReplayHandler() != null) {
            if (button.id == BUTTON_EXIT_REPLAY) {
                button.active = false;
                try {
                    mod.getReplayHandler().endReplay();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class InjectedButton extends Button {
        public final Screen guiScreen;
        public final int id;
        private Consumer<InjectedButton> onClick;
        public InjectedButton(Screen guiScreen, int buttonId, int x, int y, int width, int height, String buttonText, String tooltip, Consumer<InjectedButton> onClick) {
            super(x, y, width, height, Component.translatable(buttonText), self -> onClick.accept((InjectedButton) self), Button.DEFAULT_NARRATION);
            this.guiScreen = guiScreen;
            this.id = buttonId;
            this.onClick = onClick;

            if (tooltip != null) {
                setTooltip(Tooltip.create(Component.translatable(tooltip)));
            }
        }
    }

    public enum MainMenuButtonPosition {
        // The old big button below Realms/Mods which pushes other buttons around.
        BIG,
        // Right of the bottom-most button in the main block of buttons (so not the quit button).
        // That will generally be either RIGHT_OF_REALMS or RIGHT_OF_MODS depending on version and installed mods.
        DEFAULT,
        TOP_LEFT,
        TOP_RIGHT,
        LEFT_OF_SINGLEPLAYER,
        RIGHT_OF_SINGLEPLAYER,
        LEFT_OF_MULTIPLAYER,
        RIGHT_OF_MULTIPLAYER,
        LEFT_OF_REALMS,
        RIGHT_OF_REALMS
    }
}
