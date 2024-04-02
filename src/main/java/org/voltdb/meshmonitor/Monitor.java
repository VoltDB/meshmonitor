/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.voltdb.meshmonitor.serdes.PacketSerializer;

public class Monitor {

    private final Clock systemClock = Clock.systemUTC();
    private final ConsoleLogger logger;
    private final MeshMonitor meshMonitor;

    private final InetSocketAddress remoteId;
    private final SocketChannel channel;

    private final MeshMonitorTimings timings;
    private final Duration pingInterval;

    private volatile boolean isRunning;
    private ReceiveThread receiveThread;

    public Monitor(ConsoleLogger logger,
                   MeshMonitor meshMonitor,
                   MeshMonitorTimings timings,
                   Duration pingInterval,
                   SocketChannel channel,
                   InetSocketAddress remoteId) {
        this.logger = logger;
        this.meshMonitor = meshMonitor;
        this.timings = timings;
        this.pingInterval = pingInterval;
        this.channel = channel;
        this.remoteId = remoteId;
    }

    public void start() {
        isRunning = true;

        SendThread sendThread = new SendThread(channel);
        sendThread.start();
        receiveThread = new ReceiveThread(channel);
        receiveThread.start();
    }

    public MeshMonitorTimings getTimings() {
        return timings;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public InetSocketAddress getRemoteId() {
        return remoteId;
    }

    private class ReceiveThread extends Thread {
        public ReceiveThread(SocketChannel sc) {
            super(sc.socket().getRemoteSocketAddress() + " receive thread");
        }

        @Override
        public void run() {
            try {
                long lastRecvTime = currentTimeMicroseconds();
                while (isRunning) {
                    long timestampFromRemoteHost = receiveTimestamp();
                    long now = currentTimeMicroseconds();

                    logger.debug(remoteId, "Received ping, timings: %d", now - lastRecvTime);
                    timings.pingReceived(now, lastRecvTime, timestampFromRemoteHost, TimeUnit.NANOSECONDS.toMicros(pingInterval.toNanos()));
                    lastRecvTime = now;
                }
                System.out.println("ReceiveThread Exiting");
            } catch (IOException e) {
                System.out.println("ReceiveThread error");
                e.printStackTrace();
                isRunning = false;
                meshMonitor.onDisconnect(remoteId, e);
            }
        }

        private long receiveTimestamp() throws IOException {
            return PacketSerializer.receiveTimestamp(channel, list -> meshMonitor.onNewNodeInMesh(remoteId, list));
        }
    }

    private class SendThread extends Thread {
        public SendThread(SocketChannel sc) {
            super(sc.socket().getRemoteSocketAddress() + " send thread");
        }

        @Override
        public void run() {
            long lastRunTime = currentTimeMicroseconds();
            try {
                while (isRunning) {
                    sleepUninterruptibly();

                    long now = currentTimeMicroseconds();
                    sendPing(now);

                    timings.trackWakeupJitter(now - lastRunTime, TimeUnit.NANOSECONDS.toMicros(pingInterval.toNanos()));
                    lastRunTime = now;
                }
                System.out.println("SendThread Exiting");
            } catch (IOException e) {
                System.out.println("SendThread error");
                e.printStackTrace();
                isRunning = false;
                receiveThread.interrupt();
                meshMonitor.onDisconnect(remoteId, e);
            }
        }

        private void sendPing(long now) throws IOException {
            List<InetSocketAddress> connectedServers = meshMonitor.getConnections();
            logger.debug(remoteId, "Sending IP list: %s", connectedServers);
            PacketSerializer.sendPing(channel, now, connectedServers);
        }

        private void sleepUninterruptibly() {
            try {
                Thread.sleep(pingInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private long currentTimeMicroseconds() {
        Instant instant = systemClock.instant();
        long seconds = TimeUnit.SECONDS.toNanos(instant.getEpochSecond());

        return TimeUnit.NANOSECONDS.toMicros(seconds + instant.getNano());
    }
}
