/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.voltdb.meshmonitor.serdes.IpPortSerializer;
import org.voltdb.meshmonitor.serdes.PacketSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MeshMonitor {

    private static final int PROGRAM_ERROR_RESULT = 1;
    private static final int PROGRAM_SUCCESS_RESULT = 0;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setUncaughtExceptionHandler((t, e) -> System.err.printf("[%s] %s", t.getName(), e.getMessage()));

                return thread;
            });

    private final ConsoleLogger consoleLogger;
    private final ServerManager serverManager;

    private final InetSocketAddress bindAddress;
    private final List<InetSocketAddress> permanentNodesToConnectTo;
    private final int reportIntervalSeconds;
    private final long minHiccupSizeMicroseconds;

    public MeshMonitor(
            ConsoleLogger consoleLogger,
            ServerManager serverManager,
            InetSocketAddress bindAddress,
            List<InetSocketAddress> permanentNodesToConnectTo,
            int reportIntervalSeconds,
            int minHiccupSizeMilliseconds) {
        this.consoleLogger = consoleLogger;
        this.serverManager = serverManager;
        this.bindAddress = bindAddress;
        this.permanentNodesToConnectTo = permanentNodesToConnectTo;
        this.reportIntervalSeconds = reportIntervalSeconds;
        this.minHiccupSizeMicroseconds = TimeUnit.MILLISECONDS.toMicros(minHiccupSizeMilliseconds);
    }

    public int start(boolean printStatistics) {
        consoleLogger.log("Starting meshmonitor. Binding to address " + bindAddress);
        ServerSocketChannel serverSocketChannel;

        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().setSoTimeout(10_000);
            serverSocketChannel.socket().bind(bindAddress);
        } catch (IOException e) {
            consoleLogger.fatalError("Error while opening server socket", e);
            return PROGRAM_ERROR_RESULT;
        }

        for (InetSocketAddress connectAddress : permanentNodesToConnectTo) {
            connectToWithReconnection(connectAddress);
        }

        if (printStatistics) {
            scheduleStatisticsPrinting();
        }

        try {
            while (serverSocketChannel.isOpen()) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                handleNewConnection(socketChannel);
            }
        } catch (IOException e) {
            consoleLogger.fatalError("Error while accepting remote connection", e);
            return PROGRAM_ERROR_RESULT;
        }

        return PROGRAM_SUCCESS_RESULT;
    }

    private void scheduleStatisticsPrinting() {
        SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                List<Monitor> monitors = serverManager.getMonitors();
                consoleLogger.log("Connected to %d servers", monitors.size());

                if (!monitors.isEmpty()) {
                    HistogramLogger printer = new HistogramLogger(consoleLogger);
                    printer.printHeader();
                    monitors.forEach(monitor -> printer.printResults(monitor, minHiccupSizeMicroseconds));
                }
            } catch (Exception e) {
                consoleLogger.log("Internal error. %s", e.getMessage());
            }
        }, reportIntervalSeconds, reportIntervalSeconds, TimeUnit.SECONDS);
    }

    public void connectToWithReconnection(InetSocketAddress connectAddress) {
        SCHEDULER.scheduleAtFixedRate(
                () -> connectToIfNotConnected(connectAddress),
                0,
                reportIntervalSeconds,
                TimeUnit.SECONDS);
    }

    private void connectTo(InetSocketAddress remoteId) {
        consoleLogger.warn(remoteId, "Connecting");
        try {
            SocketChannel channel = SocketChannel.open();
            channel.socket().setSoTimeout(10_000);
            channel.connect(remoteId);
            consoleLogger.log(remoteId, "Connected");
            PacketSerializer.writeHelloMessage(channel, bindAddress);
            consoleLogger.log(remoteId, "Handshake sent");
            if (!serverManager.createNewMonitorIfNotAlreadyPresent(channel, this, remoteId)) {
                channel.close();
                consoleLogger.debug(remoteId, "Connection already established");
            }
        } catch (IOException e) {
            consoleLogger.error(remoteId, "Unable to connect to remote address. %s. Retrying", e.getMessage());
        }
    }

    public void connectToIfNotConnected(InetSocketAddress remoteId) {
        if (bindAddress.equals(remoteId) || serverManager.hasConnection(remoteId)) {
            return;
        }

        consoleLogger.log(remoteId, "New remote endpoint - establishing connection");
        connectTo(remoteId);
    }

    public void handleNewConnection(SocketChannel socketChannel) throws IOException {
        consoleLogger.log(socketChannel.getRemoteAddress(), "Received connection");

        ByteBuffer buffer = PacketSerializer.readPacketFully(socketChannel);
        InetSocketAddress remoteAddress = IpPortSerializer.deserializeSingleIp(buffer);

        if (!serverManager.createNewMonitorIfNotAlreadyPresent(socketChannel, this, remoteAddress)) {
            consoleLogger.log(socketChannel.getRemoteAddress(), "Closing connection - node already connected");
            socketChannel.close();
        }
    }

    public void onNewNodeInMesh(InetSocketAddress origin, List<InetSocketAddress> newIpList) {
        consoleLogger.debug(origin, "Received IP list: %s", newIpList);
        newIpList.forEach(this::connectToIfNotConnected);
    }

    public List<InetSocketAddress> getConnections() {
        return serverManager.getConnections();
    }

    public void onDisconnect(InetSocketAddress remoteId, IOException e) {
        if (serverManager.hasConnection(remoteId)) {
            consoleLogger.log(remoteId, "Disconnected duplicated connection");
        } else {
            consoleLogger.error(remoteId, e.getMessage());
        }

        serverManager.removeConnection(remoteId);
    }
}
