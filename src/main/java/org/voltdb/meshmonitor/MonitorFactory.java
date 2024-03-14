package org.voltdb.meshmonitor;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.time.Duration;

@FunctionalInterface
public interface MonitorFactory {

    Monitor newMonitor(ConsoleLogger logger,
                       MeshMonitor meshMonitor,
                       MeshMonitorTimings timings,
                       Duration pingInterval,
                       SocketChannel channel,
                       InetSocketAddress remoteId);
}
