/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.voltdb.meshmonitor;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistogramLoggerTest {

    private static final InetSocketAddress REMOTE_ID_1 = new InetSocketAddress("10.1.0.2", 8080);
    private static final InetSocketAddress REMOTE_ID_2 = new InetSocketAddress("10.1.0.3", 8080);

    public static final long EXPECTED_INTERVAL = TimeUnit.MILLISECONDS.toMicros(5);

    @Test
    void shouldPrintHeader() {
        // Given
        StringWriter logContent = new StringWriter();
        ConsoleLogger consoleLogger = ConsoleLoggerTest.loggerForTest(logContent);

        HistogramLogger logger = new HistogramLogger(consoleLogger);

        // When
        logger.printHeader();

        // Then
        assertThat(logContent.toString()).contains(
                "----------ping-(ms)---------- ---------jitter-(ms)--------- ----timestamp-diff-(ms)------",
                "Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99"
        );
    }

    @Test
    void shouldPrintHistograms() {
        // Given
        StringWriter logContent = new StringWriter();
        ConsoleLogger consoleLogger = ConsoleLoggerTest.loggerForTest(logContent);

        MeshMonitorTimings timings1 = MeshMonitorTimings.createDefault(consoleLogger);
        timings1.receiveHistogram().recordValueWithExpectedInterval(TimeUnit.MILLISECONDS.toMicros(10), EXPECTED_INTERVAL);
        timings1.deltaHistogram().recordValueWithExpectedInterval(TimeUnit.MILLISECONDS.toMicros(12), EXPECTED_INTERVAL);
        timings1.sendHistogram().recordValueWithExpectedInterval(TimeUnit.MILLISECONDS.toMicros(14), EXPECTED_INTERVAL);

        MeshMonitorTimings timings2 = MeshMonitorTimings.createDefault(consoleLogger);
        timings2.receiveHistogram().recordValueWithExpectedInterval(TimeUnit.MILLISECONDS.toMicros(1), EXPECTED_INTERVAL);
        timings2.deltaHistogram().recordValueWithExpectedInterval(TimeUnit.MILLISECONDS.toMicros(3), EXPECTED_INTERVAL);
        timings2.sendHistogram().recordValueWithExpectedInterval(TimeUnit.MILLISECONDS.toMicros(7), EXPECTED_INTERVAL);

        Monitor monitor1 = mock(Monitor.class);
        when(monitor1.getRemoteId()).thenReturn(REMOTE_ID_1);
        when(monitor1.getTimings()).thenReturn(timings1);

        Monitor monitor2 = mock(Monitor.class);
        when(monitor2.getRemoteId()).thenReturn(REMOTE_ID_2);
        when(monitor2.getTimings()).thenReturn(timings2);

        HistogramLogger logger = new HistogramLogger(consoleLogger);

        // When
        logger.printResults(monitor1, EXPECTED_INTERVAL * 2);
        logger.printResults(monitor2, EXPECTED_INTERVAL * 2);

        // Then
        assertThat(logContent.toString()).containsIgnoringNewLines(
//                                 ----------ping-(ms)---------- ---------jitter-(ms)--------- ----timestamp-diff-(ms)------
//                                   Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99
                "[       10.1.0.2]  10.0   7.5  10.0  10.0  10.0| 12.0   9.5  12.0  12.0  12.0| 14.0  11.5  14.0  14.0  14.0",
                "[       10.1.0.3]   1.0   1.0   1.0   1.0   1.0|  3.0   3.0   3.0   3.0   3.0|  7.0   7.0   7.0   7.0   7.0"
        );
    }

    @Test
    void shouldConvertLargeValuesToSecondss() {
        // Given
        StringWriter logContent = new StringWriter();
        ConsoleLogger consoleLogger = ConsoleLoggerTest.loggerForTest(logContent);

        MeshMonitorTimings timings1 = MeshMonitorTimings.createDefault(consoleLogger);
        timings1.receiveHistogram().recordValueWithExpectedInterval(TimeUnit.SECONDS.toMicros(10), EXPECTED_INTERVAL);
        timings1.deltaHistogram().recordValueWithExpectedInterval(TimeUnit.SECONDS.toMicros(12), EXPECTED_INTERVAL);
        timings1.sendHistogram().recordValueWithExpectedInterval(TimeUnit.SECONDS.toMicros(14), EXPECTED_INTERVAL);

        Monitor monitor1 = mock(Monitor.class);
        when(monitor1.getRemoteId()).thenReturn(REMOTE_ID_1);
        when(monitor1.getTimings()).thenReturn(timings1);

        HistogramLogger logger = new HistogramLogger(consoleLogger);

        // When
        logger.printResults(monitor1, EXPECTED_INTERVAL * 2);

        // Then
        assertThat(logContent.toString()).containsIgnoringNewLines(
//                                 ----------ping-(ms)---------- ---------jitter-(ms)--------- ----timestamp-diff-(ms)------
//                                   Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99|  Max  Mean    99  99.9 99.99
                "[       10.1.0.2] 10.0s  5.0s  9.9s 10.0s 10.0s|12.0s  6.0s 11.9s 12.0s 12.0s|14.0s  7.0s 13.9s 14.0s 14.0s"
        );
    }
}
