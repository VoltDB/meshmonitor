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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ServerManager {

    private final List<Monitor> monitors = new ArrayList<>();
    private final ConsoleLogger consoleLogger;
    private final Duration pingInterval;

    public ServerManager(ConsoleLogger consoleLogger, Duration pingInterval) {
        this.consoleLogger = consoleLogger;
        this.pingInterval = pingInterval;
    }

    public synchronized List<Monitor> getMonitors() {
        return monitors.stream()
                .filter(Monitor::isRunning)
                .toList();
    }

    public synchronized List<InetSocketAddress> getConnections() {
        return monitors.stream()
                .filter(Monitor::isRunning)
                .map(Monitor::getRemoteId)
                .toList();
    }

    public synchronized boolean createNewMonitorIfNotAlreadyPresent(SocketChannel channel, MeshMonitor meshMonitor, InetSocketAddress remoteId) throws IOException {
        if (!hasConnection(remoteId)) {
            MeshMonitorTimings timings = MeshMonitorTimings.createDefault(consoleLogger);

            Monitor monitor = new Monitor(consoleLogger, meshMonitor, timings, pingInterval, channel, remoteId);
            monitors.add(monitor);
            return true;
        }

        return false;
    }

    public synchronized boolean hasConnection(InetSocketAddress remoteId) {
        return monitors.stream()
                .filter(Monitor::isRunning)
                .map(Monitor::getRemoteId)
                .anyMatch(socketAddress -> socketAddress.equals(remoteId));
    }
}
