/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.voltdb.meshmonitor.ConsoleLoggerTest.loggerForTest;

class MeshMonitorTimingsTest {

    @Test
    void shouldRecordPingReceivedProperly() {
        // Given
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(loggerForTest());

        long now = TimeUnit.MILLISECONDS.toMicros(42);
        long lastReceiveTime = TimeUnit.MILLISECONDS.toMicros(35);
        long timestampFromRemoteHost = TimeUnit.MILLISECONDS.toMicros(41);
        long pingInterval = TimeUnit.MILLISECONDS.toMicros(5);

        // When
        timings.pingReceived(now, lastReceiveTime, timestampFromRemoteHost, pingInterval);

        // Then
        long receiveSamples = timings.pingHistogram().getCumulativeHistogram().getCountAtValue(now - lastReceiveTime);
        long deltaSamples = timings.timestampDeltaHistogram().getCumulativeHistogram().getCountAtValue(now - timestampFromRemoteHost);

        assertThat(receiveSamples).isEqualTo(1);
        assertThat(deltaSamples).isEqualTo(1);
    }

    @Test
    void shouldHandleNegativeDeltaInPingReceived() {
        // Given
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(loggerForTest());

        long now = TimeUnit.MILLISECONDS.toMicros(42);
        long lastReceiveTime = TimeUnit.MILLISECONDS.toMicros(35);
        long timestampFromRemoteHost = TimeUnit.MILLISECONDS.toMicros(43);
        long pingInterval = TimeUnit.MILLISECONDS.toMicros(5);

        // When
        timings.pingReceived(now, lastReceiveTime, timestampFromRemoteHost, pingInterval);

        // Then
        long receiveSamples = timings.pingHistogram().getCumulativeHistogram().getCountAtValue(now - lastReceiveTime);
        long deltaSamples = timings.timestampDeltaHistogram().getCumulativeHistogram().getCountAtValue(Math.abs(now - timestampFromRemoteHost));

        assertThat(receiveSamples).isEqualTo(1);
        assertThat(deltaSamples).isEqualTo(1);
    }

    @Test
    void shouldTrackWakeupJitterProperly() {
        // Given
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(loggerForTest());

        long expected = TimeUnit.MILLISECONDS.toMicros(10);

        // When
        timings.trackWakeupJitter(expected, expected);

        // Then
        long actual = timings.jitterHistogram().getCumulativeHistogram().getCountAtValue(expected);
        assertThat(actual).isEqualTo(1);
    }
}
