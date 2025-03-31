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
    private final HistogramWithDelta receiveHistogram;
    private final HistogramWithDelta sendHistogram;
    private final HistogramWithDelta deltaHistogram;

    public MeshMonitorTimings(
            HistogramWithDelta receiveHistogram,
            HistogramWithDelta sendHistogram,
            HistogramWithDelta deltaHistogram) {
        this.receiveHistogram = receiveHistogram;
        this.sendHistogram = sendHistogram;
        this.deltaHistogram = deltaHistogram;
    }

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

    public HistogramWithDelta receiveHistogram() {
        return receiveHistogram;
    }

    public HistogramWithDelta sendHistogram() {
        return sendHistogram;
    }

    public HistogramWithDelta deltaHistogram() {
        return deltaHistogram;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        MeshMonitorTimings that = (MeshMonitorTimings) obj;
        return Objects.equals(this.receiveHistogram, that.receiveHistogram) &&
               Objects.equals(this.sendHistogram, that.sendHistogram) &&
               Objects.equals(this.deltaHistogram, that.deltaHistogram);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiveHistogram, sendHistogram, deltaHistogram);
    }

    @Override
    public String toString() {
        return "MeshMonitorTimings[" +
               "receiveHistogram=" + receiveHistogram + ", " +
               "sendHistogram=" + sendHistogram + ", " +
               "deltaHistogram=" + deltaHistogram + ']';
    }

}
