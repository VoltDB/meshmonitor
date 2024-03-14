/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.voltdb.meshmonitor.ConsoleLoggerTest.loggerForTest;

class MeshMonitorTimingsTest {

    private MeshMonitorTimings timings;

    @BeforeEach
    void setUp() {
        // Initialize with a simple logger that doesn't output to console to avoid cluttering test output.
        ConsoleLogger logger = loggerForTest();
        timings = MeshMonitorTimings.createDefault(logger);
    }

    @Test
    void shouldRecordPingReceivedProperly() {
        // Given
        long now = 100_000L; // Simulated current time
        long lastReceiveTime = 95_000L; // Simulated last receive time
        long timestampFromRemoteHost = 98_000L; // Simulated timestamp from remote host
        long pingInterval = 5_000L; // Simulated ping interval

        // When
        timings.pingReceived(now, lastReceiveTime, timestampFromRemoteHost, pingInterval);

        // Then
        // Direct assertions on histograms are not possible without exposing histogram data.
        // Assuming we can check histograms indirectly via public API or simulated effects.
        // This would ideally be more specific, such as checking that counts increased or specific values are within expected ranges.
    }

    @Test
    void shouldTrackWakeupJitterProperly() {
        // Given
        long observedInterval = 5_050L; // Observed interval slightly higher than expected
        long expectedInterval = 5_000L; // Expected interval

        // When
        timings.trackWakeupJitter(observedInterval, expectedInterval);

        // Then
        // Assertions similar to `shouldRecordPingReceivedProperly`, focusing on sendHistogram.
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

    // Additional tests can be designed based on other edge cases or behaviors you wish to verify.
}
