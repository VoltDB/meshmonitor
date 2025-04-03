/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor.metrics;

import org.voltdb.meshmonitor.MeshMonitorTimings;
import org.voltdb.meshmonitor.Monitor;

import java.net.InetSocketAddress;

public class MonitorStatsPrinter {

    private final HistogramPrinter histogramPrinter;

    public MonitorStatsPrinter(String hostName) {
        this.histogramPrinter = new HistogramPrinter(hostName);
    }

    public void print(StringBuilder output, Monitor monitor) {
        MeshMonitorTimings timings = monitor.getTimings();
        InetSocketAddress remoteId = monitor.getRemoteId();

        timings.pingHistogram().getCumulativeHistogram(histogram ->
                histogramPrinter.printHistogram(output,
                        histogram,
                        remoteId,
                        "receive_seconds")
        );

        timings.timestampDeltaHistogram().getCumulativeHistogram(histogram ->
                histogramPrinter.printHistogram(output,
                        histogram,
                        remoteId,
                        "delta_seconds")
        );

        timings.jitterHistogram().getCumulativeHistogram(histogram -> {
            histogramPrinter.printHistogram(output,
                    histogram,
                    remoteId,
                    "send_seconds");
        });
    }
}
