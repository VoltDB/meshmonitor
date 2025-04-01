/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerManager {

    private final ConsoleLogger consoleLogger;
    private final MonitorFactory monitorFactory;
    private final Duration pingInterval;

    private final List<Monitor> monitors = new ArrayList<>();

    public ServerManager(ConsoleLogger consoleLogger, MonitorFactory monitorFactory, Duration pingInterval) {
        this.consoleLogger = consoleLogger;
        this.monitorFactory = monitorFactory;
        this.pingInterval = pingInterval;
    }

    public synchronized List<Monitor> getMonitors() {
        return monitors.stream()
                .filter(Monitor::isRunning)
                .collect(Collectors.toList());
    }

    public synchronized List<InetSocketAddress> getConnections() {
        return monitors.stream()
                .filter(Monitor::isRunning)
                .map(Monitor::getRemoteId)
                .collect(Collectors.toList());
    }

    public synchronized boolean createNewMonitorIfNotAlreadyPresent(SocketChannel channel, MeshMonitor meshMonitor, InetSocketAddress remoteId) {
        if (!hasConnection(remoteId)) {
            MeshMonitorTimings timings = MeshMonitorTimings.createDefault(consoleLogger);

            Monitor monitor = monitorFactory.newMonitor(consoleLogger, meshMonitor, timings, pingInterval, channel, remoteId);
            monitor.start();
            monitors.add(monitor);
            return true;
        }

        return false;
    }

    public synchronized boolean removeConnection(InetSocketAddress remoteId) {
        return monitors.removeIf(monitor -> monitor.getRemoteId().equals(remoteId));
    }

    public synchronized boolean hasConnection(InetSocketAddress remoteId) {
        return monitors.stream()
                .filter(Monitor::isRunning)
                .map(Monitor::getRemoteId)
                .anyMatch(socketAddress -> socketAddress.equals(remoteId));
    }
}
