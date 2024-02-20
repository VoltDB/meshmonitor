package org.voltdb.meshmonitor;

import java.net.InetSocketAddress;

public class HistogramLogger {

    private final ConsoleLogger consoleLogger;

    public HistogramLogger(ConsoleLogger consoleLogger) {
        this.consoleLogger = consoleLogger;
    }

    public void printResults(Monitor monitor, long minHiccupSize) {
        MeshMonitorTimings currentTimings = monitor.getTimings();
        InetSocketAddress remoteId = monitor.getRemoteId();

        if (currentTimings.hasOutliers(minHiccupSize)) {
            currentTimings.receiveHistogram().printResultsAndReset(remoteId);
            currentTimings.deltaHistogram().printResultsAndReset(remoteId);
            currentTimings.sendHistogram().printResultsAndReset(remoteId);
        } else {
            consoleLogger.log(
                    remoteId,
                    "Threshold (%2.1fms) not reached, nothing to report",
                    minHiccupSize / 1000.0);
        }
    }
}
