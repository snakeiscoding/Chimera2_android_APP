package com.jht.chimera.service.hal.lib.io.iocommlib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.serialport.ISerialPort;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Tests for the {@code notifyRxPacketSync} ({@link JHTNotification}) handshake between
 * {@link BasePacketManager}'s TxThread and RxThread.
 *
 * <p>Group A pins down the {@link JHTNotification} contract that the IO comm layer relies on:
 * the {@code notify} flag is sticky, so a stale notification left by a previous command makes
 * {@code waitForNotification} return immediately. That is exactly the bug that caused WRITE_PAGE
 * packets to be resent during an IO firmware update, and the reason TxThread now calls
 * {@code notifyRxPacketSync.reset()} when it dequeues a new command.
 *
 * <p>Group B drives a real (isolated) {@link McuPacketManager} over a {@link TestSerialPort} and
 * verifies the fixed behaviour: a reply wakes the wait with a single send, a stale notification
 * does not cause a premature duplicate send, and an unanswered command retries three times before
 * being skipped.
 *
 * <p>This test lives in the {@code iocommlib} package so it can reach the protected
 * {@code txPacketQueue} / {@code notifyRxPacketSync} fields and the protected
 * {@link McuPacketManager} constructor (a fresh instance, not the device singleton).
 *
 * <p>The serial port is a {@link TestSerialPort} (not the shared MockSerialPort) whose input stream
 * throws {@link IOException} when the RxThread is interrupted or the port is closed. That routes
 * the RxThread's teardown through its clean {@code catch (IOException) { break; }} path, so it never
 * calls {@code restartRxThread()} on a torn-down port - no production code change is required.
 */
public class NotifyRxPacketSyncTest {

    /** Loop pacing of TxThread is 50ms; windows below are comfortably larger. */
    private static final int LOOP_MS = 50;

    private Context context;
    private TestSerialPort port;
    private McuPacketManager mgr;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        port = new TestSerialPort();
        mgr = new McuPacketManager();
        mgr.setCallback(NO_OP_CALLBACK);
    }

    @After
    public void tearDown() {
        if (mgr != null) {
            mgr.stop(); // interrupts/closes the RX read, which exits cleanly via IOException
        }
        if (port != null && port.isOpen()) {
            port.close();
        }
    }

    // ----------------------------------------------------------------------------------------
    // Group A - JHTNotification contract (deterministic, no threads under test)
    // ----------------------------------------------------------------------------------------

    /** A stale notification (set before the wait) makes waitForNotification return immediately. */
    @Test @Emulator @Default @Chimera
    public void staleNotification_returnsImmediately() {
        JHTNotification notification = new JHTNotification();
        notification.setNotification();

        long start = System.currentTimeMillis();
        boolean got = notification.waitForNotification(2000);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("a pre-set notification should satisfy the wait", got);
        assertTrue("wait should return ~immediately, took " + elapsed + "ms", elapsed < 500);
    }

    /** With no notification pending the wait blocks for the timeout and reports failure. */
    @Test @Emulator @Default @Chimera
    public void noNotification_blocksUntilTimeout() {
        JHTNotification notification = new JHTNotification();

        long start = System.currentTimeMillis();
        boolean got = notification.waitForNotification(200);
        long elapsed = System.currentTimeMillis() - start;

        assertFalse("no notification was sent", got);
        assertTrue("wait should block close to the timeout, took " + elapsed + "ms", elapsed >= 150);
    }

    /** reset() clears a pending notification - the behaviour the TxThread fix depends on. */
    @Test @Emulator @Default @Chimera
    public void reset_clearsPendingNotification() {
        JHTNotification notification = new JHTNotification();
        notification.setNotification();
        notification.reset();

        long start = System.currentTimeMillis();
        boolean got = notification.waitForNotification(200);
        long elapsed = System.currentTimeMillis() - start;

        assertFalse("reset() should have cleared the stale notification", got);
        assertTrue("wait should block after reset, took " + elapsed + "ms", elapsed >= 150);
    }

    /** A notification arriving while waiting wakes the waiter early. */
    @Test @Emulator @Default @Chimera
    public void notificationWhileWaiting_wakesEarly() throws InterruptedException {
        JHTNotification notification = new JHTNotification();
        Thread notifier = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            notification.setNotification();
        });

        long start = System.currentTimeMillis();
        notifier.start();
        boolean got = notification.waitForNotification(2000);
        long elapsed = System.currentTimeMillis() - start;
        notifier.join();

        assertTrue("the wait should have been woken by setNotification", got);
        assertTrue("should wake well before the timeout, took " + elapsed + "ms", elapsed < 1500);
    }

    // ----------------------------------------------------------------------------------------
    // Group B - McuPacketManager driven over a TestSerialPort
    // ----------------------------------------------------------------------------------------

    /** A matching reply releases the wait and the command is sent exactly once. */
    @Test @Emulator @Default @Chimera
    public void reply_releasesWait_singleSend() throws Exception {
        startManager();

        mgr.sendPacket(new McuPacket(McuProtocol.McuCommands.VERSION));
        // Once TxThread has transmitted the command it parks in waitForNotification(timeout).
        waitUntil(() -> port.sentCount() >= 1, 2000);
        port.injectReply(new McuPacket(McuProtocol.McuCommands.VERSION, (byte) 0xA, (byte) 0x5));

        // The reply wakes the wait and finishTxCommand() drains the in-flight packet.
        waitUntil(() -> mgr.txPacketQueue.isEmpty(), 2000);

        assertEquals("command should be sent exactly once", 1, port.sentCount());
        assertTrue("in-flight packet should be cleared after the reply", mgr.txPacketQueue.isEmpty());
    }

    /**
     * Regression for the stale-notification fix: a leftover notification must NOT make the next
     * command's wait return immediately and resend. With {@code notifyRxPacketSync.reset()} on
     * dequeue, the command is sent once and the thread blocks on its own reply; without the fix a
     * duplicate would be sent within ~one loop interval.
     */
    @Test @Emulator @Default @Chimera
    public void staleNotification_doesNotCauseDuplicateSend() throws Exception {
        startManager();

        // Simulate a stale notification left behind by a previous (e.g. duplicate/late) reply.
        // A long per-packet timeout (2000ms) keeps the observation window below well under the
        // timeout, so a legitimate timeout-retry can never be mistaken for a duplicate send.
        mgr.notifyRxPacketSync.setNotification();
        mgr.sendPacket(new McuPacket(McuProtocol.McuCommands.VERSION, 2000));

        // Wait until the command has been transmitted once (event-driven, not a fixed delay)...
        waitUntil(() -> port.sentCount() >= 1, 2000);
        // ...then observe across several loop intervals (>> 50ms loop, << 2000ms timeout). With the
        // reset() fix the thread stays parked in waitForNotification, so no duplicate is sent.
        Thread.sleep(6L * LOOP_MS);
        assertEquals("stale notification must not trigger a premature resend", 1, port.sentCount());

        // Complete the command cleanly so the thread is not left parked for the full timeout.
        port.injectReply(new McuPacket(McuProtocol.McuCommands.VERSION, (byte) 0xA, (byte) 0x5));
        waitUntil(() -> mgr.txPacketQueue.isEmpty(), 2000);
        assertTrue("reply should clear the in-flight packet", mgr.txPacketQueue.isEmpty());
        assertEquals("still only one transmission after completion", 1, port.sentCount());
    }

    /** With no reply, the command is retried 3 times then skipped (in-flight packet cleared). */
    @Test @Emulator @Default @Chimera
    public void noReply_retriesThreeTimesThenSkips() throws Exception {
        startManager();

        // 100ms per-packet timeout keeps the 3 retries fast (~300ms total).
        mgr.sendPacket(new McuPacket(McuProtocol.McuCommands.VERSION, 100));

        // Event-driven, not time-based: the combined condition also avoids the startup race where
        // txPacketQueue is momentarily empty before the first dequeue. The send count is a fixed 3
        // (driven by txRetryCounter < 3), so this is deterministic, not a timing measurement.
        waitUntil(() -> port.sentCount() >= 3 && mgr.txPacketQueue.isEmpty(), 3000);

        assertEquals("command should be transmitted exactly 3 times before giving up",
                3, port.sentCount());
        assertTrue("skipCurrentCommand() should clear the in-flight packet",
                mgr.txPacketQueue.isEmpty());
    }

    // ----------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------

    private void startManager() {
        assertTrue("manager should start", mgr.start(port, context));
    }

    private static void waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
    }

    /**
     * A minimal {@link ISerialPort} test double.
     *
     * <ul>
     *   <li>RX: bytes injected via {@link #injectReply} are read by the manager's RxThread. The
     *       input stream throws {@link IOException} when interrupted or closed, so the RxThread
     *       exits via its clean IOException path during teardown (no restartRxThread, no NPE).</li>
     *   <li>TX: bytes written by the manager's TxThread are parsed on the fly; {@link #sentCount}
     *       returns the number of complete packets transmitted (robust to write coalescing).</li>
     * </ul>
     */
    private static final class TestSerialPort implements ISerialPort {
        private final LinkedBlockingQueue<Integer> rxQueue = new LinkedBlockingQueue<>();
        private final McuPacketParser txParser = new McuPacketParser();
        private final AtomicInteger sentCount = new AtomicInteger(0);
        private volatile boolean open = false;

        @Override
        public void open() throws SecurityException {
            open = true;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false; // unblocks the input stream's read() which then throws IOException
        }

        @Override
        public int serialBreak(boolean enable) {
            return 0;
        }

        @Override
        public int flushInput() {
            rxQueue.clear();
            return 0;
        }

        @Override
        public int drainOutput() {
            return 0;
        }

        @Override
        public void setTraceDebug(boolean debug) {
            // no-op
        }

        @Override
        public InputStream getInputStream() {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    byte[] one = new byte[1];
                    int n = read(one, 0, 1);
                    return n <= 0 ? -1 : (one[0] & 0xFF);
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    Integer first;
                    try {
                        first = rxQueue.poll(50, TimeUnit.MILLISECONDS);
                        while (first == null) {
                            if (!open) {
                                throw new IOException("serial port closed");
                            }
                            first = rxQueue.poll(50, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException("read interrupted");
                    }
                    b[off] = (byte) (int) first;
                    int n = 1;
                    Integer next;
                    while (n < len && (next = rxQueue.poll()) != null) {
                        b[off + n++] = (byte) (int) next;
                    }
                    return n;
                }
            };
        }

        @Override
        public OutputStream getOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int b) {
                    countByte((byte) b);
                }

                @Override
                public void write(byte[] buf) {
                    for (byte b : buf) {
                        countByte(b);
                    }
                }

                @Override
                public void write(byte[] buf, int off, int len) {
                    for (int i = 0; i < len; i++) {
                        countByte(buf[off + i]);
                    }
                }
            };
        }

        private void countByte(byte b) {
            McuPacketParser.ParseResult result = txParser.feedByte(b);
            if (result == McuPacketParser.ParseResult.Success) {
                sentCount.incrementAndGet();
            } else if (McuPacketParser.isInvalid(result)) {
                txParser.reset();
            }
        }

        /** Number of complete packets the manager has transmitted. */
        int sentCount() {
            return sentCount.get();
        }

        /** Feeds a reply packet to the manager's RxThread as if the MCU had sent it. */
        void injectReply(McuPacket reply) {
            for (byte b : reply.getBytes()) {
                rxQueue.offer(b & 0xFF);
            }
        }
    }

    private static final IMCUCallback NO_OP_CALLBACK = new IMCUCallback() {
        @Override public void operationMode(int mode) { }
        @Override public void version(int version) { }
        @Override public void safeKeyStatus(boolean status) { }
        @Override public void powerState(byte source, boolean _switch) { }
        @Override public void externalPower(McuPacketManager.PowerVoltage powerVoltage, int current) { }
        @Override public void boot(boolean state) { }
        @Override public void fanSpeed(byte level, int rpm) { }
        @Override public void lcbType(byte type) { }
        @Override public void consolePowerVoltage(short voltage) { }
        @Override public void extendPowerVoltage(short voltage, boolean check) { }
        @Override public void usbVoltage(short voltage) { }
        @Override public void powerMonitorState(boolean enable) { }
        @Override public void consolePowerCurrent(short current) { }
        @Override public void extendPowerCurrent(short current) { }
        @Override public void extendPowerError(boolean error) { }
        @Override public void keyEvent(int event, short keyCode) { }
        @Override public void safeKeyEvent(boolean status) { }
        @Override public void debugMessage(String message) { }
        @Override public void cecCommand(int result) { }
        @Override public void getCECresponse(byte[] response) { }
        @Override public void irCommand(boolean result) { }
        @Override public void heartRateEvent(int heartRate) { }
        @Override public void canRx(byte[] data) { }
    };
}
