package net.patchedpixel.scenecraft.core.versions.scheduler;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.client.Minecraft;
import net.minecraft.ReportedException;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class SchedulerImpl implements Scheduler {
    private static final Minecraft mc = MCVer.getMinecraft();

    @Override
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        if (mc.isSameThread()) {
            runnable.run();
        } else {
            executor.submit(() -> {
                runnable.run();
                return null;
            }).get(30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void runPostStartup(Runnable runnable) {
        runLater(new Runnable() {
            @Override
            public void run() {
                if (mc.getOverlay() != null) {
                    // delay until after resources have been loaded
                    runLater(this);
                    return;
                }
                runnable.run();
            }
        });
    }

    /**
     * Set when the currently running code has been scheduled by runLater.
     * If this is the case, subsequent calls to runLater have to be delayed until all scheduled tasks have been
     * processed, otherwise a livelock may occur.
     */
    private boolean inRunLater = false;
    // Starting 1.14 MC clears the queue of scheduled tasks on disconnect.
    // This works fine for MC since it uses the queue only for packet handling but breaks our assumption that
    // stuff submitted via runLater is actually always run (e.g. record might not be fully stopped because parts
    // of that are run via runLater and stopping the record happens right around the time MC clears the queue).
    // Luckily, that's also the version where MC pulled out the executor implementation, so we can just spin up our own.
    public static class SceneCraftExecutor extends ReentrantBlockableEventLoop<Runnable> {
        private final Thread mcThread = Thread.currentThread();

        private SceneCraftExecutor(String string_1) {
            super(string_1);
        }

        @Override public Runnable wrapRunnable(Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean shouldRun(Runnable runnable) {
            return true;
        }

        @Override
        protected Thread getRunningThread() {
            return mcThread;
        }

        @Override
        public void runAllTasks() {
            super.runAllTasks();
        }
    }
    public final SceneCraftExecutor executor = new SceneCraftExecutor("Client/SceneCraft");
    private final List<Runnable> delayedTasks = new ArrayList<>();

    @Override
    public void runTasks() {
        executor.runAllTasks();
        delayedTasks.forEach(executor::tell);
        delayedTasks.clear();
    }

    @Override
    public void runLaterWithoutLock(Runnable runnable) {
        // MC 1.14+ no longer synchronizes on the queue while running its tasks
        runLater(runnable);
    }

    @Override
    public void runLater(Runnable runnable) {
        runLater(runnable, () -> runLater(runnable));
    }

    private void runLater(Runnable runnable, Runnable defer) {
        if (mc.isSameThread() && inRunLater) {
            delayedTasks.add(defer);
        } else {
            executor.tell(() -> {
                inRunLater = true;
                try {
                    runnable.run();
                } catch (ReportedException e) {
                    e.printStackTrace();
                    System.err.println(e.getReport().getFriendlyReport());
                    mc.delayCrash(e.getReport());
                } finally {
                    inRunLater = false;
                }
            });
        }
    }
}
