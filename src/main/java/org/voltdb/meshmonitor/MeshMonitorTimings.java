/*
 * Copyright (C) 2024-2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.HdrHistogram.SynchronizedHistogram;

import java.util.Objects;

public final class MeshMonitorTimings {

    public static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;
    public static final long HIGHEST_TRACKABLE_VALUE = 24 * 60 * 60 * 1000 * 1000L;

    private final HistogramWithDelta pingHistogram;
    private final HistogramWithDelta jitterHistogram;
    private final HistogramWithDelta timestampDeltaHistogram;

    public MeshMonitorTimings(
            HistogramWithDelta pingHistogram,
            HistogramWithDelta jitterHistogram,
            HistogramWithDelta timestampDeltaHistogram) {
        this.pingHistogram = pingHistogram;
        this.jitterHistogram = jitterHistogram;
        this.timestampDeltaHistogram = timestampDeltaHistogram;
    }

    public void pingReceived(long now, long lastReceiveTime, long timestampFromRemoteHost, long pingInterval) {
        long valueToRecord = now - lastReceiveTime;
        pingHistogram.recordValueWithExpectedInterval(valueToRecord, pingInterval);

        // Abs because clocks can be slightly out of sync...
        valueToRecord = Math.abs(now - timestampFromRemoteHost);
        timestampDeltaHistogram.recordValueWithExpectedInterval(valueToRecord, pingInterval);
    }

    public void trackWakeupJitter(long observedInterval, long expectedInterval) {
        jitterHistogram.recordValueWithExpectedInterval(observedInterval, expectedInterval);
    }

    private static SynchronizedHistogram defaultHistogram() {
        return new SynchronizedHistogram(HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    }

    public static MeshMonitorTimings createDefault(ConsoleLogger logger) {
        return new MeshMonitorTimings(
                new HistogramWithDelta(logger, "ping", defaultHistogram()),
                new HistogramWithDelta(logger, "jitter", defaultHistogram()),
                new HistogramWithDelta(logger, "timestamp delta", defaultHistogram())
        );
    }

    public HistogramWithDelta pingHistogram() {
        return pingHistogram;
    }

    public HistogramWithDelta jitterHistogram() {
        return jitterHistogram;
    }

    public HistogramWithDelta timestampDeltaHistogram() {
        return timestampDeltaHistogram;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        MeshMonitorTimings that = (MeshMonitorTimings) obj;
        return Objects.equals(this.pingHistogram, that.pingHistogram) &&
               Objects.equals(this.jitterHistogram, that.jitterHistogram) &&
               Objects.equals(this.timestampDeltaHistogram, that.timestampDeltaHistogram);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pingHistogram, jitterHistogram, timestampDeltaHistogram);
    }

    @Override
    public String toString() {
        return "MeshMonitorTimings[" +
               "pingHistogram=" + pingHistogram + ", " +
               "jitterHistogram=" + jitterHistogram + ", " +
               "timestampDeltaHistogram=" + timestampDeltaHistogram + ']';
    }
}
