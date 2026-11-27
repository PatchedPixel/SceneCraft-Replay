package net.patchedpixel.scenecraft.common.render.rendering;

import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.render.capturer.WorldRenderer;
import net.patchedpixel.scenecraft.common.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.common.render.processor.AbsoluteDepthProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.ReportedException;
import net.minecraft.CrashReport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getMinecraft;

public class Pipeline<R extends Frame, P extends Frame> implements Runnable {
    private final WorldRenderer worldRenderer;
    private final FrameCapturer<R> capturer;
    private final FrameProcessor<R, P> processor;
    private final AbsoluteDepthProcessor depthProcessor;
    private final FrameConsumer<P> consumer;

    private volatile boolean abort;

    public Pipeline(WorldRenderer worldRenderer, FrameCapturer<R> capturer, FrameProcessor<R, P> processor, FrameConsumer<P> consumer) {
        this.worldRenderer = worldRenderer;
        this.capturer = capturer;
        this.processor = processor;
        this.consumer = new ParallelSafeConsumer<>(consumer);

        float near = 0.05f;
        float far = getMinecraft().options.renderDistance().get() * 16 * 4;
        this.depthProcessor = new AbsoluteDepthProcessor(near, far);
    }

    @Override
    public synchronized void run() {
        int processors = Runtime.getRuntime().availableProcessors();
        int processThreads = Math.max(1, processors - 2); // One processor for the main thread and one for ffmpeg, sorry OS :(
        ExecutorService processService = new ThreadPoolExecutor(processThreads, processThreads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(2) {
                    @Override
                    public boolean offer(Runnable runnable) {
                        try {
                            put(runnable);
                        } catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        return true;
                    }
                }, new ThreadPoolExecutor.DiscardPolicy());

        Minecraft mc = MCVer.getMinecraft();
        Throwable failure = null;
        try {
            while(!capturer.isDone() && !abort) {
                if(mc.getWindow().shouldClose() || mc.delayedCrash != null) {
                    break;
                }
                Map<Channel, R> rawFrame = capturer.process();
                if(rawFrame != null) {
                    processService.submit(new ProcessTask(rawFrame));
                }
            }
        } catch(Throwable t) {
            failure = t;
        }

        // After a failure or an early exit, queued frames may never become consumable
        // (a failed frame blocks every later one), so do not wait for them indefinitely
        boolean stoppedEarly = failure != null
                || abort
                || mc.delayedCrash != null
                || mc.getWindow().shouldClose();
        if(stoppedEarly) {
            processService.shutdownNow();
        } else {
            processService.shutdown();
        }
        try {
            if(stoppedEarly) {
                processService.awaitTermination(10, TimeUnit.SECONDS);
            } else {
                processService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            worldRenderer.close();
            capturer.close();
            processor.close();
            consumer.close();
        } catch(Throwable t) {
            // Do not hide the original failure
            if(failure == null) {
                failure = t;
            } else {
                failure.addSuppressed(t);
            }
        }

        if(failure != null) {
            if(failure instanceof RuntimeException e) throw e;
            if(failure instanceof Error e) throw e;
            throw new ReportedException(CrashReport.forThrowable(failure, "Rendering pipeline"));
        }
    }

    public void cancel() {
        abort = true;
    }

    private class ProcessTask implements Runnable {
        private final Map<Channel, R> rawChannels;

        public ProcessTask(Map<Channel, R> rawChannels) {
            this.rawChannels = rawChannels;
        }

        @Override
        public void run() {
            try {
                Map<Channel, P> processedChannels = new HashMap<>();
                for(Map.Entry<Channel, R> entry : rawChannels.entrySet()) {
                    P processedFrame = processor.process(entry.getValue());
                    if(entry.getKey() == Channel.DEPTH && processedFrame instanceof BitmapFrame) {
                        depthProcessor.process((BitmapFrame) processedFrame);
                    }
                    processedChannels.put(entry.getKey(), processedFrame);
                }
                if(processedChannels.isEmpty()) {
                    return;
                }
                consumer.consume(processedChannels);
            } catch(Throwable t) {
                if(abort || Thread.currentThread().isInterrupted()) {
                    // The pipeline is shutting down, errors from interrupted or aborted frames are expected
                    return;
                }
                CrashReport crashReport = CrashReport.forThrowable(t, "Processing frame");
                MCVer.getMinecraft().delayCrash(crashReport);
            }
        }
    }

    private static class ParallelSafeConsumer<P extends Frame> implements FrameConsumer<P> {
        private final FrameConsumer<P> inner;

        private int nextFrame;
        private final Object lock = new Object();

        private ParallelSafeConsumer(FrameConsumer<P> inner) {
            this.inner = inner;
        }

        @Override
        public void consume(Map<Channel, P> channels) {
            if(inner.isParallelCapable()) {
                inner.consume(channels);
            } else {
                int frameId = channels.values().iterator().next().getFrameId();
                synchronized(lock) {
                    while(nextFrame != frameId) {
                        try {
                            lock.wait();
                        } catch(InterruptedException e) {
                            // Shutting down: give up on this frame instead of waiting for earlier ones
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    inner.consume(channels);
                    nextFrame++;
                    lock.notifyAll();
                }
            }
        }

        @Override
        public boolean isParallelCapable() {
            return true;
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }
    }
}
