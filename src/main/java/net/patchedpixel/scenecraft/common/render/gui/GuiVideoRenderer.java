package net.patchedpixel.scenecraft.common.render.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.render.rendering.VideoRenderer;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;
import net.patchedpixel.scenecraft.common.flamex.widget.ProgressBarWidget;
import net.patchedpixel.scenecraft.common.utils.Utils;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.nio.ByteBuffer;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.identifier;

public class GuiVideoRenderer extends Screen {
    private static final ResourceLocation NO_PREVIEW_TEXTURE = identifier(SceneCraft.MOD_ID, "unknown.png");

    private final VideoRenderer renderer;

    private boolean preview;
    private String renderTimeText = "";
    private String remainingTimeText = "";
    private boolean waitingForConfirmation;

    private final ProgressBarWidget progressBar = new ProgressBarWidget(200, 20);
    private final Checkbox previewCheckbox;
    private final Button pauseButton;
    private final Button cancelButton;

    private DynamicTexture previewTexture;
    private boolean previewTextureDirty;

    public GuiVideoRenderer(VideoRenderer renderer) {
        super(Component.translatable("scenecraft.gui.rendering.title"));
        this.renderer = renderer;
        previewCheckbox = FlameX.checkbox("scenecraft.gui.rendering.preview", false, v -> preview = v);
        pauseButton = FlameX.button("scenecraft.gui.rendering.pause", 150, () -> {
            if(renderer.isPaused()) {
                pauseButton().setMessage(Component.translatable("scenecraft.gui.rendering.pause"));
                renderer.setPaused(false);
            } else {
                pauseButton().setMessage(Component.translatable("scenecraft.gui.rendering.resume"));
                renderer.setPaused(true);
            }
        });
        cancelButton = FlameX.button("scenecraft.gui.rendering.cancel", 150, () -> {
            if(!waitingForConfirmation) {
                cancelButton().setMessage(Component.translatable("scenecraft.gui.rendering.cancel.callback"));
                waitingForConfirmation = true;
            } else {
                renderer.cancel();
            }
        });
    }

    private Button pauseButton() {
        return pauseButton;
    }

    private Button cancelButton() {
        return cancelButton;
    }

    @Override
    protected void init() {
        int buttonsY = height - 27;
        pauseButton.setPosition(width / 2 - 154, buttonsY);
        cancelButton.setPosition(width / 2 + 4, buttonsY);
        progressBar.setWidth(width - 10);
        progressBar.setPosition(5, buttonsY - 5 - 20);
        int renderTimeY = progressBar.getY() - 2 - font.lineHeight;
        previewCheckbox.setPosition(width / 2 - previewCheckbox.getWidth() / 2, renderTimeY - 10 - previewCheckbox.getHeight());
        addRenderableWidget(previewCheckbox);
        addRenderableWidget(progressBar);
        addRenderableWidget(pauseButton);
        addRenderableWidget(cancelButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        if(waitingForConfirmation && !cancelButton.isMouseOver(mouseX, mouseY)) {
            cancelButton.setMessage(Component.translatable("scenecraft.gui.rendering.cancel"));
            waitingForConfirmation = false;
        }
        return result;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // FIXME default background doesn't work during rendering because the blur effect relies on the framebuffer
        graphics.fill(0, 0, width, height, 0xFF202020);
        graphics.drawString(font, title, width / 2 - font.width(title) / 2, 3, 0xFFFFFFFF, false);

        int areaTop = 3 + font.lineHeight + 5;
        int areaBottom = previewCheckbox.getY() - 5;
        Size area = new Size(width - 10, Math.max(1, areaBottom - areaTop));
        if(preview) {
            renderPreview(graphics, area, 5, areaTop);
        } else {
            renderTexture(graphics, NO_PREVIEW_TEXTURE, area, 5, areaTop, 1280, 720);
        }

        int textY = progressBar.getY() - 2 - font.lineHeight;
        graphics.drawString(font, renderTimeText, 5, textY, 0xFFFFFFFF, false);
        graphics.drawString(font, remainingTimeText, width - 5 - font.width(remainingTimeText), textY, 0xFFFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    //the total render time
    private int renderTimeTaken = 0;

    //the time at which the Screen was last updated
    private long prevTime = -1;

    //the time at which the rendering of the current frame started
    private long frameStartTime = -1;

    //the amount of frames that were rendered when the time left was last updated
    private int prevRenderedFrames = 0;

    //the estimated render time that is left (in seconds)
    private int renderTimeLeft = 0;

    //each of the durations the rendering process took for the last 50 frames
    private int[] renderTimes = new int[50];
    //the algorithm's current position in the renderTimes array
    private int currentIndex = 0;

    @Override
    public void tick() {
        long current = System.nanoTime() / 1_000_000;

        //first, update the total render time (only if rendering is not paused and has already started)
        if(!renderer.isPaused() && renderer.getFramesDone() > 0 && prevTime > -1) {
            renderTimeTaken += (current - prevTime);
        } else {
            //if the rendering process is paused, we have to update the frame start time to prevent huge render times
            //for the currently rendered frame(s)
            frameStartTime = current;
        }

        //always update prevTime, so we don't get huge time jumps when unpausing the rendering process
        prevTime = current;

        //calculate estimated time left

        //if the amount of rendered frames has increased since the last update
        if(prevRenderedFrames < renderer.getFramesDone()) {
            //we don't include the first frame in our calculations,
            //as setting up the rendering process takes a few seconds
            if(prevRenderedFrames > 0) {

                //calculate the amount of frames that have been rendered since the last update
                int framesRendered = renderer.getFramesDone() - prevRenderedFrames;

                //calculate the time it took to render these frames
                int renderTime = (int)(current - frameStartTime);

                //calculate the average time it took for each of these frames to render
                int avgRenderTime = renderTime/framesRendered;

                //add all of the average render times to the render times array
                for(int i=0; i<framesRendered; i++) {
                    renderTimes[currentIndex] = avgRenderTime;
                    currentIndex++;
                    if(currentIndex >= renderTimes.length) currentIndex = 0;
                }

                //the renderTimes array initially contains lots of zeros,
                //so we count the amount of valid timespans
                int validValues = 0;

                int totalTime = 0;
                for(int i : renderTimes) {
                    if(i > 0) {
                        totalTime += i;
                        validValues++;
                    }
                }

                //calculate the average render time for the previous [up to 50] frames
                float averageRenderTime = validValues > 0 ? totalTime / validValues : 0;

                //calculate the remaining render time in seconds
                renderTimeLeft = Math.round((averageRenderTime * (renderer.getTotalFrames() - renderer.getFramesDone())) / 1000);
            }

            //set the render start time of the next frame(s) to the current timestamp
            frameStartTime = current;
            //update the amount of rendered frames for the last calculation
            prevRenderedFrames = renderer.getFramesDone();
        }

        renderTimeText = I18n.get("scenecraft.gui.rendering.timetaken") + ": " + secToString(renderTimeTaken/1000);
        remainingTimeText = I18n.get("scenecraft.gui.rendering.timeleft") + ": " + secToString(renderTimeLeft);

        int framesDone = renderer.getFramesDone(), framesTotal = renderer.getTotalFrames();
        progressBar.setLabel(I18n.get("scenecraft.gui.rendering.progress", framesDone, framesTotal));
        progressBar.setProgress((float) framesDone / framesTotal);
    }

    private String secToString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(hours).append(I18n.get("scenecraft.gui.hours"));
        if(min > 0 || hours > 0) builder.append(min).append(I18n.get("scenecraft.gui.minutes"));
        builder.append(sec).append(I18n.get("scenecraft.gui.seconds"));

        return builder.toString();
    }

    private synchronized void renderPreview(GuiGraphics graphics, Size size, int ox, int oy) {
        Size videoSize = renderer.getFrameSize();
        final int videoWidth = videoSize.width();
        final int videoHeight = videoSize.height();

        if(previewTexture == null) {
            previewTexture = new DynamicTexture(videoWidth, videoHeight, true);
            previewLocation = MCVer.getMinecraft().getTextureManager()
                    .register("scenecraft_render_preview", previewTexture);
        }

        if(previewTextureDirty) {
            previewTexture.upload();
            previewTextureDirty = false;
        }

        renderTexture(graphics, previewLocation, size, ox, oy, videoWidth, videoHeight);
    }

    private ResourceLocation previewLocation;

    private void renderTexture(GuiGraphics graphics, ResourceLocation tex, Size size, int ox, int oy,
                               int videoWidth, int videoHeight) {
        Size dimension = Utils.fitIntoBounds(new Size(videoWidth, videoHeight), size);

        int w = dimension.width();
        int h = dimension.height();
        int x = ox + (size.width() - w) / 2;
        int y = oy + (size.height() - h) / 2;

        graphics.blit(tex, x, y, w, h, 0f, 0f, videoWidth, videoHeight, videoWidth, videoHeight);
    }

    public void updatePreview(ByteBuffer buffer, Size size) {
        if(preview && previewTexture != null) {
            buffer.mark();
            synchronized(this) {
                NativeImage data = previewTexture.getPixels();
                assert data != null;
                // Note: Optifine changes the texture data array to be three times as long (for use by shaders),
                //       we only want to initialize the first third and since we use our frame size, not the array size,
                //       we're good to go.
                int width = size.width();
                for(int y = 0; y < size.height(); y++) {
                    for(int x = 0; x < width; x++) {
                        int b = buffer.get() & 0xff;
                        int g = buffer.get() & 0xff;
                        int r = buffer.get() & 0xff;
                        buffer.get(); // alpha
                        int value = 0xff << 24 | b << 16 | g << 8 | r;
                        data.setPixelRGBA(x, y, value);
                    }
                }
                previewTextureDirty = true;
            }
            buffer.reset();
        }
    }
}
