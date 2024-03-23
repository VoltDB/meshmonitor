package org.voltdb.meshmonitor;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.voltdb.meshmonitor.ConsoleLoggerTest.loggerForTest;
import static org.voltdb.meshmonitor.MeshMonitorTimings.createDefault;

class FakeMonitor extends Monitor {

    private static final Duration PING_INTERVAL = Duration.ofSeconds(5);

    private final boolean isRunning;

    public static FakeMonitor random() {
        int lastOctet = ThreadLocalRandom.current().nextInt(255);
        int port = ThreadLocalRandom.current().nextInt() & 0xFFF;

        return new FakeMonitor(new InetSocketAddress("127.0.0" + lastOctet, port));
    }

    public FakeMonitor(InetSocketAddress remoteId) {
        super(loggerForTest(), null, createDefault(loggerForTest()), PING_INTERVAL, null, remoteId);
        isRunning = true;
    }

    public FakeMonitor(InetSocketAddress remoteId, boolean isRunning) {
        super(loggerForTest(), null, createDefault(loggerForTest()), PING_INTERVAL, null, remoteId);
        this.isRunning = isRunning;
    }

    @Override
    public void start() {
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
