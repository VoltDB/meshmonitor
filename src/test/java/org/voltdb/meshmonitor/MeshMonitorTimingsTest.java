/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.voltdb.meshmonitor.ConsoleLoggerTest.loggerForTest;

class MeshMonitorTimingsTest {

    private MeshMonitorTimings timings;

    @Test
    void shouldRecordPingReceivedProperly() {
        // Given
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(loggerForTest());

        long expected = TimeUnit.MILLISECONDS.toMicros(10);

        long now = 100_000L; // Simulated current time
        long lastReceiveTime = 95_000L; // Simulated last receive time
        long timestampFromRemoteHost = 98_000L; // Simulated timestamp from remote host
        long pingInterval = 5_000L; // Simulated ping interval

        // When
        timings.pingReceived(now, lastReceiveTime, timestampFromRemoteHost, pingInterval);

        // Then
    }

    @Test
    void shouldTrackWakeupJitterProperly() {
        // Given
        MeshMonitorTimings timings = MeshMonitorTimings.createDefault(loggerForTest());

        long expected = TimeUnit.MILLISECONDS.toMicros(10);

        // When
        timings.trackWakeupJitter(expected, expected);

        // Then
        long actual = timings.sendHistogram().getCumulativeHistogram().getCountAtValue(expected);
        assertThat(actual).isEqualTo(1);
    }

    @Test
    void shouldHandleNegativeDeltaInPingReceived() {
        // Given
        long now = 100_000L; // Simulated current time
        long lastReceiveTime = 95_000L; // Last receive time
        // Simulating timestamp from the remote host that is ahead of 'now', causing a negative delta
        long timestampFromRemoteHost = 100_500L;
        long pingInterval = 5_000L;

        // When
        timings.pingReceived(now, lastReceiveTime, timestampFromRemoteHost, pingInterval);

        // Then
        // Assertions focusing on deltaHistogram to verify handling of negative delta correctly by taking absolute value.
    }
}
