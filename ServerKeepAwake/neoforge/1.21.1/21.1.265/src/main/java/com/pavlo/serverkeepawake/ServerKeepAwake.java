package com.pavlo.serverkeepawake;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod(ServerKeepAwake.MOD_ID)
public final class ServerKeepAwake {
    public static final String MOD_ID = "serverkeepawake";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int ES_CONTINUOUS = 0x80000000;
    private static final int ES_SYSTEM_REQUIRED = 0x00000001;
    private static final long REFRESH_INTERVAL_MS = 30_000L;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread keepAwakeThread;

    public ServerKeepAwake() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (!Platform.isWindows()) {
            LOGGER.info("ServerKeepAwake: non-Windows OS detected; sleep prevention is inactive.");
            return;
        }
        if (!running.compareAndSet(false, true)) return;
        Thread thread = new Thread(this::keepAwakeLoop, "ServerKeepAwake-WindowsPower");
        thread.setDaemon(true);
        keepAwakeThread = thread;
        thread.start();
        LOGGER.info("ServerKeepAwake: Windows sleep prevention enabled while the server is running.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        stopKeepAwake();
    }

    private void keepAwakeLoop() {
        Kernel32 kernel32 = null;
        try {
            kernel32 = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);
            while (running.get()) {
                int result = kernel32.SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED);
                if (result == 0) {
                    LOGGER.warn("ServerKeepAwake: Windows rejected the execution-state request.");
                    break;
                }
                try {
                    Thread.sleep(REFRESH_INTERVAL_MS);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        } catch (Throwable error) {
            LOGGER.error("ServerKeepAwake: could not control Windows sleep state. The server will continue normally.", error);
        } finally {
            if (kernel32 != null) {
                try {
                    kernel32.SetThreadExecutionState(ES_CONTINUOUS);
                } catch (Throwable error) {
                    LOGGER.warn("ServerKeepAwake: could not clear the Windows execution-state request.", error);
                }
            }
            running.set(false);
        }
    }

    private void stopKeepAwake() {
        if (!running.getAndSet(false)) return;
        Thread thread = keepAwakeThread;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(2_000L);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("ServerKeepAwake: Windows sleep prevention disabled; normal power settings restored.");
    }

    public interface Kernel32 extends StdCallLibrary {
        int SetThreadExecutionState(int executionState);
    }
}
