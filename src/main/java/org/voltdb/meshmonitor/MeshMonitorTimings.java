package org.voltdb.meshmonitor;

import org.HdrHistogram.SynchronizedHistogram;

public record MeshMonitorTimings(
        HistogramWithDelta receiveHistogram,
        HistogramWithDelta sendHistogram,
        HistogramWithDelta deltaHistogram) {

    public static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;
    public static final long HIGHEST_TRACKABLE_VALUE = 24 * 60 * 60 * 1000 * 1000L;

    public void pingReceived(long now, long lastReceiveTime, long timestampFromRemoteHost, long pingInterval) {
        long valueToRecord = now - lastReceiveTime;
        receiveHistogram.recordValueWithExpectedInterval(valueToRecord, pingInterval);

        // Abs because clocks can be slightly out of sync...
        valueToRecord = Math.abs(now - timestampFromRemoteHost);
        deltaHistogram.recordValueWithExpectedInterval(valueToRecord, pingInterval);
    }

    public void trackWakeupJitter(long observedInterval, long expectedInterval) {
        sendHistogram.recordValueWithExpectedInterval(observedInterval, expectedInterval);
    }

    private static SynchronizedHistogram defaultHistogram() {
        return new SynchronizedHistogram(HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    }

    public static MeshMonitorTimings createDefault(ConsoleLogger logger) {
        return new MeshMonitorTimings(
                new HistogramWithDelta(logger, "receive", defaultHistogram()),
                new HistogramWithDelta(logger, "send", defaultHistogram()),
                new HistogramWithDelta(logger, "delta", defaultHistogram())
        );
    }
}
