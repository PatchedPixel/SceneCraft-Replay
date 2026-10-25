package net.patchedpixel.scenecraft.render;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import de.javagl.jgltf.impl.v2.Accessor;
import de.javagl.jgltf.impl.v2.Animation;
import de.javagl.jgltf.impl.v2.AnimationChannel;
import de.javagl.jgltf.impl.v2.AnimationChannelTarget;
import de.javagl.jgltf.impl.v2.AnimationSampler;
import de.javagl.jgltf.impl.v2.Asset;
import de.javagl.jgltf.impl.v2.Buffer;
import de.javagl.jgltf.impl.v2.BufferView;
import de.javagl.jgltf.impl.v2.Camera;
import de.javagl.jgltf.impl.v2.CameraPerspective;
import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.impl.v2.Node;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetWriterV2;
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.apache.commons.io.FilenameUtils;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import static net.patchedpixel.scenecraft.platform.Utils.configure;

public class CameraPathExporter {
    /** OpenGL's GL_FLOAT, which is what glTF uses as component type for 32 bit floats. */
    private static final int GL_FLOAT = 0x1406;

    private final Minecraft mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private int framesDone;
    private ByteBuffer timeBuffer;
    private ByteBuffer cameraTranslationBuffer;
    private ByteBuffer cameraRotationBuffer;

    public CameraPathExporter(RenderSettings settings) {
        this.settings = settings;
    }

    public void setup(int totalFrames) {
        timeBuffer = ByteBuffer.allocate(4 * totalFrames).order(ByteOrder.LITTLE_ENDIAN);
        cameraTranslationBuffer = ByteBuffer.allocate(4 * totalFrames * 3).order(ByteOrder.LITTLE_ENDIAN);
        cameraRotationBuffer = ByteBuffer.allocate(4 * totalFrames * 4).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void recordFrame() {
        Entity entity = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();

        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 vec = camera.getPosition();
        float x = (float) vec.x();
        float y = (float) vec.y();
        float z = (float) vec.z();
        float yaw = camera.getYRot() + 180;
        float pitch = camera.getXRot();
        float roll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;

        Quaternionf quatYaw = new Quaternionf().rotationAxis((float) Math.toRadians(yaw), 0, -1, 0);
        Quaternionf quatPitch = new Quaternionf().rotationAxis((float) Math.toRadians(pitch), -1, 0, 0);
        Quaternionf quatRoll = new Quaternionf().rotationAxis((float) Math.toRadians(roll), 0, 0, -1);

        Quaternionf quaternion = new Quaternionf()
                .mul(quatYaw)
                .mul(quatPitch)
                .mul(quatRoll)
                .normalize();

        float[] translation = new float[] { x, y, z };
        float[] rotation = new float[] { quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w() };

        timeBuffer.putFloat(framesDone / (float) settings.getFramesPerSecond());
        for (float f : translation) {
            cameraTranslationBuffer.putFloat(f);
        }
        for (float f : rotation) {
            cameraRotationBuffer.putFloat(f);
        }

        framesDone++;
    }

    public void finish() throws IOException {
        int timeBufferSize = timeBuffer.rewind().remaining();
        int cameraTranslationBufferSize = cameraTranslationBuffer.rewind().remaining();
        int cameraRotationBufferSize = cameraRotationBuffer.rewind().remaining();

        int binaryDataSize = 0;
        binaryDataSize += timeBufferSize;
        binaryDataSize += cameraTranslationBufferSize;
        binaryDataSize += cameraRotationBufferSize;

        ByteBuffer binaryData = ByteBuffer.allocate(binaryDataSize);
        int timeBufferOffset = binaryData.position();
        binaryData.put(timeBuffer);
        int cameraTranslationBufferOffset = binaryData.position();
        binaryData.put(cameraTranslationBuffer);
        int cameraRotationBufferOffset = binaryData.position();
        binaryData.put(cameraRotationBuffer);
        binaryData.rewind();

        GlTF glTF = new GlTF();
        glTF.setAsset(configure(new Asset(), asset -> {
            asset.setVersion("2.0");
            asset.setGenerator("SceneCraft v" + SceneCraft.instance.getVersion());
        }));
        glTF.addAnimations(configure(new Animation(), animation -> {
            animation.addChannels(configure(new AnimationChannel(), channel -> {
                channel.setTarget(configure(new AnimationChannelTarget(), target -> {
                    target.setNode(0);
                    target.setPath("translation");
                }));
                channel.setSampler(0);
            }));
            animation.addChannels(configure(new AnimationChannel(), channel -> {
                channel.setTarget(configure(new AnimationChannelTarget(), target -> {
                    target.setNode(0);
                    target.setPath("rotation");
                }));
                channel.setSampler(1);
            }));
            animation.addSamplers(configure(new AnimationSampler(), sampler -> {
                sampler.setInput(0);
                sampler.setOutput(1);
            }));
            animation.addSamplers(configure(new AnimationSampler(), sampler -> {
                sampler.setInput(0);
                sampler.setOutput(2);
            }));
        }));
        glTF.addCameras(configure(new Camera(), camera -> {
            camera.setType("perspective");
            camera.setPerspective(configure(new CameraPerspective(), perspective -> {
                float aspectRatio = (float) settings.getVideoWidth() / (float) settings.getVideoHeight();
                perspective.setAspectRatio(aspectRatio);
                perspective.setYfov((float) Math.toRadians(mc.options.fov().get()));
                perspective.setZnear(0.05f);
                perspective.setZfar((float) mc.options.renderDistance().get() * 16 * 4);
            }));
        }));
        glTF.addNodes(configure(new Node(), node -> node.setCamera(0)));
        glTF.addBuffers(configure(new Buffer(), buffer -> buffer.setByteLength(binaryData.limit())));
        // Time
        glTF.addBufferViews(configure(new BufferView(), bufferView -> {
            bufferView.setBuffer(0);
            bufferView.setByteOffset(timeBufferOffset);
            bufferView.setByteLength(timeBufferSize);
        }));
        glTF.addAccessors(configure(new Accessor(), accessor -> {
            accessor.setBufferView(0);
            accessor.setType("SCALAR");
            accessor.setComponentType(GL_FLOAT);
            accessor.setCount(framesDone);
        }));
        // Camera translation
        glTF.addBufferViews(configure(new BufferView(), bufferView -> {
            bufferView.setBuffer(0);
            bufferView.setByteOffset(cameraTranslationBufferOffset);
            bufferView.setByteLength(cameraTranslationBufferSize);
        }));
        glTF.addAccessors(configure(new Accessor(), accessor -> {
            accessor.setBufferView(1);
            accessor.setType("VEC3");
            accessor.setComponentType(GL_FLOAT);
            accessor.setCount(framesDone);
        }));
        // Camera rotation
        glTF.addBufferViews(configure(new BufferView(), bufferView -> {
            bufferView.setBuffer(0);
            bufferView.setByteOffset(cameraRotationBufferOffset);
            bufferView.setByteLength(cameraRotationBufferSize);
        }));
        glTF.addAccessors(configure(new Accessor(), accessor -> {
            accessor.setBufferView(2);
            accessor.setType("VEC4");
            accessor.setComponentType(GL_FLOAT);
            accessor.setCount(framesDone);
        }));

        java.nio.file.Path videoPath = settings.getOutputFile().toPath();
        java.nio.file.Path glbBasePath = Files.isDirectory(videoPath)
                ? videoPath.resolve("camera.glb")
                : videoPath.resolveSibling(FilenameUtils.getBaseName(videoPath.getFileName().toString()) + ".glb");
        java.nio.file.Path glbPath = glbBasePath;
        for (int i = 0; Files.exists(glbPath); i++) {
            String baseName = FilenameUtils.getBaseName(glbBasePath.getFileName().toString());
            glbPath = glbBasePath.resolveSibling(baseName + "." + i + ".glb");
        }
        try (OutputStream out = Files.newOutputStream(glbPath)) {
            GltfAssetV2 asset = new GltfAssetV2(glTF, binaryData);
            new GltfAssetWriterV2().writeBinary(asset, out);
        }
    }
}
