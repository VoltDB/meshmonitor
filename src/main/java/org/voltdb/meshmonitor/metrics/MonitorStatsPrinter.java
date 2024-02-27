/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.metrics;

import java.net.InetSocketAddress;

import org.voltdb.meshmonitor.MeshMonitorTimings;
import org.voltdb.meshmonitor.Monitor;

public class MonitorStatsPrinter {

    private final HistogramPrinter histogramPrinter;

    public MonitorStatsPrinter(String hostName) {
        this.histogramPrinter = new HistogramPrinter(hostName);
    }

    public void print(StringBuilder output, Monitor monitor) {
        MeshMonitorTimings timings = monitor.getTimings();
        InetSocketAddress remoteId = monitor.getRemoteId();

        histogramPrinter.printHistogram(output,
                timings.receiveHistogram().getCumulativeHistogram(),
                remoteId,
                "receive_seconds");

        histogramPrinter.printHistogram(output,
                timings.deltaHistogram().getCumulativeHistogram(),
                remoteId,
                "delta_seconds");

        histogramPrinter.printHistogram(output,
                timings.sendHistogram().getCumulativeHistogram(),
                remoteId,
                "send_seconds");
    }
}
