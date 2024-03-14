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

@FunctionalInterface
public interface MonitorFactory {

    Monitor newMonitor(ConsoleLogger logger,
                       MeshMonitor meshMonitor,
                       MeshMonitorTimings timings,
                       Duration pingInterval,
                       SocketChannel channel,
                       InetSocketAddress remoteId);
}
